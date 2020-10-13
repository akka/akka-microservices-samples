package shopping.cart

import java.time.Instant

import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.sharding.typed.scaladsl.Entity
import akka.cluster.sharding.typed.scaladsl.EntityContext
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey

object ShoppingCart {

  // tag::howto-crud-to-es-initial[]
  sealed trait Command
  final case class AddItem(itemId: String, quantity: Int)
      extends Command
  case object Checkout extends Command

  sealed trait Event {
    def cartId: String
  }
  final case class ItemAdded(
      cartId: String,
      itemId: String,
      quantity: Int)
      extends Event
  final case class CheckedOut(
      cartId: String,
      eventTime: Instant)
      extends Event
  // end::howto-crud-to-es-initial[]

  val EntityKey: EntityTypeKey[Command] = ???
  def apply(
      cartId: String,
      projectionTag: String): Behavior[Command] = ???
  val tags: Seq[String] = ???

  // tag::write-side-with-role[]
  def init(system: ActorSystem[_]): Unit = {
    val behaviorFactory
        : EntityContext[Command] => Behavior[Command] = {
      entityContext =>
        val i = math.abs(
          entityContext.entityId.hashCode % tags.size)
        val selectedTag = tags(i)
        ShoppingCart(entityContext.entityId, selectedTag)
    }
    ClusterSharding(system).init(
      Entity(EntityKey)(behaviorFactory)
        .withRole("eventsourcing"))
  }
  // end::write-side-with-role[]

}
