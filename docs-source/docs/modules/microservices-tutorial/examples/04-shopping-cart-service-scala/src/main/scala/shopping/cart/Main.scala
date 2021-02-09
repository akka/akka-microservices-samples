package shopping.cart

import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import shopping.cart.repository.ItemPopularityRepositoryImpl
import shopping.cart.repository.ScalikeJdbcSetup

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

  // tag::ItemPopularityProjection[]
  ScalikeJdbcSetup.init(system) // <1>
  // end::ItemPopularityProjection[]

  AkkaManagement(system).start()
  ClusterBootstrap(system).start()

  ShoppingCart.init(system)

  // tag::ItemPopularityProjection[]
  val itemPopularityRepository = new ItemPopularityRepositoryImpl() // <2>
  ItemPopularityProjection.init(system, itemPopularityRepository) // <3>
  // end::ItemPopularityProjection[]

  val grpcInterface =
    system.settings.config.getString("shopping-cart-service.grpc.interface")
  val grpcPort =
    system.settings.config.getInt("shopping-cart-service.grpc.port")
  val grpcService =
    new ShoppingCartServiceImpl(system, itemPopularityRepository)
  ShoppingCartServer.start(grpcInterface, grpcPort, system, grpcService)

  override def onMessage(msg: Nothing): Behavior[Nothing] =
    this
}
