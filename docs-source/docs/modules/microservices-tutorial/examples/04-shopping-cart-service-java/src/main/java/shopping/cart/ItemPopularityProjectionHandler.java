// tag::handler[]
package shopping.cart;

import akka.projection.eventsourced.EventEnvelope;
import akka.projection.jdbc.javadsl.JdbcHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shopping.cart.repository.HibernateJdbcSession;
import shopping.cart.repository.ItemPopularityRepository;

public final class ItemPopularityProjectionHandler
    extends JdbcHandler<EventEnvelope<ShoppingCart.Event>, HibernateJdbcSession> { // <1>
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final String tag;
  private final ItemPopularityRepository repo;

  public ItemPopularityProjectionHandler(String tag, ItemPopularityRepository repo) {
    this.tag = tag;
    this.repo = repo;
  }

  private ItemPopularity findOrNew(String itemId) {
    return repo.findById(itemId).orElseGet(() -> new ItemPopularity(itemId, 0, 0));
  }

  @Override
  public void process(
      HibernateJdbcSession session, EventEnvelope<ShoppingCart.Event> envelope) { // <2>
    ShoppingCart.Event event = envelope.event();

    if (event instanceof ShoppingCart.ItemAdded) { // <3>
      ShoppingCart.ItemAdded added = (ShoppingCart.ItemAdded) event;
      String itemId = added.itemId;

      ItemPopularity existingItemPop = findOrNew(itemId);
      ItemPopularity updatedItemPop = existingItemPop.changeCount(added.quantity);
      repo.save(updatedItemPop);

      logger.info(
          "ItemPopularityProjectionHandler({}) item popularity for '{}': [{}]",
          this.tag,
          itemId,
          updatedItemPop.getCount());
      // end::handler[]
    } else if (event instanceof ShoppingCart.ItemQuantityAdjusted) {
      ShoppingCart.ItemQuantityAdjusted adjusted = (ShoppingCart.ItemQuantityAdjusted) event;
      String itemId = adjusted.itemId;

      ItemPopularity existingItemPop = findOrNew(itemId);
      ItemPopularity updatedItemPop =
          existingItemPop.changeCount(adjusted.newQuantity - adjusted.oldQuantity);
      repo.save(updatedItemPop);

    } else if (event instanceof ShoppingCart.ItemRemoved) {
      ShoppingCart.ItemRemoved removed = (ShoppingCart.ItemRemoved) event;
      String itemId = removed.itemId;

      ItemPopularity existingItemPop = findOrNew(itemId);
      ItemPopularity updatedItemPop = existingItemPop.changeCount(-removed.oldQuantity);
      repo.save(updatedItemPop);
      // tag::handler[]
    } else {
      // skip all other events, such as `CheckedOut`
    }
  }
}
// end::handler[]
