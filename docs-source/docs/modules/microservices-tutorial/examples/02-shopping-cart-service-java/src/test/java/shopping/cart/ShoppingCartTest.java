package shopping.cart;

import static akka.persistence.testkit.javadsl.EventSourcedBehaviorTestKit.CommandResultWithReply;
import static org.junit.Assert.*;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.pattern.StatusReply;
import akka.persistence.testkit.javadsl.EventSourcedBehaviorTestKit;
import com.typesafe.config.ConfigFactory;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

public class ShoppingCartTest {

  private static final String CART_ID = "testCart";

  @ClassRule
  public static final TestKitJunitResource testKit =
      new TestKitJunitResource(
          ConfigFactory.parseString(
                  "akka.actor.serialization-bindings {\n"
                      + "  \"shopping.cart.CborSerializable\" = jackson-cbor\n"
                      + "}")
              .withFallback(EventSourcedBehaviorTestKit.config()));

  private EventSourcedBehaviorTestKit<ShoppingCart.Command, ShoppingCart.Event, ShoppingCart.State>
      eventSourcedTestKit =
          EventSourcedBehaviorTestKit.create(testKit.system(), ShoppingCart.create(CART_ID));

  @Before
  public void beforeEach() {
    eventSourcedTestKit.clear();
  }

  @Test
  public void addAnItemToCart() {
    CommandResultWithReply<
            ShoppingCart.Command,
            ShoppingCart.Event,
            ShoppingCart.State,
            StatusReply<ShoppingCart.Summary>>
        result =
            eventSourcedTestKit.runCommand(replyTo -> new ShoppingCart.AddItem("foo", 42, replyTo));
    assertTrue(result.reply().isSuccess());
    ShoppingCart.Summary summary = result.reply().getValue();
    assertEquals(1, summary.items.size());
    assertEquals(42, summary.items.get("foo").intValue());
    assertEquals(new ShoppingCart.ItemAdded(CART_ID, "foo", 42), result.event());
  }

  @Test
  public void rejectAlreadyAddedItem() {
    CommandResultWithReply<
            ShoppingCart.Command,
            ShoppingCart.Event,
            ShoppingCart.State,
            StatusReply<ShoppingCart.Summary>>
        result1 =
            eventSourcedTestKit.runCommand(replyTo -> new ShoppingCart.AddItem("foo", 42, replyTo));
    assertTrue(result1.reply().isSuccess());
    CommandResultWithReply<
            ShoppingCart.Command,
            ShoppingCart.Event,
            ShoppingCart.State,
            StatusReply<ShoppingCart.Summary>>
        result2 =
            eventSourcedTestKit.runCommand(replyTo -> new ShoppingCart.AddItem("foo", 42, replyTo));
    assertTrue(result2.reply().isError());
    assertTrue(result2.hasNoEvents());
  }
}
