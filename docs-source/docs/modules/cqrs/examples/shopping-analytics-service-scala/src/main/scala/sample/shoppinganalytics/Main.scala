package sample.shoppinganalytics

import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.management.scaladsl.AkkaManagement

object Main {

  def main(args: Array[String]): Unit = {
    ActorSystem[Nothing](Guardian(), "ShoppingAnalytics")
  }
}

object Guardian {

  def apply(): Behavior[Nothing] = {
    Behaviors.setup[Nothing] { context =>
      val system = context.system

      AkkaManagement(system).start()

      ShoppingCartEventConsumer.init(system)

      Behaviors.empty
    }
  }
}
