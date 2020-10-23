// tag::handler[]
package shopping.cart;

import akka.Done;
import akka.projection.eventsourced.EventEnvelope;
import akka.projection.javadsl.Handler;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ItemPopularityProjectionHandler
    extends Handler<EventEnvelope<ShoppingCart.Event>> { // <1>
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final String tag;
  private final ItemPopularityRepository repo;

  public ItemPopularityProjectionHandler(String tag, ItemPopularityRepository repo) {
    this.tag = tag;
    this.repo = repo;
  }

  @Override
  public CompletionStage<Done> process(EventEnvelope<ShoppingCart.Event> envelope) // <2>
      throws Exception, Exception {
    ShoppingCart.Event event = envelope.event();

    CompletionStage<Done> dbEffect = null;
    if (event instanceof ShoppingCart.ItemAdded) { // <3>
      ShoppingCart.ItemAdded added = (ShoppingCart.ItemAdded) event;
      dbEffect = this.repo.update(added.itemId, added.quantity);
      // end::handler[]
    } else if (event instanceof ShoppingCart.ItemQuantityAdjusted) {
      ShoppingCart.ItemQuantityAdjusted adjusted = (ShoppingCart.ItemQuantityAdjusted) event;
      dbEffect = this.repo.update(adjusted.itemId, adjusted.newQuantity - adjusted.oldQuantity);
    } else if (event instanceof ShoppingCart.ItemRemoved) {
      ShoppingCart.ItemRemoved removed = (ShoppingCart.ItemRemoved) event;
      dbEffect = this.repo.update(removed.itemId, -removed.oldQuantity);
      // tag::handler[]
    } else {
      // skip all other events, such as `CheckedOut`
      dbEffect = CompletableFuture.completedFuture(Done.getInstance());
    }

    dbEffect.thenAccept(done -> logItemCount(event));

    return dbEffect;
  }

  /** Log the popularity of the item in every `ItemEvent` every `LogInterval`. */
  private void logItemCount(ShoppingCart.Event event) {
    if (event instanceof ShoppingCart.ItemEvent) {
      ShoppingCart.ItemEvent itemEvent = (ShoppingCart.ItemEvent) event;

      String itemId = itemEvent.itemId;
      repo.getItem(itemId)
          .thenAccept(
              opt ->
                  logger.info(
                      "ItemPopularityProjectionHandler({}) item popularity for '{}': [{}]",
                      this.tag,
                      itemId,
                      opt.orElse(0L)));
    }
  }
}
// end::handler[]
