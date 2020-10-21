package shopping.cart;

import akka.actor.typed.ActorSystem;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shopping.cart.proto.AddItemRequest;
import shopping.cart.proto.Cart;
import shopping.cart.proto.Item;
import shopping.cart.proto.ShoppingCartService;

public final class ShoppingCartServiceImpl implements ShoppingCartService {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final Duration timeout;
  private final ClusterSharding sharding;

  public ShoppingCartServiceImpl(ActorSystem<?> system) {
    timeout = system.settings().config().getDuration("shopping-cart-service.ask-timeout");
    sharding = ClusterSharding.get(system);
  }

  @Override
  public CompletionStage<Cart> addItem(AddItemRequest in) { // <1>
    logger.info("addItem {} to cart {}", in.getItemId(), in.getCartId());
    Item item = Item.newBuilder().setItemId(in.getItemId()).setQuantity(in.getQuantity()).build();
    Cart cart = Cart.newBuilder().addItems(item).build();
    return CompletableFuture.completedFuture(cart);
  }
}
