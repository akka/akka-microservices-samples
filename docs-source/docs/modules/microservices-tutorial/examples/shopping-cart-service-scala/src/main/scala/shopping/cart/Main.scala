package shopping.cart

import akka.actor.typed.{ ActorSystem, Behavior }
import akka.actor.typed.scaladsl.{ AbstractBehavior, ActorContext, Behaviors }
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement

// tag::SendOrderProjection[]
import shopping.order.proto.{ ShoppingOrderService, ShoppingOrderServiceClient }
import akka.grpc.GrpcClientSettings

// end::SendOrderProjection[]

// tag::ItemPopularityProjection[]
import akka.stream.alpakka.cassandra.scaladsl.CassandraSessionRegistry

// end::ItemPopularityProjection[]

object Main {

  // tag::createTables[]
  def main(args: Array[String]): Unit = {
    val system = ActorSystem[Nothing](Main(), "ShoppingCartService")
    createTables(system)
  }
  // end::createTables[]

  def apply(): Behavior[Nothing] = {
    Behaviors.setup[Nothing](context => new Main(context))
  }

  // tag::createTables[]
  def createTables(system: ActorSystem[_]): Unit = {
    import org.slf4j.LoggerFactory
    import akka.projection.cassandra.scaladsl.CassandraProjection
    import scala.concurrent.Await
    import scala.concurrent.duration._

    // TODO: In production the keyspace and tables should not be created automatically.
    // ok to block here, main thread
    Await.result(CassandraProjection.createOffsetTableIfNotExists()(system), 30.seconds)

    // use same keyspace for the item_popularity table as the offset store
    val keyspace = system.settings.config.getString("akka.projection.cassandra.offset-store.keyspace")
    val session = CassandraSessionRegistry(system).sessionFor("akka.persistence.cassandra")
    Await.result(ItemPopularityRepositoryImpl.createItemPopularityTable(session, keyspace), 30.seconds)

    LoggerFactory.getLogger("shopping.cart.Main").info("Created keyspace [{}] and tables", keyspace)
  }
  // end::createTables[]

}

class Main(context: ActorContext[Nothing]) extends AbstractBehavior[Nothing](context) {
  val system = context.system
  AkkaManagement(system).start()
  ClusterBootstrap(system).start()

  ShoppingCart.init(system)

  // tag::ItemPopularityProjection[]
  val session = CassandraSessionRegistry(system).sessionFor("akka.persistence.cassandra") // <1>
  // use same keyspace for the item_popularity table as the offset store
  val itemPopularityKeyspace = system.settings.config.getString("akka.projection.cassandra.offset-store.keyspace")
  val itemPopularityRepository =
    new ItemPopularityRepositoryImpl(session, itemPopularityKeyspace)(system.executionContext) // <2>

  ItemPopularityProjection.init(system, itemPopularityRepository) // <3>
  // end::ItemPopularityProjection[]

  val grpcInterface =
    system.settings.config.getString("shopping-cart-service.grpc.interface")
  val grpcPort = system.settings.config.getInt("shopping-cart-service.grpc.port")
  ShoppingCartServer.start(grpcInterface, grpcPort, system, itemPopularityRepository)

  // tag::PublishEventsProjection[]
  PublishEventsProjection.init(system)
  // end::PublishEventsProjection[]

  // tag::SendOrderProjection[]
  val orderService = orderServiceClient(system)
  SendOrderProjection.init(system, orderService)

  // can be overridden in tests
  protected def orderServiceClient(system: ActorSystem[_]): ShoppingOrderService = {
    val orderServiceClientSettings =
      GrpcClientSettings
        .connectToServiceAt(
          system.settings.config.getString("shopping-order-service.host"),
          system.settings.config.getInt("shopping-order-service.port"))(system)
        .withTls(false)
    val orderServiceClient =
      ShoppingOrderServiceClient(orderServiceClientSettings)(system)
    orderServiceClient
  }
  // end::SendOrderProjection[]

  override def onMessage(msg: Nothing): Behavior[Nothing] = {
    this
  }
}
