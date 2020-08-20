package sample.shoppingorder

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
        val grpcPort = ("90" + portString.takeRight(2)).toInt
        startNode(port, grpcPort)

      case None =>
        throw new IllegalArgumentException("port number required argument")
    }
  }

  def startNode(port: Int, grpcPort: Int): Unit = {
    ActorSystem[Nothing](Guardian(), "ShoppingOrder", config(port, grpcPort))
  }

  def config(port: Int, grpcPort: Int): Config =
    ConfigFactory
      .parseString(s"""
      akka.remote.artery.canonical.port = $port
      shopping-order.grpc.port = $grpcPort
       """)
      .withFallback(ConfigFactory.load())

}

object Guardian {

  def apply(): Behavior[Nothing] = {
    Behaviors.setup[Nothing] { context =>
      val system = context.system

      val grpcInterface = context.system.settings.config.getString("shopping-order.grpc.interface")
      val grpcPort = context.system.settings.config.getInt("shopping-order.grpc.port")
      new ShoppingOrderServer(grpcInterface, grpcPort, system).start()

      Behaviors.empty
    }
  }
}
