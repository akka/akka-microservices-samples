package shopping.cart;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.pattern.StatusReply;
import akka.persistence.testkit.javadsl.EventSourcedBehaviorTestKit;
import com.typesafe.config.ConfigFactory;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.*;

public class ShoppingCartTest {

    private final static String CART_ID = "testCart";

    @ClassRule
    public static final TestKitJunitResource testKit = new TestKitJunitResource(ConfigFactory.parseString(
            "akka.actor.serialization-bindings {\n" +
               "  \"shopping.cart.CborSerializable\" = jackson-cbor\n" +
               "}").withFallback(EventSourcedBehaviorTestKit.config())
    );

    private EventSourcedBehaviorTestKit<
            ShoppingCart.Command, ShoppingCart.Event, ShoppingCart.State>
            eventSourcedTestKit =
            EventSourcedBehaviorTestKit.create(
                    testKit.system(), ShoppingCart.create(CART_ID, "tag-1"));


    @Before
    public void beforeEach() {
        eventSourcedTestKit.clear();
    }

    @Test
    public void addAnItemToCart() {
        EventSourcedBehaviorTestKit.CommandResultWithReply<ShoppingCart.Command, ShoppingCart.Event, ShoppingCart.State, StatusReply<ShoppingCart.Summary>> result =
                eventSourcedTestKit.runCommand(replyTo -> new ShoppingCart.AddItem("foo", 42, replyTo));
        assertTrue(result.reply().isSuccess());
        ShoppingCart.Summary summary = result.reply().getValue();
        assertFalse(summary.checkedOut);
        assertEquals(1, summary.items.size());
        assertEquals(42, summary.items.get("foo").longValue());
        assertEquals(new ShoppingCart.ItemAdded(CART_ID, "foo", 42), result.event());
    }

    @Test
    public void rejectAlreadyAddedItem() {
        EventSourcedBehaviorTestKit.CommandResultWithReply<ShoppingCart.Command, ShoppingCart.Event, ShoppingCart.State, StatusReply<ShoppingCart.Summary>> result1 =
                eventSourcedTestKit.runCommand(replyTo -> new ShoppingCart.AddItem("foo", 42, replyTo));
        assertTrue(result1.reply().isSuccess());
        EventSourcedBehaviorTestKit.CommandResultWithReply<ShoppingCart.Command, ShoppingCart.Event, ShoppingCart.State, StatusReply<ShoppingCart.Summary>> result2 =
                eventSourcedTestKit.runCommand(replyTo -> new ShoppingCart.AddItem("foo", 42, replyTo));
        assertTrue(result2.reply().isError());
        assertTrue(result2.hasNoEvents());
    }

    @Test
    public void removeItem() {
        EventSourcedBehaviorTestKit.CommandResultWithReply<ShoppingCart.Command, ShoppingCart.Event, ShoppingCart.State, StatusReply<ShoppingCart.Summary>> result1 =
                eventSourcedTestKit.runCommand(replyTo -> new ShoppingCart.AddItem("foo", 42, replyTo));
        assertTrue(result1.reply().isSuccess());
        EventSourcedBehaviorTestKit.CommandResultWithReply<ShoppingCart.Command, ShoppingCart.Event, ShoppingCart.State, StatusReply<ShoppingCart.Summary>> result2 =
                eventSourcedTestKit.runCommand(replyTo -> new ShoppingCart.RemoveItem("foo", replyTo));
        assertTrue(result2.reply().isSuccess());
        assertEquals(new ShoppingCart.ItemRemoved(CART_ID, "foo", 42), result2.event());
    }

    @Test
    public void adjustQuantity() {
        EventSourcedBehaviorTestKit.CommandResultWithReply<ShoppingCart.Command, ShoppingCart.Event, ShoppingCart.State, StatusReply<ShoppingCart.Summary>> result1 =
                eventSourcedTestKit.runCommand(replyTo -> new ShoppingCart.AddItem("foo", 42, replyTo));
        assertTrue(result1.reply().isSuccess());
        EventSourcedBehaviorTestKit.CommandResultWithReply<ShoppingCart.Command, ShoppingCart.Event, ShoppingCart.State, StatusReply<ShoppingCart.Summary>> result2 =
                eventSourcedTestKit.runCommand(replyTo -> new ShoppingCart.AdjustItemQuantity("foo", 43, replyTo));
        assertTrue(result2.reply().isSuccess());
        assertEquals(43, result2.reply().getValue().items.get("foo").longValue());
        assertEquals(new ShoppingCart.ItemQuantityAdjusted(CART_ID, "foo", 42, 43), result2.event());
    }

    @Test
    public void checkout() {
        EventSourcedBehaviorTestKit.CommandResultWithReply<ShoppingCart.Command, ShoppingCart.Event, ShoppingCart.State, StatusReply<ShoppingCart.Summary>> result1 =
                eventSourcedTestKit.runCommand(replyTo -> new ShoppingCart.AddItem("foo", 42, replyTo));
        assertTrue(result1.reply().isSuccess());
        EventSourcedBehaviorTestKit.CommandResultWithReply<ShoppingCart.Command, ShoppingCart.Event, ShoppingCart.State, StatusReply<ShoppingCart.Summary>> result2 =
                eventSourcedTestKit.runCommand(replyTo -> new ShoppingCart.Checkout(replyTo));
        assertTrue(result2.reply().isSuccess());
        assertTrue(result2.event() instanceof ShoppingCart.CheckedOut);
        assertEquals(CART_ID, result2.event().cartId);

        EventSourcedBehaviorTestKit.CommandResultWithReply<ShoppingCart.Command, ShoppingCart.Event, ShoppingCart.State, StatusReply<ShoppingCart.Summary>> result3 =
                eventSourcedTestKit.runCommand(replyTo -> new ShoppingCart.AddItem("foo", 42, replyTo));
        assertTrue(result3.reply().isError());
    }

}