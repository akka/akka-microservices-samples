package shopping.order

import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement

object Main {

  def main(args: Array[String]): Unit = {
    ActorSystem[Nothing](Guardian(), "ShoppingOrder")
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

  val grpcInterface = context.system.settings.config.getString("shopping-order.grpc.interface")
  val grpcPort = context.system.settings.config.getInt("shopping-order.grpc.port")
  ShoppingOrderServer.start(grpcInterface, grpcPort, system)

  // can be overridden in tests
  protected def startAkkaManagement(): Unit = {
    AkkaManagement(system).start()
    ClusterBootstrap(system).start()
  }

  override def onMessage(msg: Nothing): Behavior[Nothing] =
    this

}
