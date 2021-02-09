package shopping.cart;

import static akka.Done.done;
import static org.junit.Assert.assertEquals;

import akka.Done;
import akka.NotUsed;
import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.persistence.query.Offset;
import akka.projection.ProjectionId;
import akka.projection.eventsourced.EventEnvelope;
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

public class ItemPopularityProjectionTest {
  // stub out the db layer and simulate recording item count updates
  static class TestItemPopularityRepository implements ItemPopularityRepository {
    private final Map<String, Long> counts = new HashMap<>();

    @Override
    public CompletionStage<Done> update(String itemId, int delta) {
      long current = counts.getOrDefault(itemId, 0L);
      counts.put(itemId, current + delta);
      return CompletableFuture.completedFuture(done());
    }

    @Override
    public CompletionStage<Optional<Long>> getItem(String itemId) {
      return CompletableFuture.completedFuture(Optional.ofNullable(counts.get(itemId)));
    }
  }

  @ClassRule public static final TestKitJunitResource testKit = new TestKitJunitResource();

  static final ProjectionTestKit projectionTestKit = ProjectionTestKit.create(testKit.system());

  private EventEnvelope<ShoppingCart.Event> createEnvelope(ShoppingCart.Event event, long seqNo) {
    return new EventEnvelope(Offset.sequence(seqNo), "persistenceId", seqNo, event, 0L);
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
            () -> new ItemPopularityProjectionHandler("carts-0", repository));

    projectionTestKit.run(
        projection,
        () -> {
          assertEquals(3, repository.counts.size());
          assertEquals(2L, repository.counts.get("bowling shoes").longValue());
          assertEquals(1L, repository.counts.get("akka t-shirt").longValue());
          assertEquals(0L, repository.counts.get("skis").longValue());
        });
  }
}
