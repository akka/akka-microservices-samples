package shopping.cart

import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.sharding.typed.scaladsl.Entity
import akka.cluster.sharding.typed.scaladsl.EntityContext
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey

object ShoppingCart {

  sealed trait Command

  sealed trait Event {
    def cartId: String
  }
  val EntityKey: EntityTypeKey[Command] = ???
  def apply(cartId: String, projectionTag: String): Behavior[Command] = ???
  val tags: Seq[String] = ???

  // tag::write-side-with-role[]
  def init(system: ActorSystem[_]): Unit = {
    val behaviorFactory: EntityContext[Command] => Behavior[Command] = { entityContext =>
      val i = math.abs(entityContext.entityId.hashCode % tags.size)
      val selectedTag = tags(i)
      ShoppingCart(entityContext.entityId, selectedTag)
    }
    val entity: Entity[Command, ShardingEnvelope[Command]] =
      Entity(EntityKey)(behaviorFactory).withRole("eventsourcing")
    ClusterSharding(system).init(entity)
  }
  // end::write-side-with-role[]

}
