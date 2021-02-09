// tag::handler[]
package shopping.cart

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import akka.Done
import akka.actor.typed.ActorSystem
import akka.projection.eventsourced.EventEnvelope
import akka.projection.scaladsl.Handler
import org.slf4j.LoggerFactory

class ItemPopularityProjectionHandler(
    tag: String,
    system: ActorSystem[_],
    repo: ItemPopularityRepository)
    extends Handler[EventEnvelope[ShoppingCart.Event]]() { // <1>

  private val log = LoggerFactory.getLogger(getClass)
  private implicit val ec: ExecutionContext =
    system.executionContext

  override def process(
      envelope: EventEnvelope[ShoppingCart.Event]): Future[Done] = { // <2>
    envelope.event match { // <3>
      case ShoppingCart.ItemAdded(_, itemId, quantity) =>
        val result = repo.update(itemId, quantity)
        result.foreach(_ => logItemCount(itemId))
        result

      // end::handler[]
      case ShoppingCart.ItemQuantityAdjusted(
            _,
            itemId,
            newQuantity,
            oldQuantity) =>
        val result =
          repo.update(itemId, newQuantity - oldQuantity)
        result.foreach(_ => logItemCount(itemId))
        result

      case ShoppingCart.ItemRemoved(_, itemId, oldQuantity) =>
        val result = repo.update(itemId, 0 - oldQuantity)
        result.foreach(_ => logItemCount(itemId))
        result

      // tag::handler[]

      case _: ShoppingCart.CheckedOut =>
        Future.successful(Done)
    }
  }

  private def logItemCount(itemId: String): Unit = {
    repo.getItem(itemId).foreach { optCount =>
      log.info(
        "ItemPopularityProjectionHandler({}) item popularity for '{}': [{}]",
        tag,
        itemId,
        optCount.getOrElse(0))
    }
  }

}
// end::handler[]
