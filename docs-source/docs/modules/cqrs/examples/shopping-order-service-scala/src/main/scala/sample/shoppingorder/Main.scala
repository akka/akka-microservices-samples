package sample.shoppingorder

import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.management.scaladsl.AkkaManagement

object Main {

  def main(args: Array[String]): Unit = {
    ActorSystem[Nothing](Guardian(), "ShoppingOrder")
  }

}

object Guardian {

  def apply(): Behavior[Nothing] = {
    Behaviors.setup[Nothing] { context =>
      val system = context.system

      AkkaManagement(system).start()

      val grpcInterface = context.system.settings.config.getString("shopping-order.grpc.interface")
      val grpcPort = context.system.settings.config.getInt("shopping-order.grpc.port")
      ShoppingOrderServer.start(grpcInterface, grpcPort, system)

      Behaviors.empty
    }
  }
}
