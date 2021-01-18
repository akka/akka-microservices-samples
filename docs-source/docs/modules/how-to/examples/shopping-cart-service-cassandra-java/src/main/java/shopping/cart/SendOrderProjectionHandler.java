package shopping.cart;

import static akka.Done.done;

import akka.Done;
import akka.actor.typed.ActorSystem;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.EntityRef;
import akka.projection.eventsourced.EventEnvelope;
import akka.projection.javadsl.Handler;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shopping.order.proto.Item;
import shopping.order.proto.OrderRequest;
import shopping.order.proto.ShoppingOrderService;

public final class SendOrderProjectionHandler extends Handler<EventEnvelope<ShoppingCart.Event>> {

  private final Logger log = LoggerFactory.getLogger(getClass());
  private final ClusterSharding sharding;
  private final Duration timeout;
  private final ShoppingOrderService orderService;

  public SendOrderProjectionHandler(
      ActorSystem<?> system, ShoppingOrderService orderService) { // <1>
    sharding = ClusterSharding.get(system);
    timeout = system.settings().config().getDuration("shopping-cart-service.ask-timeout");
    this.orderService = orderService;
  }

  @Override
  public CompletionStage<Done> process(EventEnvelope<ShoppingCart.Event> envelope)
      throws Exception {
    if (envelope.event() instanceof ShoppingCart.CheckedOut) {
      ShoppingCart.CheckedOut checkedOut = (ShoppingCart.CheckedOut) envelope.event();
      return sendOrder(checkedOut);
    } else {
      return CompletableFuture.completedFuture(done());
    }
  }

  private CompletionStage<Done> sendOrder(ShoppingCart.CheckedOut checkout) {
    EntityRef<ShoppingCart.Command> entityRef =
        sharding.entityRefFor(ShoppingCart.ENTITY_KEY, checkout.cartId);
    CompletionStage<ShoppingCart.Summary> reply =
        entityRef.ask(replyTo -> new ShoppingCart.Get(replyTo), timeout);
    return reply.thenCompose(
        cart -> { // <2>
          List<Item> protoItems =
              cart.items.entrySet().stream()
                  .map(
                      entry ->
                          Item.newBuilder()
                              .setItemId(entry.getKey())
                              .setQuantity(entry.getValue())
                              .build())
                  .collect(Collectors.toList());
          log.info("Sending order of {} items for cart {}.", cart.items.size(), checkout.cartId);
          OrderRequest orderRequest =
              OrderRequest.newBuilder().setCartId(checkout.cartId).addAllItems(protoItems).build();
          return orderService.order(orderRequest).thenApply(response -> done()); // <3>
        });
  }
}
