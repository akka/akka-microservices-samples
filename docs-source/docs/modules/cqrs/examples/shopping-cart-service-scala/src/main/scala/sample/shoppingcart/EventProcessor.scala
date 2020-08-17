package sample.shoppingcart

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.ClusterShardingSettings
import akka.cluster.sharding.typed.ShardedDaemonProcessSettings
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.Offset
import akka.projection.ProjectionBehavior
import akka.projection.ProjectionId
import akka.projection.cassandra.scaladsl.CassandraProjection
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.scaladsl.AtLeastOnceProjection
import akka.projection.scaladsl.SourceProvider

object EventProcessor {

  private def createProjectionFor(
      system: ActorSystem[_],
      index: Int): AtLeastOnceProjection[Offset, EventEnvelope[ShoppingCart.Event]] = {
    val tag = s"${ShoppingCart.TagPrefix}-$index"
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

  def init(system: ActorSystem[_], projectionParallelism: Int): Unit = {
    // we only want to run the daemon processes on the read-model nodes
    val shardingSettings = ClusterShardingSettings(system)
    val shardedDaemonProcessSettings =
      ShardedDaemonProcessSettings(system).withShardingSettings(shardingSettings.withRole("read-model"))

    ShardedDaemonProcess(system).init(
      name = "ShoppingCartProjection",
      projectionParallelism,
      index => ProjectionBehavior(createProjectionFor(system, index)),
      shardedDaemonProcessSettings,
      Some(ProjectionBehavior.Stop))
  }

}
