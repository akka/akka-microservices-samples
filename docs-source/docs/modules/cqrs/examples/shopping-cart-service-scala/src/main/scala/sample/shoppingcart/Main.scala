package sample.shoppingcart

import scala.concurrent.Await
import scala.concurrent.duration._

import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.Cluster
import akka.projection.cassandra.scaladsl.CassandraProjection
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

object Main {

  def main(args: Array[String]): Unit = {
    args.headOption match {

      case Some(portString) if portString.matches("""\d+""") =>
        val port = portString.toInt
        val grpcPort = ("80" + portString.takeRight(2)).toInt
        startNode(port, grpcPort)

      case None =>
        throw new IllegalArgumentException("port number, or cassandra required argument")
    }
  }

  def startNode(port: Int, grpcPort: Int): Unit = {
    val system =
      ActorSystem[Nothing](Guardian(), "Shopping", config(port, grpcPort))

    if (Cluster(system).selfMember.hasRole("read-model"))
      createTables(system)
  }

  def config(port: Int, grpcPort: Int): Config =
    ConfigFactory
      .parseString(s"""
      akka.remote.artery.canonical.port = $port
      shopping.grpc.port = $grpcPort
       """)
      .withFallback(ConfigFactory.load())

  def createTables(system: ActorSystem[_]): Unit = {
    // TODO: In production the keyspace and tables should not be created automatically.
    // ok to block here, main thread
    Await.ready(CassandraProjection.createOffsetTableIfNotExists()(system), 30.seconds)
  }

}

object Guardian {

  def apply(): Behavior[Nothing] = {
    Behaviors.setup[Nothing] { context =>
      val system = context.system

      val settings = EventProcessorSettings(system)

      val grpcPort = context.system.settings.config.getInt("shopping.grpc.port")

      ShoppingCart.init(system, settings)

      if (Cluster(system).selfMember.hasRole("read-model")) {
        EventProcessor.init(system, settings)
      }

      new ShoppingCartServer(grpcPort, context.system).start()

      Behaviors.empty
    }
  }
}
