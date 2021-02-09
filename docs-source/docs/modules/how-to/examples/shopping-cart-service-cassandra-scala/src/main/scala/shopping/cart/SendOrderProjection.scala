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
import shopping.order.proto.ShoppingOrderService

object SendOrderProjection {

  def init(system: ActorSystem[_], orderService: ShoppingOrderService): Unit = {
    ShardedDaemonProcess(system).init(
      name = "SendOrderProjection",
      ShoppingCart.tags.size,
      index =>
        ProjectionBehavior(createProjectionFor(system, orderService, index)),
      ShardedDaemonProcessSettings(system),
      Some(ProjectionBehavior.Stop))
  }

  private def createProjectionFor(
      system: ActorSystem[_],
      orderService: ShoppingOrderService,
      index: Int): AtLeastOnceProjection[
    Offset,
    EventEnvelope[ShoppingCart.Event]] = { // <1>
    val tag = ShoppingCart.tags(index)
    val sourceProvider
        : SourceProvider[Offset, EventEnvelope[ShoppingCart.Event]] =
      EventSourcedProvider.eventsByTag[ShoppingCart.Event](
        system = system,
        readJournalPluginId = CassandraReadJournal.Identifier, // <2>
        tag = tag)

    CassandraProjection.atLeastOnce( // <3>
      projectionId = ProjectionId("SendOrderProjection", tag),
      sourceProvider,
      handler = () => new SendOrderProjectionHandler(system, orderService))
  }

}
