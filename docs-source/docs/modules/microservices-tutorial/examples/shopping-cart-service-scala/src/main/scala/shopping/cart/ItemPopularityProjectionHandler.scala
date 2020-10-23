// tag::handler[]
package shopping.cart

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Success

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
      envelope: EventEnvelope[ShoppingCart.Event])
      : Future[Done] = { // <2>
    val processed = envelope.event match { // <3>
      case ShoppingCart.ItemAdded(_, itemId, quantity) =>
        repo.update(itemId, quantity)

      // end::handler[]
      case ShoppingCart.ItemQuantityAdjusted(
            _,
            itemId,
            newQuantity,
            oldQuantity) =>
        repo.update(itemId, newQuantity - oldQuantity)

      case ShoppingCart.ItemRemoved(
            _,
            itemId,
            oldQuantity) =>
        repo.update(itemId, 0 - oldQuantity)
      // tag::handler[]

      case _: ShoppingCart.CheckedOut =>
        Future.successful(Done)
    }
    processed.onComplete {
      case Success(_) => logItemCount(envelope.event)
      case _          => ()
    }
    processed
  }

  private def logItemCount(
      event: ShoppingCart.Event): Unit =
    event match {
      case itemEvent: ShoppingCart.ItemEvent =>
        val itemId = itemEvent.itemId
        repo.getItem(itemId).foreach {
          case Some(count) =>
            log.info(
              "ItemPopularityProjectionHandler({}) item popularity for '{}': [{}]",
              tag,
              itemId,
              count)
          case None =>
            log.info(
              "ItemPopularityProjectionHandler({}) item popularity for '{}': [0]",
              tag,
              itemId)
        }
      case _ => ()
    }

}
// end::handler[]
