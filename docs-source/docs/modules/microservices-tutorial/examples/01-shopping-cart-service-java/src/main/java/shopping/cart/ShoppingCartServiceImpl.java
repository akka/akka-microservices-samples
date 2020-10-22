package shopping.cart;

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

  @Override
  public CompletionStage<Cart> addItem(AddItemRequest in) { // <1>
    logger.info("addItem {} to cart {}", in.getItemId(), in.getCartId());
    Item item = Item.newBuilder().setItemId(in.getItemId()).setQuantity(in.getQuantity()).build();
    Cart cart = Cart.newBuilder().addItems(item).build();
    return CompletableFuture.completedFuture(cart);
  }
}
