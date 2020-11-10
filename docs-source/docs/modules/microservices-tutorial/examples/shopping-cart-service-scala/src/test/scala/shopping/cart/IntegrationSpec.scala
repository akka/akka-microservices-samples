package shopping.cart

import java.util.UUID

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.MemberStatus
import akka.cluster.typed.Cluster
import akka.grpc.GrpcClientSettings
import akka.kafka.ConsumerSettings
import akka.kafka.Subscriptions
import akka.kafka.scaladsl.Consumer
import akka.persistence.testkit.scaladsl.PersistenceInit
import akka.testkit.SocketUtil
import com.google.protobuf.any.{ Any => ScalaPBAny }
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.Span
import org.scalatest.wordspec.AnyWordSpec
import org.slf4j.LoggerFactory
import shopping.order.proto.OrderRequest
import shopping.order.proto.OrderResponse
import shopping.order.proto.ShoppingOrderService

object IntegrationSpec {
  private val uniqueQualifier = System.currentTimeMillis()
  private val keyspace = s"IntegrationSpec_$uniqueQualifier"

  val config: Config = ConfigFactory
    .parseString(s"""
      akka.cluster.jmx.multi-mbeans-in-same-jvm = on
      
      akka.remote.artery.canonical {
        hostname = "127.0.0.1"
        port = 0
      }

      akka.persistence.cassandra {
        events-by-tag {
          eventual-consistency-delay = 200ms
        }
        query {
          refresh-interval = 500 ms
        }
        journal.keyspace = $keyspace
        journal.keyspace-autocreate = on
        journal.tables-autocreate = on
        snapshot.keyspace = $keyspace
        snapshot.keyspace-autocreate = on
        snapshot.tables-autocreate = on
      }
      datastax-java-driver {
        basic.contact-points = ["127.0.0.1:9042"]
        basic.load-balancing-policy.local-datacenter = "datacenter1"
        # lots of ActorSystems in the same JVM
        advanced.session-leak.threshold = 10
      }
      
      akka.projection.cassandra.offset-store.keyspace = $keyspace
      
      shopping-cart-service.kafka.topic = "shopping_cart_events_$uniqueQualifier"

      akka.kafka.consumer {
        kafka-clients {
          auto.offset.reset = "earliest"
        }
      }
      
      akka.actor.testkit.typed {
        single-expect-default = 5s
        filter-leeway = 5s
        system-shutdown-default = 30s
      }
    """)
    .withFallback(
      ConfigFactory.load("local1")
    ) // pick up local configuration to test it, dynamic ports have been overridden

  private def nodeConfig(
      grpcPort: Int,
      managementPorts: Seq[Int],
      managementPortIndex: Int): Config =
    ConfigFactory.parseString(s"""
      shopping-cart-service.grpc {
        interface = "localhost"
        port = $grpcPort
      }
      akka.management.http.port = ${managementPorts(managementPortIndex)}
      akka.discovery.config.services {
        "shopping-cart-service" {
          endpoints = [
            {host = "127.0.0.1", port = ${managementPorts(0)}},
            {host = "127.0.0.1", port = ${managementPorts(1)}}
          ]
        }
      }
      """)

  class TestNodeFixture(
      grpcPort: Int,
      managementPorts: Seq[Int],
      managementPortIndex: Int) {
    val testKit =
      ActorTestKit(
        "IntegrationSpec",
        nodeConfig(grpcPort, managementPorts, managementPortIndex)
          .withFallback(IntegrationSpec.config)
          .resolve())

    def system: ActorSystem[_] = testKit.system

    private val clientSettings =
      GrpcClientSettings
        .connectToServiceAt("127.0.0.1", grpcPort)(testKit.system)
        .withTls(false)
    lazy val client: proto.ShoppingCartService =
      proto.ShoppingCartServiceClient(clientSettings)(testKit.system)

  }
}

