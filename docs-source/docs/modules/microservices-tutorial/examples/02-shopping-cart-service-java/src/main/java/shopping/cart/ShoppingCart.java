// tag::imports[]
package shopping.cart;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.Entity;
import akka.cluster.sharding.typed.javadsl.EntityTypeKey;
import akka.pattern.StatusReply;
import akka.persistence.typed.PersistenceId;
import akka.persistence.typed.javadsl.*;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

// end::imports[]

/**
 * This is an event sourced actor (`EventSourcedBehavior`). An entity managed by Cluster Sharding.
 *
 * <p>It has a state, [[ShoppingCart.State]], which holds the current shopping cart items and
 * whether it's checked out.
 *
 * <p>You interact with event sourced actors by sending commands to them, see classes implementing
 * [[ShoppingCart.Command]].
 *
 * <p>The command handler validates and translates commands to events, see classes implementing
 * [[ShoppingCart.Event]]. It's the events that are persisted by the `EventSourcedBehavior`. The
 * event handler updates the current state based on the event. This is done when the event is first
 * created, and when the entity is loaded from the database - each event will be replayed to
 * recreate the state of the entity.
 */
// tag::shoppingCart[]
public final class ShoppingCart
    extends EventSourcedBehaviorWithEnforcedReplies<
        ShoppingCart.Command, ShoppingCart.Event, ShoppingCart.State> {
  // end::shoppingCart[]

  /** The current state held by the `EventSourcedBehavior`. */
  // tag::state[]
  // ...
  static final class State implements CborSerializable {
    final Map<String, Integer> items;

    public State() {
      this(new HashMap<>());
    }

    public State(Map<String, Integer> items) {
      this.items = items;
    }

    public boolean hasItem(String itemId) {
      return items.containsKey(itemId);
    }

    public State updateItem(String itemId, int quantity) {
      if (quantity == 0) {
        items.remove(itemId);
      } else {
        items.put(itemId, quantity);
      }
      return this;
    }

    public Summary toSummary() {
      return new Summary(items);
    }

    public int itemCount(String itemId) {
      return items.get(itemId);
    }

    public boolean isEmpty() {
      return items.isEmpty();
    }
  }
  // end::state[]

  // tag::commands[]
  // ...
  /** This interface defines all the commands (messages) that the ShoppingCart actor supports. */
  interface Command extends CborSerializable {}

  /**
   * A command to add an item to the cart.
   *
   * <p>It replies with `StatusReply&lt;Summary&gt;`, which is sent back to the caller when all the
   * events emitted by this command are successfully persisted.
   */
  public static final class AddItem implements Command {
    final String itemId;
    final int quantity;
    final ActorRef<StatusReply<Summary>> replyTo;

    public AddItem(String itemId, int quantity, ActorRef<StatusReply<Summary>> replyTo) {
      this.itemId = itemId;
      this.quantity = quantity;
      this.replyTo = replyTo;
    }
  }

  /** Summary of the shopping cart state, used in reply messages. */
  public static final class Summary implements CborSerializable {
    final Map<String, Integer> items;

    @JsonCreator
    public Summary(Map<String, Integer> items) {
      // defensive copy since items is a mutable object
      this.items = new HashMap<>(items);
    }
  }
  // end::commands[]

  // tag::events[]
  // ...
  abstract static class Event implements CborSerializable {
    public final String cartId;

    public Event(String cartId) {
      this.cartId = cartId;
    }
  }

  static final class ItemAdded extends Event {
    public final String itemId;
    public final int quantity;

    public ItemAdded(String cartId, String itemId, int quantity) {
      super(cartId);
      this.itemId = itemId;
      this.quantity = quantity;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ItemAdded other = (ItemAdded) o;

      if (quantity != other.quantity) return false;
      if (!cartId.equals(other.cartId)) return false;
      return itemId.equals(other.itemId);
    }

    @Override
    public int hashCode() {
      int result = cartId.hashCode();
      result = 31 * result + itemId.hashCode();
      result = 31 * result + quantity;
      return result;
    }
  }
  // end::events[]

  // tag::init[]
  static final EntityTypeKey<Command> ENTITY_KEY =
      EntityTypeKey.create(Command.class, "ShoppingCart");

  private final String cartId;

  public static void init(ActorSystem<?> system) {
    ClusterSharding.get(system)
        .init(
            Entity.of(
                ENTITY_KEY,
                entityContext -> { // <1>
                  return ShoppingCart.create(entityContext.getEntityId());
                }));
  }

  public static Behavior<Command> create(String cartId) {
    return Behaviors.setup(
        ctx ->
            EventSourcedBehavior // <2>
                .start(new ShoppingCart(cartId), ctx));
  }

  @Override
  public RetentionCriteria retentionCriteria() { // <3>
    return RetentionCriteria.snapshotEvery(100, 3);
  }

  private ShoppingCart(String cartId) {
    super(
        PersistenceId.of(ENTITY_KEY.name(), cartId),
        SupervisorStrategy // <4>
            .restartWithBackoff(Duration.ofMillis(200), Duration.ofSeconds(5), 0.1));
    this.cartId = cartId;
  }

  @Override
  public State emptyState() {
    return new State();
  }
  // end::init[]

  // tag::commandHandler[]
  // ...
  @Override
  public CommandHandlerWithReply<Command, Event, State> commandHandler() {
    CommandHandlerWithReplyBuilder<Command, Event, State> builder =
        newCommandHandlerWithReplyBuilder();
    builder.forAnyState().onCommand(AddItem.class, this::onAddItem); // <1>
    return builder.build();
  }

  private ReplyEffect<Event, State> onAddItem(State state, AddItem cmd) {
    if (state.hasItem(cmd.itemId)) {
      return Effect()
          .reply(
              cmd.replyTo,
              StatusReply.error(
                  "Item '" + cmd.itemId + "' was already added to this shopping cart"));
    } else if (cmd.quantity <= 0) {
      return Effect().reply(cmd.replyTo, StatusReply.error("Quantity must be greater than zero"));
    } else {
      return Effect() // <2>
          .persist(new ItemAdded(cartId, cmd.itemId, cmd.quantity))
          .thenReply(cmd.replyTo, updatedCart -> StatusReply.success(updatedCart.toSummary()));
    }
  }
  // end::commandHandler[]

  // tag::eventHandler[]
  // ...
  @Override
  public EventHandler<State, Event> eventHandler() {
    return newEventHandlerBuilder()
        .forAnyState()
        .onEvent(ItemAdded.class, (state, evt) -> state.updateItem(evt.itemId, evt.quantity))
        .build();
  }
  // end::eventHandler[]

  // tag::shoppingCart[]
}

// end::shoppingCart[]
