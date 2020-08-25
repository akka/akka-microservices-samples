package shopping.cart

import akka.actor.typed.ActorSystem
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

object ItemPopularityProjection {
  def init(system: ActorSystem[_], repository: ItemPopularityRepository): Unit = {
    ShardedDaemonProcess(system).init( // <1>
      name = "ItemPopularityProjection",
      ShoppingCart.tags.size,
      index => ProjectionBehavior(createProjectionFor(system, repository, index)),
      ShardedDaemonProcessSettings(system),
      Some(ProjectionBehavior.Stop))
  }

  private def createProjectionFor(
      system: ActorSystem[_],
      repository: ItemPopularityRepository,
      index: Int): AtLeastOnceProjection[Offset, EventEnvelope[ShoppingCart.Event]] = {
    val tag = ShoppingCart.tags(index) // <2>
    // tag::projection[]
    val sourceProvider: SourceProvider[Offset, EventEnvelope[ShoppingCart.Event]] = // <3>
      EventSourcedProvider.eventsByTag[ShoppingCart.Event](
        system = system,
        readJournalPluginId = CassandraReadJournal.Identifier, // <4>
        tag = tag)

    CassandraProjection.atLeastOnce( // <5>
      projectionId = ProjectionId("item-popularity", tag),
      sourceProvider,
      handler = () => new ItemPopularityProjectionHandler(tag, system, repository)
    ) // <6>
    // end::projection[]
  }

}
