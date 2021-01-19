package shopping.cart;

import static org.junit.Assert.assertEquals;

import akka.Done;
import akka.NotUsed;
import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.persistence.query.Offset;
import akka.projection.ProjectionId;
import akka.projection.eventsourced.EventEnvelope;
import akka.projection.javadsl.Handler;
import akka.projection.testkit.javadsl.ProjectionTestKit;
import akka.projection.testkit.javadsl.TestProjection;
import akka.projection.testkit.javadsl.TestSourceProvider;
import akka.stream.javadsl.Source;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.ClassRule;
import org.junit.Test;
import shopping.cart.repository.ItemPopularityRepository;

public class ItemPopularityProjectionTest {
  // stub out the db layer and simulate recording item count updates
  static class TestItemPopularityRepository implements ItemPopularityRepository {
    private final Map<String, ItemPopularity> itemPops = new HashMap<>();

    @Override
    public ItemPopularity save(ItemPopularity itemPopularity) {
      itemPops.put(itemPopularity.getItemId(), itemPopularity);
      return itemPopularity;
    }

    @Override
    public Optional<ItemPopularity> findById(String id) {
      return Optional.ofNullable(itemPops.get(id));
    }
  }

  @ClassRule public static final TestKitJunitResource testKit = new TestKitJunitResource();

  static final ProjectionTestKit projectionTestKit = ProjectionTestKit.create(testKit.system());

  private EventEnvelope<ShoppingCart.Event> createEnvelope(ShoppingCart.Event event, long seqNo) {
    return new EventEnvelope<>(Offset.sequence(seqNo), "persistenceId", seqNo, event, 0L);
  }

  private Handler<EventEnvelope<ShoppingCart.Event>> toAsyncHandler(
      ItemPopularityProjectionHandler itemHandler) {
    return new Handler<EventEnvelope<ShoppingCart.Event>>() {
      @Override
      public CompletionStage<Done> process(EventEnvelope<ShoppingCart.Event> eventEventEnvelope)
          throws Exception {
        return CompletableFuture.supplyAsync(
            () -> {
              itemHandler.process(
                  // session = null is safe.
                  // The real handler never uses the session. The connection is provided to the repo
                  // by Spring itself
                  null, eventEventEnvelope);
              return Done.getInstance();
            });
      }
    };
  }

  @Test
  public void itemPopularityUpdateUpdate() {
    Source<EventEnvelope<ShoppingCart.Event>, NotUsed> events =
        Source.from(
            Arrays.asList(
                createEnvelope(new ShoppingCart.ItemAdded("a7079", "bowling shoes", 1), 0L),
                createEnvelope(
                    new ShoppingCart.ItemQuantityAdjusted("a7079", "bowling shoes", 1, 2), 1L),
                createEnvelope(
                    new ShoppingCart.CheckedOut("a7079", Instant.parse("2020-01-01T12:00:00.00Z")),
                    2L),
                createEnvelope(new ShoppingCart.ItemAdded("0d12d", "akka t-shirt", 1), 3L),
                createEnvelope(new ShoppingCart.ItemAdded("0d12d", "skis", 1), 4L),
                createEnvelope(new ShoppingCart.ItemRemoved("0d12d", "skis", 1), 5L),
                createEnvelope(
                    new ShoppingCart.CheckedOut("0d12d", Instant.parse("2020-01-01T12:05:00.00Z")),
                    6L)));

    TestItemPopularityRepository repository = new TestItemPopularityRepository();
    ProjectionId projectionId = ProjectionId.of("item-popularity", "carts-0");

    TestSourceProvider<Offset, EventEnvelope<ShoppingCart.Event>> sourceProvider =
        TestSourceProvider.create(events, EventEnvelope::offset);

    TestProjection<Offset, EventEnvelope<ShoppingCart.Event>> projection =
        TestProjection.create(
            projectionId,
            sourceProvider,
            () -> toAsyncHandler(new ItemPopularityProjectionHandler("carts-0", repository)));

    projectionTestKit.run(
        projection,
        () -> {
          assertEquals(3, repository.itemPops.size());
          assertEquals(2L, repository.itemPops.get("bowling shoes").getCount());
          assertEquals(1L, repository.itemPops.get("akka t-shirt").getCount());
          assertEquals(0L, repository.itemPops.get("skis").getCount());
        });
  }
}
