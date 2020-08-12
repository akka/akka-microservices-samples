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
    ConfigFactory.parseString(s"""
      akka.remote.artery.canonical.port = $port
      shopping.grpc.port = $grpcPort
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

  def createProjectionFor(
      system: ActorSystem[_],
      settings: EventProcessorSettings,
      index: Int): AtLeastOnceProjection[Offset, EventEnvelope[ShoppingCart.Event]] = {
    val tag = s"${settings.tagPrefix}-$index"
    val sourceProvider = EventSourcedProvider.eventsByTag[ShoppingCart.Event](
      system = system,
      readJournalPluginId = CassandraReadJournal.Identifier,
      tag = tag)
    CassandraProjection.atLeastOnce(
      projectionId = ProjectionId("shopping-carts", tag),
      sourceProvider,
      handler = () => new ShoppingCartProjectionHandler(tag, system))
  }

  def apply(): Behavior[Nothing] = {
    Behaviors.setup[Nothing] { context =>
      val system = context.system

      val settings = EventProcessorSettings(system)

      val grpcPort = context.system.settings.config.getInt("shopping.grpc.port")

      ShoppingCart.init(system, settings)

      if (Cluster(system).selfMember.hasRole("read-model")) {

        // we only want to run the daemon processes on the read-model nodes
        val shardingSettings = ClusterShardingSettings(system)
        val shardedDaemonProcessSettings =
          ShardedDaemonProcessSettings(system).withShardingSettings(shardingSettings.withRole("read-model"))

        ShardedDaemonProcess(system).init(
          name = "ShoppingCartProjection",
          settings.parallelism,
          n => ProjectionBehavior(createProjectionFor(system, settings, n)),
          shardedDaemonProcessSettings,
          Some(ProjectionBehavior.Stop))
      }

      new ShoppingCartServer(grpcPort, context.system).start()

      Behaviors.empty
    }
  }
}
