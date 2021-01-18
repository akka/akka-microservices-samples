package shopping.cart;

import akka.Done;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface ItemPopularityRepository {
  CompletionStage<Done> update(String itemId, int delta);

  CompletionStage<Optional<Long>> getItem(String itemId);
}
