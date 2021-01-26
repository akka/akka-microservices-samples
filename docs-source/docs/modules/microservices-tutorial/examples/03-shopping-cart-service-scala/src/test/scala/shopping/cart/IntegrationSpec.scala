package shopping.cart

import scala.concurrent.Await
import scala.concurrent.duration._

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.MemberStatus
import akka.cluster.typed.Cluster
import akka.grpc.GrpcClientSettings
import akka.persistence.testkit.scaladsl.PersistenceInit
import akka.testkit.SocketUtil
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.Span
import org.scalatest.wordspec.AnyWordSpec

object IntegrationSpec {
  val sharedConfig: Config = ConfigFactory.load("integration-test.conf")

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
          .withFallback(sharedConfig)
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

  def mainBehavior(): Behavior[Nothing] = {
    Behaviors.setup[Nothing] { context =>
      new Main(context)
    }
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    CreateTableTestUtils.dropAndRecreateTables(testNode1.system)
    // avoid concurrent creation of tables
    val timeout = 10.seconds
    Await.result(
      PersistenceInit.initializeDefaultPlugins(testNode1.system, timeout),
      timeout)
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
    }

    "update from different nodes via gRPC" in {
      // add from client1
      val response1 = testNode1.client.addItem(
        proto.AddItemRequest(cartId = "cart-1", itemId = "foo", quantity = 42))
      val updatedCart1 = response1.futureValue
      updatedCart1.items.head.itemId should ===("foo")
      updatedCart1.items.head.quantity should ===(42)

      // add from client2
      val response2 = testNode2.client.addItem(
        proto.AddItemRequest(cartId = "cart-2", itemId = "bar", quantity = 17))
      val updatedCart2 = response2.futureValue
      updatedCart2.items.head.itemId should ===("bar")
      updatedCart2.items.head.quantity should ===(17)
    }

  }
}
