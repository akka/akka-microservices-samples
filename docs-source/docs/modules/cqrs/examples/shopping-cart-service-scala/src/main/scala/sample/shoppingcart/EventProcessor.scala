package sample.shoppingcart

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardedDaemonProcessSettings}
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.Offset
import akka.projection.{ProjectionBehavior, ProjectionId}
import akka.projection.cassandra.scaladsl.CassandraProjection
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.scaladsl.{AtLeastOnceProjection, SourceProvider}

import akka.actor.typed.ActorSystem
import com.typesafe.config.Config

object EventProcessorSettings {

  def apply(system: ActorSystem[_]): EventProcessorSettings = {
    apply(system.settings.config.getConfig("shopping.event-processor"))
  }

  def apply(config: Config): EventProcessorSettings = {
    val tagPrefix: String = config.getString("tag-prefix")
    val parallelism: Int = config.getInt("parallelism")
    EventProcessorSettings(tagPrefix, parallelism)
  }
}

final case class EventProcessorSettings(tagPrefix: String, parallelism: Int)

object EventProcessor {

  def createProjectionFor(
                           system: ActorSystem[_],
                           settings: EventProcessorSettings,
                           index: Int): AtLeastOnceProjection[Offset, EventEnvelope[ShoppingCart.Event]] = {
    val tag = s"${settings.tagPrefix}-$index"
    // tag::projection[]
    val sourceProvider: SourceProvider[Offset, EventEnvelope[ShoppingCart.Event]] =
      EventSourcedProvider.eventsByTag[ShoppingCart.Event](
        system = system,
        readJournalPluginId = CassandraReadJournal.Identifier,
        tag = tag)

    CassandraProjection.atLeastOnce(
      projectionId = ProjectionId("shopping-carts", tag),
      sourceProvider,
      handler = () => new ShoppingCartProjectionHandler(tag, system))
    // end::projection[]
  }

  def apply(system: ActorSystem[_], settings: EventProcessorSettings): Unit = {
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

}
