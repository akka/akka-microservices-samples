package sample.shoppinganalytics

import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

object Main {

  def main(args: Array[String]): Unit = {
    args.headOption match {

      case Some(portString) if portString.matches("""\d+""") =>
        val port = portString.toInt
        startNode(port)

      case None =>
        throw new IllegalArgumentException("port number required argument")
    }
  }

  def startNode(port: Int): Unit = {
    ActorSystem[Nothing](Guardian(), "ShoppingAnalytics", config(port))
  }

  def config(port: Int): Config =
    ConfigFactory
      .parseString(s"""
      akka.remote.artery.canonical.port = $port
       """)
      .withFallback(ConfigFactory.load())

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
