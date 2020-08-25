package shopping.cart

import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement

object Main {

  def main(args: Array[String]): Unit = {
    ActorSystem[Nothing](Guardian(), "Cart")
  }

}

object Guardian {
  def apply(): Behavior[Nothing] = {
    Behaviors.setup[Nothing](context => new Guardian(context))
  }
}

class Guardian(context: ActorContext[Nothing]) extends AbstractBehavior[Nothing](context) {
  val system = context.system

  startAkkaManagement()

  // tag::ShoppingCart[]
  ShoppingCart.init(system)
  // end::ShoppingCart[]

  val grpcInterface = system.settings.config.getString("shopping-cart.grpc.interface")
  val grpcPort = system.settings.config.getInt("shopping-cart.grpc.port")
  ShoppingCartServer.start(grpcInterface, grpcPort, system)

  // can be overridden in tests
  protected def startAkkaManagement(): Unit = {
    AkkaManagement(system).start()
    ClusterBootstrap(system).start()
  }

  override def onMessage(msg: Nothing): Behavior[Nothing] =
    this
}
