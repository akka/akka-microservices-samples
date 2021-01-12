package shopping.cart.repository;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import shopping.cart.ItemPopularity;

public class AsyncItemPopularityRepository {
  private final Executor blockingExecutor;
  private final ItemPopularityRepository repository;

  public AsyncItemPopularityRepository(
      Executor blockingExecutor, ItemPopularityRepository repository) {
    this.blockingExecutor = blockingExecutor;
    this.repository = repository;
  }

  public CompletionStage<Optional<ItemPopularity>> findById(String itemId) {
    return CompletableFuture.supplyAsync(() -> repository.findById(itemId), blockingExecutor);
  }
}
