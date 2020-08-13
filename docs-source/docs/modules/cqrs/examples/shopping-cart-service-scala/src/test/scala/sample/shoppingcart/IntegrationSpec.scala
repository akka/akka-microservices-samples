package sample.shoppingcart

import java.util.UUID

import scala.concurrent.duration._

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.eventstream.EventStream
import akka.cluster.MemberStatus
import akka.cluster.typed.Cluster
import akka.cluster.typed.Join
import akka.grpc.GrpcClientSettings
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.Effect
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import akka.testkit.SocketUtil
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.TestSuite
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.Span
import org.scalatest.wordspec.AnyWordSpecLike

object IntegrationSpec {
  private val keyspace = s"IntegrationSpec_${System.currentTimeMillis()}"

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
    PatienceConfig(3.seconds, Span(100, org.scalatest.time.Millis))

  private def nodeConfig(role: String, grpcPort: Int): Config = {
    ConfigFactory.parseString(s"""
      akka.cluster.roles = [$role]
      shopping.grpc.port = $grpcPort
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

  override protected def beforeAll(): Unit = {
    // avoid concurrent creation of keyspace and tables
    initializePersistence()
    Main.createTables(testKit1.system)

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

    "update and consume from different nodes via gRPC" in {
      val eventProbe3 = testKit3.createTestProbe[ShoppingCart.Event]()
      testKit3.system.eventStream ! EventStream.Subscribe(eventProbe3.ref)

      // add from client1, consume event on node3
      val response1 = client1.addItem(proto.AddItemRequest(cartId = "cart-1", itemId = "foo", quantity = 42))
      val updatedCart1 = response1.futureValue
      updatedCart1.items.head.itemId should ===("foo")
      updatedCart1.items.head.quantity should ===(42)
      eventProbe3.expectMessage(ShoppingCart.ItemAdded("cart-1", "foo", 42))

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

      eventProbe3.expectMessage(ShoppingCart.ItemAdded("cart-2", "bar", 17))
      eventProbe3.expectMessage(ShoppingCart.ItemQuantityAdjusted("cart-2", "bar", 18))
    }

    "continue event processing from offset" in {
      // give it time to write the offset before shutting down
      Thread.sleep(1000)
      testKit3.shutdownTestKit()

      val eventProbe4 = testKit4.createTestProbe[ShoppingCart.Event]()
      testKit4.system.eventStream ! EventStream.Subscribe(eventProbe4.ref)

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
      eventProbe4.expectMessage(ShoppingCart.ItemAdded("cart-3", "abc", 43))
    }

  }
}
