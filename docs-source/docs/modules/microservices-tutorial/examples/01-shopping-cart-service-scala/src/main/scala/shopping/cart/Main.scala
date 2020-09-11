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
    ActorSystem[Nothing](Main(), "Cart") // <1>
  }

  def apply(): Behavior[Nothing] = {
    Behaviors.setup[Nothing](context => new Main(context))
  }
}

class Main(context: ActorContext[Nothing]) extends AbstractBehavior[Nothing](context) {
  val system = context.system

  AkkaManagement(system).start() // <2>
  ClusterBootstrap(system).start()

  val grpcInterface = system.settings.config.getString("shopping-cart-service.grpc.interface")
  val grpcPort = system.settings.config.getInt("shopping-cart-service.grpc.port")
  ShoppingCartServer.start(grpcInterface, grpcPort, system) // <3>

  override def onMessage(msg: Nothing): Behavior[Nothing] = // <4>
    this
}
