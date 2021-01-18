package shopping.cart

import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import shopping.cart.repository.{ DBsFromConfig, ItemPopularityRepositoryImpl }

// tag::SendOrderProjection[]
import shopping.order.proto.{ ShoppingOrderService, ShoppingOrderServiceClient }
import akka.grpc.GrpcClientSettings

// end::SendOrderProjection[]

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

  DBsFromConfig.init(system)
  AkkaManagement(system).start()
  ClusterBootstrap(system).start()

  ShoppingCart.init(system)

  // tag::ItemPopularityProjection[]
  val itemPopularityRepository = new ItemPopularityRepositoryImpl() // <1>
  ItemPopularityProjection.init(system, itemPopularityRepository) // <2>
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
