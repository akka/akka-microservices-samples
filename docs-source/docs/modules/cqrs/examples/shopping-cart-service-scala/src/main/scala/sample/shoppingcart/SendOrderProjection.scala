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
import sample.shoppingorder.proto.ShoppingOrderService

object SendOrderProjection {

  private def createProjectionFor(
      system: ActorSystem[_],
      orderService: ShoppingOrderService,
      index: Int): AtLeastOnceProjection[Offset, EventEnvelope[ShoppingCart.Event]] = {
    val tag = s"${ShoppingCart.TagPrefix}-$index"
    val sourceProvider: SourceProvider[Offset, EventEnvelope[ShoppingCart.Event]] =
      EventSourcedProvider.eventsByTag[ShoppingCart.Event](
        system = system,
        readJournalPluginId = CassandraReadJournal.Identifier,
        tag = tag)

    CassandraProjection.atLeastOnce(
      projectionId = ProjectionId("orders", tag),
      sourceProvider,
      handler = () => new SendOrderProjectionHandler(system, orderService))
  }

  def init(system: ActorSystem[_], projectionParallelism: Int, orderService: ShoppingOrderService): Unit = {
    // we only want to run the daemon processes on the read-model nodes
    val shardingSettings = ClusterShardingSettings(system)
    val shardedDaemonProcessSettings =
      ShardedDaemonProcessSettings(system).withShardingSettings(shardingSettings.withRole("read-model"))

    ShardedDaemonProcess(system).init(
      name = "SendOrderProjection",
      projectionParallelism,
      index => ProjectionBehavior(createProjectionFor(system, orderService, index)),
      shardedDaemonProcessSettings,
      Some(ProjectionBehavior.Stop))
  }

}
