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
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public final class ShoppingCart
    extends EventSourcedBehaviorWithEnforcedReplies<
        ShoppingCart.Command, ShoppingCart.Event, ShoppingCart.State> {

  static final class State {}

  interface Command {}

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

  public static final class Checkout implements Command {
    final ActorRef<StatusReply<Summary>> replyTo;

    public Checkout(ActorRef<StatusReply<Summary>> replyTo) {
      this.replyTo = replyTo;
    }
  }

  public static final class Summary {
    final Map<String, Integer> items;
    final boolean checkedOut;

    public Summary(Map<String, Integer> items, boolean checkedOut) {
      // defensive copy since items is a mutable object
      this.items = new HashMap<>(items);
      this.checkedOut = checkedOut;
    }
  }

  abstract static class Event {
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
  }

  static final class CheckedOut extends Event {
    final Instant eventTime;

    public CheckedOut(String cartId, Instant eventTime) {
      super(cartId);
      this.eventTime = eventTime;
    }
  }

  static final EntityTypeKey<Command> ENTITY_KEY =
      EntityTypeKey.create(Command.class, "ShoppingCart");

  static final List<String> TAGS =
      Collections.unmodifiableList(
          Arrays.asList("carts-0", "carts-1", "carts-2", "carts-3", "carts-4"));

  // tag::write-side-with-role[]
  public static void init(ActorSystem<?> system) {
    ClusterSharding.get(system)
        .init(
            Entity.of(
                ENTITY_KEY,
                entityContext -> {
                  int i = Math.abs(entityContext.getEntityId().hashCode() % TAGS.size());
                  String selectedTag = TAGS.get(i);
                  return ShoppingCart.create(entityContext.getEntityId(), selectedTag);
                }));
  }
  // end::write-side-with-role[]

  public static Behavior<Command> create(String cartId, String projectionTag) {
    return Behaviors.setup(
        ctx -> EventSourcedBehavior.start(new ShoppingCart(cartId, projectionTag), ctx));
  }

  private final String projectionTag;

  private final String cartId;

  private ShoppingCart(String cartId, String projectionTag) {
    super(
        PersistenceId.of(ENTITY_KEY.name(), cartId),
        SupervisorStrategy.restartWithBackoff(Duration.ofMillis(200), Duration.ofSeconds(5), 0.1));
    this.cartId = cartId;
    this.projectionTag = projectionTag;
  }

  @Override
  public Set<String> tagsFor(Event event) {
    return Collections.singleton(projectionTag);
  }

  @Override
  public State emptyState() {
    return new State();
  }

  @Override
  public CommandHandlerWithReply<Command, Event, State> commandHandler() {
    return null; // not implemented here
  }

  @Override
  public EventHandler<State, Event> eventHandler() {
    return null; // not implemented here
  }
}
