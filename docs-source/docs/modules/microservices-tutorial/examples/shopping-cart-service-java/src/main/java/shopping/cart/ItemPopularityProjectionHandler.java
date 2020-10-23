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

    if (event instanceof ShoppingCart.ItemAdded) { // <3>
      ShoppingCart.ItemAdded added = (ShoppingCart.ItemAdded) event;
      CompletionStage<Done> result = this.repo.update(added.itemId, added.quantity);
      result.thenAccept(done -> logItemCount(added.itemId));
      return result;
      // end::handler[]
    } else if (event instanceof ShoppingCart.ItemQuantityAdjusted) {
      ShoppingCart.ItemQuantityAdjusted adjusted = (ShoppingCart.ItemQuantityAdjusted) event;
      CompletionStage<Done> result =
          this.repo.update(adjusted.itemId, adjusted.newQuantity - adjusted.oldQuantity);
      result.thenAccept(done -> logItemCount(adjusted.itemId));
      return result;
    } else if (event instanceof ShoppingCart.ItemRemoved) {
      ShoppingCart.ItemRemoved removed = (ShoppingCart.ItemRemoved) event;
      CompletionStage<Done> result = this.repo.update(removed.itemId, -removed.oldQuantity);
      result.thenAccept(done -> logItemCount(removed.itemId));
      return result;
      // tag::handler[]
    } else {
      // skip all other events, such as `CheckedOut`
      return CompletableFuture.completedFuture(Done.getInstance());
    }
  }

  private void logItemCount(String itemId) {
    repo.getItem(itemId)
        .thenAccept(
            optCount ->
                logger.info(
                    "ItemPopularityProjectionHandler({}) item popularity for '{}': [{}]",
                    this.tag,
                    itemId,
                    optCount.orElse(0L)));
  }
}
// end::handler[]
