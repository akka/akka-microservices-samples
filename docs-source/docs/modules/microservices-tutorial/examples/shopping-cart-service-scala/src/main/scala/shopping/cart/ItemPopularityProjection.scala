// tag::projection[]
package shopping.cart

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.ShardedDaemonProcessSettings
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.Offset
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.jdbc.scaladsl.JdbcProjection
import akka.projection.scaladsl.{ ExactlyOnceProjection, SourceProvider }
import akka.projection.{ ProjectionBehavior, ProjectionId }
import shopping.cart.repository.{ ItemPopularityRepository, ScalikeJdbcSession }

object ItemPopularityProjection {
  // tag::howto-read-side-without-role[]
  def init(
      system: ActorSystem[_],
      repository: ItemPopularityRepository): Unit = {
    ShardedDaemonProcess(system).init( // <1>
      name = "ItemPopularityProjection",
      ShoppingCart.tags.size,
      index =>
        ProjectionBehavior(createProjectionFor(system, repository, index)),
      ShardedDaemonProcessSettings(system),
      Some(ProjectionBehavior.Stop))
  }
  // end::howto-read-side-without-role[]

  private def createProjectionFor(
      system: ActorSystem[_],
      repository: ItemPopularityRepository,
      index: Int)
      : ExactlyOnceProjection[Offset, EventEnvelope[ShoppingCart.Event]] = {
    val tag = ShoppingCart.tags(index) // <2>

    val sourceProvider
        : SourceProvider[Offset, EventEnvelope[ShoppingCart.Event]] = // <3>
      EventSourcedProvider.eventsByTag[ShoppingCart.Event](
        system = system,
        readJournalPluginId = JdbcReadJournal.Identifier, // <4>
        tag = tag)

    JdbcProjection.exactlyOnce( // <5>
      projectionId = ProjectionId("ItemPopularityProjection", tag),
      sourceProvider,
      handler = () =>
        new ItemPopularityProjectionHandler(tag, system, repository), // <6>
      sessionFactory = () => new ScalikeJdbcSession())(system)
  }

}
// end::projection[]
