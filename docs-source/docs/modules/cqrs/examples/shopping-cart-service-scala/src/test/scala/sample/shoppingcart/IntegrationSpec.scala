package sample.shoppingcart

import java.util.UUID

import scala.concurrent.duration._

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.cluster.MemberStatus
import akka.cluster.typed.Cluster
import akka.cluster.typed.Join
import akka.grpc.GrpcClientSettings
import akka.kafka.ConsumerSettings
import akka.kafka.Subscriptions
import akka.kafka.scaladsl.Consumer
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.Effect
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import akka.testkit.SocketUtil
import com.google.protobuf.any.{ Any => ScalaPBAny }
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.scalatest.BeforeAndAfterAll
import org.scalatest.TestSuite
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.Span
import org.scalatest.wordspec.AnyWordSpecLike

object IntegrationSpec {
  private val uniqueQualifier = System.currentTimeMillis()
  private val keyspace = s"IntegrationSpec_$uniqueQualifier"

  val config: Config = ConfigFactory
    .parseString(s"""
      akka.cluster {
         seed-nodes = []
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
      }
      
      akka.projection.cassandra.offset-store.keyspace = $keyspace
      
      shopping-cart.kafka-topic = "shopping_cart_events_$uniqueQualifier"
      
      akka.http.server.preview.enable-http2 = on
      
      akka.loglevel = DEBUG
      akka.actor.testkit.typed.single-expect-default = 5s
      # For LoggingTestKit
      akka.actor.testkit.typed.filter-leeway = 5s
      akka.actor.testkit.typed.throw-on-shutdown-timeout = off
    """)
    .withFallback(ConfigFactory.load())
}

