package sample.shoppinganalytics

import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object Main {

  def main(args: Array[String]): Unit = {
    ActorSystem[Nothing](Guardian(), "ShoppingAnalytics")
  }
}

object Guardian {

  def apply(): Behavior[Nothing] = {
    Behaviors.setup[Nothing] { context =>
      val system = context.system

      ShoppingCartEventConsumer.init(system)

      Behaviors.empty
    }
  }
}
