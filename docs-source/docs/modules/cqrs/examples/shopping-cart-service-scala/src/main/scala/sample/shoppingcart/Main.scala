package sample.shoppingcart

import scala.concurrent.Await
import scala.concurrent.duration._

import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.Cluster
import akka.grpc.GrpcClientSettings
import akka.projection.cassandra.scaladsl.CassandraProjection
import akka.stream.alpakka.cassandra.scaladsl.CassandraSessionRegistry
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import sample.shoppingorder.proto.ShoppingOrderService
import sample.shoppingorder.proto.ShoppingOrderServiceClient

object Main {

  def main(args: Array[String]): Unit = {
    args.headOption match {

      case Some(portString) if portString.matches("""\d+""") =>
        val port = portString.toInt
        val grpcPort = ("80" + portString.takeRight(2)).toInt
        startNode(port, grpcPort)

      case None =>
        throw new IllegalArgumentException("port number required argument")
    }
  }

  def startNode(port: Int, grpcPort: Int): Unit = {
    val system =
      ActorSystem[Nothing](Guardian(), "Shopping", config(port, grpcPort))

    if (Cluster(system).selfMember.hasRole("read-model"))
      createTables(system)
  }

  def config(port: Int, grpcPort: Int): Config =
    ConfigFactory
      .parseString(s"""
      akka.remote.artery.canonical.port = $port
      shopping-cart.grpc.port = $grpcPort
       """)
      .withFallback(ConfigFactory.load())

  def createTables(system: ActorSystem[_]): Unit = {
    // TODO: In production the keyspace and tables should not be created automatically.
    // ok to block here, main thread
    Await.result(CassandraProjection.createOffsetTableIfNotExists()(system), 30.seconds)

    // use same keyspace for the item_popularity table as the offset store
    val keyspace = system.settings.config.getString("akka.projection.cassandra.offset-store.keyspace")
    val session = CassandraSessionRegistry(system).sessionFor("akka.projection.cassandra.session-config")
    Await.result(ItemPopularityRepositoryImpl.createItemPopularityTable(session, keyspace), 30.seconds)

    LoggerFactory.getLogger("sample.shoppingcart.Main").info("Created keyspace [{}] and tables", keyspace)
  }

}

object Guardian {
  def apply(): Behavior[Nothing] = {
    Behaviors.setup[Nothing](context => new Guardian(context))
  }
}

class Guardian(context: ActorContext[Nothing]) extends AbstractBehavior[Nothing](context) {
  val system = context.system

  val grpcPort = system.settings.config.getInt("shopping-cart.grpc.port")
  val projectionParallelism = system.settings.config.getInt("shopping-cart.projection-parallelism")

  ShoppingCart.init(system, projectionParallelism)

  val session = CassandraSessionRegistry(system).sessionFor("akka.projection.cassandra.session-config")
  // use same keyspace for the item_popularity table as the offset store
  val itemPopularityKeyspace = system.settings.config.getString("akka.projection.cassandra.offset-store.keyspace")
  val itemPopularityRepository =
    new ItemPopularityRepositoryImpl(session, itemPopularityKeyspace)(system.executionContext)

  if (Cluster(system).selfMember.hasRole("read-model")) {
    PublishEventsProjection.init(system, projectionParallelism)

    ItemPopularityProjection.init(system, itemPopularityRepository, projectionParallelism)

    val orderService = orderServiceClient(system)
    SendOrderProjection.init(system, projectionParallelism, orderService)
  }

  // can be overridden in tests
  protected def orderServiceClient(system: ActorSystem[_]): ShoppingOrderService = {
    val orderServiceGrpcPort = system.settings.config.getInt("shopping-order.grpc.port")
    val orderServiceClientSettings =
      GrpcClientSettings.connectToServiceAt("127.0.0.1", orderServiceGrpcPort)(system).withTls(false)
    val orderServiceClient = ShoppingOrderServiceClient(orderServiceClientSettings)(system)
    orderServiceClient
  }

  new ShoppingCartServer(grpcPort, system, itemPopularityRepository).start()

  override def onMessage(msg: Nothing): Behavior[Nothing] =
    this
}
