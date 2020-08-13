package sample.shoppingcart

import scala.concurrent.Await
import scala.concurrent.duration._

import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.{ ClusterShardingSettings, ShardedDaemonProcessSettings }
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import akka.cluster.typed.Cluster
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.Offset
import akka.projection.{ ProjectionBehavior, ProjectionId }
import akka.projection.scaladsl.AtLeastOnceProjection
import akka.projection.cassandra.scaladsl.CassandraProjection
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.stream.alpakka.cassandra.scaladsl.CassandraSessionRegistry
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

object Main {

  def main(args: Array[String]): Unit = {
    args.headOption match {

      case Some(portString) if portString.matches("""\d+""") =>
        val port = portString.toInt
        val httpPort = ("80" + portString.takeRight(2)).toInt
        startNode(port, httpPort)

      case None =>
        throw new IllegalArgumentException("port number, or cassandra required argument")
    }
  }

  def startNode(port: Int, httpPort: Int): Unit = {
    val system =
      ActorSystem[Nothing](Guardian(), "Shopping", config(port, httpPort))

    if (Cluster(system).selfMember.hasRole("read-model"))
      createTables(system)
  }

  def config(port: Int, httpPort: Int): Config =
    ConfigFactory.parseString(s"""
      akka.remote.artery.canonical.port = $port
      shopping.http.port = $httpPort
       """).withFallback(ConfigFactory.load())

  def createTables(system: ActorSystem[_]): Unit = {
    val session =
      CassandraSessionRegistry(system).sessionFor("alpakka.cassandra")

    val keyspace = system.settings.config.getString("akka.projection.cassandra.offset-store.keyspace")

    // TODO use real replication strategy in real application
    val keyspaceStmt =
      s"""
      CREATE KEYSPACE IF NOT EXISTS $keyspace
      WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 }
      """

    val offsetTableStmt =
      s"""
      CREATE TABLE IF NOT EXISTS $keyspace.offset_store (
        projection_name text,
        partition int,
        projection_key text,
        offset text,
        manifest text,
        last_updated timestamp,
        PRIMARY KEY ((projection_name, partition), projection_key)
      )
      """

    // ok to block here, main thread
    Await.ready(session.executeDDL(keyspaceStmt), 30.seconds)
    system.log.info(s"Created $keyspace keyspace")
    Await.ready(session.executeDDL(offsetTableStmt), 30.seconds)
    system.log.info(s"Created $keyspace.offset_store table")

  }

}

object Guardian {

  def apply(): Behavior[Nothing] = {
    Behaviors.setup[Nothing] { context =>
      val system = context.system

      val settings = EventProcessorSettings(system)

      val httpPort = context.system.settings.config.getInt("shopping.http.port")

      ShoppingCart.init(system, settings)

      if (Cluster(system).selfMember.hasRole("read-model")) {
        EventProcessor(system, settings)
      }

      val routes = new ShoppingCartRoutes()(context.system)
      new ShoppingCartServer(routes.shopping, httpPort, context.system).start()

      Behaviors.empty
    }
  }
}