class IntegrationSpec
    extends TestSuite
    with Matchers
    with BeforeAndAfterAll
    with AnyWordSpecLike
    with ScalaFutures
    with Eventually {

  implicit private val patience: PatienceConfig =
    PatienceConfig(5.seconds, Span(100, org.scalatest.time.Millis))

  private def nodeConfig(role: String, grpcPort: Int): Config = {
    ConfigFactory.parseString(s"""
      akka.cluster.roles = [$role]
      shopping-cart.grpc.port = $grpcPort
      """)
  }

  private val grpcPorts = SocketUtil.temporaryServerAddresses(4, "127.0.0.1").map(_.getPort)

  // one TestKit (ActorSystem) per cluster node
  private val testKit1 =
    ActorTestKit("IntegrationSpec", nodeConfig("write-model", grpcPorts(0)).withFallback(IntegrationSpec.config))
  private val testKit2 =
    ActorTestKit("IntegrationSpec", nodeConfig("write-model", grpcPorts(1)).withFallback(IntegrationSpec.config))
  private val testKit3 =
    ActorTestKit("IntegrationSpec", nodeConfig("read-model", grpcPorts(2)).withFallback(IntegrationSpec.config))
  private val testKit4 =
    ActorTestKit("IntegrationSpec", nodeConfig("read-model", grpcPorts(3)).withFallback(IntegrationSpec.config))

  private val systems3 = List(testKit1.system, testKit2.system, testKit3.system)

  private val clientSettings1 =
    GrpcClientSettings.connectToServiceAt("127.0.0.1", grpcPorts(0))(testKit1.system).withTls(false)
  private lazy val client1: proto.ShoppingCartService =
    proto.ShoppingCartServiceClient(clientSettings1)(testKit1.system)

  private val clientSettings2 =
    GrpcClientSettings.connectToServiceAt("127.0.0.1", grpcPorts(1))(testKit2.system).withTls(false)
  private lazy val client2: proto.ShoppingCartService =
    proto.ShoppingCartServiceClient(clientSettings2)(testKit2.system)

  private val kafkaTopicProbe = testKit1.createTestProbe[Any]()

  override protected def beforeAll(): Unit = {
    // avoid concurrent creation of keyspace and tables
    initializePersistence()
    Main.createTables(testKit1.system)

    initializeKafkaTopicProbe()

    super.beforeAll()
  }

  // FIXME use Akka's initializePlugins instead when released https://github.com/akka/akka/issues/28808
  private def initializePersistence(): Unit = {
    val persistenceId = PersistenceId.ofUniqueId(s"persistenceInit-${UUID.randomUUID()}")
    val ref = testKit1.spawn(
      EventSourcedBehavior[String, String, String](
        persistenceId,
        "",
        commandHandler = (_, _) => Effect.stop(),
        eventHandler = (_, _) => ""))
    ref ! "start"
    testKit1.createTestProbe().expectTerminated(ref, 10.seconds)
  }

  private def initializeKafkaTopicProbe(): Unit = {
    implicit val sys: ActorSystem[_] = testKit1.system
    val bootstrapServers = testKit1.system.settings.config.getString("shopping-cart.kafka-bootstrap-servers")
    val config = testKit1.system.settings.config.getConfig("akka.kafka.consumer")
    val topic = testKit1.system.settings.config.getString("shopping-cart.kafka-topic")
    val groupId = UUID.randomUUID().toString
    val consumerSettings =
      ConsumerSettings(config, new StringDeserializer, new ByteArrayDeserializer)
        .withBootstrapServers(bootstrapServers)
        .withGroupId(groupId)
    Consumer
      .plainSource(consumerSettings, Subscriptions.topics(topic))
      .map { record =>
        val bytes = record.value()
        val x = ScalaPBAny.parseFrom(bytes)
        val typeUrl = x.typeUrl
        val inputBytes = x.value.newCodedInput()
        val event: Any =
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
              throw new IllegalArgumentException(s"unknown record type [$typeUrl]")
          }
        event
      }
      .runForeach(kafkaTopicProbe.ref.tell)
  }

  override protected def afterAll(): Unit = {
    super.afterAll()

    testKit4.shutdownTestKit()
    testKit3.shutdownTestKit()
    testKit2.shutdownTestKit()
    testKit1.shutdownTestKit()
  }

  "Shopping Cart application" should {
    "init and join Cluster" in {
      testKit1.spawn[Nothing](Guardian(), "guardian")
      testKit2.spawn[Nothing](Guardian(), "guardian")
      testKit3.spawn[Nothing](Guardian(), "guardian")
      // node4 is initialized and joining later

      systems3.foreach { sys =>
        Cluster(sys).manager ! Join(Cluster(testKit1.system).selfMember.address)
      }

      // let the nodes join and become Up
      eventually(PatienceConfiguration.Timeout(10.seconds)) {
        systems3.foreach { sys =>
          Cluster(sys).selfMember.status should ===(MemberStatus.Up)
        }
      }
    }

    "update and project from different nodes via gRPC" in {
      // add from client1, consume event on node3
      val response1 = client1.addItem(proto.AddItemRequest(cartId = "cart-1", itemId = "foo", quantity = 42))
      val updatedCart1 = response1.futureValue
      updatedCart1.items.head.itemId should ===("foo")
      updatedCart1.items.head.quantity should ===(42)

      val published1 = kafkaTopicProbe.expectMessageType[proto.ItemAdded]
      published1.cartId should ===("cart-1")
      published1.itemId should ===("foo")
      published1.quantity should ===(42)

      // add from client2, consume event on node3
      val response2 = client2.addItem(proto.AddItemRequest(cartId = "cart-2", itemId = "bar", quantity = 17))
      val updatedCart2 = response2.futureValue
      updatedCart2.items.head.itemId should ===("bar")
      updatedCart2.items.head.quantity should ===(17)

      // update from client2, consume event on node3
      val response3 = client2.updateItem(proto.UpdateItemRequest(cartId = "cart-2", itemId = "bar", quantity = 18))
      val updatedCart3 = response3.futureValue
      updatedCart3.items.head.itemId should ===("bar")
      updatedCart3.items.head.quantity should ===(18)

      // ItemPopularityProjection has consumed the events and updated db
      eventually {
        client1
          .getItemPopularity(proto.GetItemPopularityRequest(itemId = "foo"))
          .futureValue
          .popularityCount should ===(42)

        client1
          .getItemPopularity(proto.GetItemPopularityRequest(itemId = "bar"))
          .futureValue
          .popularityCount should ===(18)
      }

      val published2 = kafkaTopicProbe.expectMessageType[proto.ItemAdded]
      published2.cartId should ===("cart-2")
      published2.itemId should ===("bar")
      published2.quantity should ===(17)

      val published3 = kafkaTopicProbe.expectMessageType[proto.ItemQuantityAdjusted]
      published3.cartId should ===("cart-2")
      published3.itemId should ===("bar")
      published3.quantity should ===(18)
    }

    "continue event processing from offset" in {
      // give it time to write the offset before shutting down
      Thread.sleep(1000)
      testKit3.shutdownTestKit()

      testKit4.spawn[Nothing](Guardian(), "guardian")

      Cluster(testKit4.system).manager ! Join(Cluster(testKit1.system).selfMember.address)

      // let the node join and become Up
      eventually(PatienceConfiguration.Timeout(10.seconds)) {
        Cluster(testKit4.system).selfMember.status should ===(MemberStatus.Up)
      }

      // update from client1, consume event on node4
      val response1 = client1.addItem(proto.AddItemRequest(cartId = "cart-3", itemId = "abc", quantity = 43))
      response1.futureValue.items.head.itemId should ===("abc")

      // note that node4 is new, but continues reading from previous offset, i.e. not receiving events
      // that have already been consumed

      // ItemPopularityProjection has consumed the events and updated db
      eventually {
        client1
          .getItemPopularity(proto.GetItemPopularityRequest(itemId = "abc"))
          .futureValue
          .popularityCount should ===(43)
      }

      val published1 = kafkaTopicProbe.expectMessageType[proto.ItemAdded]
      published1.cartId should ===("cart-3")
      published1.itemId should ===("abc")
      published1.quantity should ===(43)
    }

  }
}