class IntegrationSpec
    extends AnyWordSpec
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures
    with Eventually {
  import IntegrationSpec.TestNodeFixture

  private val logger =
    LoggerFactory.getLogger(classOf[IntegrationSpec])

  implicit private val patience: PatienceConfig =
    PatienceConfig(10.seconds, Span(100, org.scalatest.time.Millis))

  private val (grpcPorts, managementPorts) =
    SocketUtil
      .temporaryServerAddresses(6, "127.0.0.1")
      .map(_.getPort)
      .splitAt(3)

  // one TestKit (ActorSystem) per cluster node
  private val testNode1 =
    new TestNodeFixture(grpcPorts(0), managementPorts, 0)
  private val testNode2 =
    new TestNodeFixture(grpcPorts(1), managementPorts, 1)
  private val testNode3 =
    new TestNodeFixture(grpcPorts(2), managementPorts, 2)

  private val systems3 =
    List(testNode1, testNode2, testNode3).map(_.testKit.system)

  private val kafkaTopicProbe =
    testNode1.testKit.createTestProbe[Any]()

  // stub of the ShoppingOrderService
  private val orderServiceProbe =
    testNode1.testKit.createTestProbe[OrderRequest]()
  private val testOrderService: ShoppingOrderService =
    new ShoppingOrderService {
      override def order(in: OrderRequest): Future[OrderResponse] = {
        orderServiceProbe.ref ! in
        Future.successful(OrderResponse(ok = true))
      }
    }

  def mainBehavior(): Behavior[Nothing] = {
    Behaviors.setup[Nothing] { context =>
      new Main(context) {
        override protected def orderServiceClient(
            system: ActorSystem[_]): ShoppingOrderService = {
          testOrderService
        }
      }
    }
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    // avoid concurrent creation of keyspace and tables
    val timeout = 10.seconds
    Await.result(
      PersistenceInit.initializeDefaultPlugins(testNode1.system, timeout),
      timeout)
    CreateTableTestUtils.createTables(testNode1.system)
  }

  private def initializeKafkaTopicProbe(): Unit = {
    implicit val sys: ActorSystem[_] = testNode1.system
    implicit val ec: ExecutionContext = sys.executionContext
    val topic =
      sys.settings.config.getString("shopping-cart-service.kafka.topic")
    val groupId = UUID.randomUUID().toString
    val consumerSettings =
      ConsumerSettings(sys, new StringDeserializer, new ByteArrayDeserializer)
        .withBootstrapServers("localhost:9092") // provided by Docker compose
        .withGroupId(groupId)
    Consumer
      .plainSource(consumerSettings, Subscriptions.topics(topic))
      .map { record =>
        val bytes = record.value()
        val x = ScalaPBAny.parseFrom(bytes)
        val typeUrl = x.typeUrl
        val inputBytes = x.value.newCodedInput()
        val event: AnyRef =
          typeUrl match {
            case "shopping-cart-service/shoppingcart.ItemAdded" =>
              proto.ItemAdded.parseFrom(inputBytes)
            case "shopping-cart-service/shoppingcart.ItemQuantityAdjusted" =>
              proto.ItemQuantityAdjusted.parseFrom(inputBytes)
            case "shopping-cart-service/shoppingcart.ItemRemoved" =>
              proto.ItemRemoved.parseFrom(inputBytes)
            case "shopping-cart-service/shoppingcart.CheckedOut" =>
              proto.CheckedOut.parseFrom(inputBytes)
            case _ =>
              throw new IllegalArgumentException(
                s"unknown record type [$typeUrl]")
          }
        event
      }
      .runForeach(kafkaTopicProbe.ref.tell)
      .failed
      .foreach { case e: Exception =>
        logger.error(s"Test consumer of $topic failed", e)
      }
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    testNode3.testKit.shutdownTestKit()
    testNode2.testKit.shutdownTestKit()
    testNode1.testKit.shutdownTestKit()
  }

  "Shopping Cart service" should {
    "init and join Cluster" in {
      testNode1.testKit.spawn[Nothing](mainBehavior(), "guardian")
      testNode2.testKit.spawn[Nothing](mainBehavior(), "guardian")
      testNode3.testKit.spawn[Nothing](mainBehavior(), "guardian")

      // let the nodes join and become Up
      eventually(PatienceConfiguration.Timeout(15.seconds)) {
        systems3.foreach { sys =>
          Cluster(sys).selfMember.status should ===(MemberStatus.Up)
          Cluster(sys).state.members.unsorted.map(_.status) should ===(
            Set(MemberStatus.Up))
        }
      }

      initializeKafkaTopicProbe()
    }

    "update and project from different nodes via gRPC" in {
      // add from client1
      val response1 = testNode1.client.addItem(
        proto.AddItemRequest(cartId = "cart-1", itemId = "foo", quantity = 42))
      val updatedCart1 = response1.futureValue
      updatedCart1.items.head.itemId should ===("foo")
      updatedCart1.items.head.quantity should ===(42)

      // first may take longer time
      val published1 =
        kafkaTopicProbe.expectMessageType[proto.ItemAdded](20.seconds)
      published1.cartId should ===("cart-1")
      published1.itemId should ===("foo")
      published1.quantity should ===(42)

      // add from client2
      val response2 = testNode2.client.addItem(
        proto.AddItemRequest(cartId = "cart-2", itemId = "bar", quantity = 17))
      val updatedCart2 = response2.futureValue
      updatedCart2.items.head.itemId should ===("bar")
      updatedCart2.items.head.quantity should ===(17)

      // update from client2
      val response3 =
        testNode2.client.updateItem(proto
          .UpdateItemRequest(cartId = "cart-2", itemId = "bar", quantity = 18))
      val updatedCart3 = response3.futureValue
      updatedCart3.items.head.itemId should ===("bar")
      updatedCart3.items.head.quantity should ===(18)

      // ItemPopularityProjection has consumed the events and updated db
      eventually {
        testNode1.client
          .getItemPopularity(proto.GetItemPopularityRequest(itemId = "foo"))
          .futureValue
          .popularityCount should ===(42)

        testNode1.client
          .getItemPopularity(proto.GetItemPopularityRequest(itemId = "bar"))
          .futureValue
          .popularityCount should ===(18)
      }

      val published2 =
        kafkaTopicProbe.expectMessageType[proto.ItemAdded]
      published2.cartId should ===("cart-2")
      published2.itemId should ===("bar")
      published2.quantity should ===(17)

      val published3 =
        kafkaTopicProbe.expectMessageType[proto.ItemQuantityAdjusted]
      published3.cartId should ===("cart-2")
      published3.itemId should ===("bar")
      published3.quantity should ===(18)

      val response4 =
        testNode2.client.checkout(proto.CheckoutRequest(cartId = "cart-2"))
      response4.futureValue.checkedOut should ===(true)

      val orderRequest =
        orderServiceProbe.expectMessageType[OrderRequest]
      orderRequest.cartId should ===("cart-2")
      orderRequest.items.head.itemId should ===("bar")
      orderRequest.items.head.quantity should ===(18)

      val published4 =
        kafkaTopicProbe.expectMessageType[proto.CheckedOut]
      published4.cartId should ===("cart-2")
    }

  }
}
