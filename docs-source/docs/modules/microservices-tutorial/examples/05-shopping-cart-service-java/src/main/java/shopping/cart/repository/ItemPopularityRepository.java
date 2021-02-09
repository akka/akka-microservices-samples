package shopping.cart.repository;

import java.util.Optional;
import org.springframework.data.repository.Repository;
import shopping.cart.ItemPopularity;

public interface ItemPopularityRepository extends Repository<ItemPopularity, String> {

  ItemPopularity save(ItemPopularity itemPopularity);

  Optional<ItemPopularity> findById(String id);
}
