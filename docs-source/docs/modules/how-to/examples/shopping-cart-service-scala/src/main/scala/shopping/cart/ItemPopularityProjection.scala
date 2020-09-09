package shopping.cart

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.ShardedDaemonProcessSettings
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import akka.persistence.query.Offset
import akka.projection.ProjectionBehavior
import akka.projection.eventsourced.EventEnvelope
import akka.projection.scaladsl.AtLeastOnceProjection

object ItemPopularityProjection {
  // tag::read-side-with-role[]
  def init(system: ActorSystem[_], repository: ItemPopularityRepository): Unit = {
    ShardedDaemonProcess(system).init(
      name = "ItemPopularityProjection",
      ShoppingCart.tags.size,
      index => ProjectionBehavior(createProjectionFor(system, repository, index)),
      ShardedDaemonProcessSettings(system).withRole("projections"),
      Some(ProjectionBehavior.Stop))
  }
  // end::read-side-with-role[]

  private def createProjectionFor(
      system: ActorSystem[_],
      repository: ItemPopularityRepository,
      index: Int): AtLeastOnceProjection[Offset, EventEnvelope[ShoppingCart.Event]] = ???
}

object ItemPopularityProjectionDedicatedRole {
  // tag::read-side-with-dedicated-role[]
  def init(system: ActorSystem[_], repository: ItemPopularityRepository): Unit = {
    ShardedDaemonProcess(system).init(
      name = "ItemPopularityProjection",
      ShoppingCart.tags.size,
      index => ProjectionBehavior(createProjectionFor(system, repository, index)),
      ShardedDaemonProcessSettings(system).withRole("projections-popularity"),
      Some(ProjectionBehavior.Stop))
  }
  // end::read-side-with-dedicated-role[]

  private def createProjectionFor(
      system: ActorSystem[_],
      repository: ItemPopularityRepository,
      index: Int): AtLeastOnceProjection[Offset, EventEnvelope[ShoppingCart.Event]] = ???
}
