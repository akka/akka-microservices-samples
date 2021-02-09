package shopping.cart

import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
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

  def main(args: Array[String]): Unit = {
    ActorSystem[Nothing](Main(), "ShoppingCartService")
  }

  def apply(): Behavior[Nothing] = {
    Behaviors.setup[Nothing](context => new Main(context))
  }
}

class Main(context: ActorContext[Nothing])
    extends AbstractBehavior[Nothing](context) {
  val system = context.system
  AkkaManagement(system).start()
  ClusterBootstrap(system).start()

  ShoppingCart.init(system)

  // tag::ItemPopularityProjection[]
  val session = CassandraSessionRegistry(system).sessionFor(
    "akka.persistence.cassandra"
  ) // <1>
  // use same keyspace for the item_popularity table as the offset store
  val itemPopularityKeyspace =
    system.settings.config
      .getString("akka.projection.cassandra.offset-store.keyspace")
  val itemPopularityRepository =
    new ItemPopularityRepositoryImpl(session, itemPopularityKeyspace)(
      system.executionContext
    ) // <2>

  ItemPopularityProjection.init(system, itemPopularityRepository) // <3>
  // end::ItemPopularityProjection[]

  val grpcInterface =
    system.settings.config.getString("shopping-cart-service.grpc.interface")
  val grpcPort =
    system.settings.config.getInt("shopping-cart-service.grpc.port")
  val grpcService =
    new ShoppingCartServiceImpl(system, itemPopularityRepository)
  ShoppingCartServer.start(grpcInterface, grpcPort, system, grpcService)

  // tag::PublishEventsProjection[]
  PublishEventsProjection.init(system)
  // end::PublishEventsProjection[]

  // tag::SendOrderProjection[]
  val orderService = orderServiceClient(system)
  SendOrderProjection.init(system, orderService)

  // can be overridden in tests
  protected def orderServiceClient(
      system: ActorSystem[_]): ShoppingOrderService = {
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

  override def onMessage(msg: Nothing): Behavior[Nothing] =
    this
}
