// tag::handler[]
package shopping.cart

import akka.actor.typed.ActorSystem
import akka.projection.eventsourced.EventEnvelope
import akka.projection.jdbc.scaladsl.JdbcHandler
import org.slf4j.LoggerFactory
import shopping.cart.repository.ScalikeJdbcSession

class ItemPopularityProjectionHandler(
    tag: String,
    system: ActorSystem[_],
    repo: ItemPopularityRepository)
    extends JdbcHandler[
      EventEnvelope[ShoppingCart.Event],
      ScalikeJdbcSession]() { // <1>

  private val log = LoggerFactory.getLogger(getClass)

  override def process(
      session: ScalikeJdbcSession,
      envelope: EventEnvelope[ShoppingCart.Event]): Unit = { // <2>
    envelope.event match { // <3>
      case ShoppingCart.ItemAdded(_, itemId, quantity) =>
        repo.update(itemId, quantity)
        logItemCount(itemId)

      // end::handler[]
      case ShoppingCart.ItemQuantityAdjusted(
            _,
            itemId,
            newQuantity,
            oldQuantity) =>
        repo.update(itemId, newQuantity - oldQuantity)
        logItemCount(itemId)

      case ShoppingCart.ItemRemoved(_, itemId, oldQuantity) =>
        repo.update(itemId, 0 - oldQuantity)
        logItemCount(itemId)

      // tag::handler[]

      case _: ShoppingCart.CheckedOut =>
    }
  }

  private def logItemCount(itemId: String): Unit = {
    log.info(
      "ItemPopularityProjectionHandler({}) item popularity for '{}': [{}]",
      tag,
      itemId,
      repo.getItem(itemId).getOrElse(0))
  }

}
// end::handler[]
