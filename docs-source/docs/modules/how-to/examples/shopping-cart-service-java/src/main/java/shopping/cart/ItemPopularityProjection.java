package shopping.cart;

import akka.actor.typed.ActorSystem;
import akka.cluster.sharding.typed.ShardedDaemonProcessSettings;
import akka.cluster.sharding.typed.javadsl.ShardedDaemonProcess;
import akka.persistence.query.Offset;
import akka.projection.ProjectionBehavior;
import akka.projection.eventsourced.EventEnvelope;
import akka.projection.javadsl.ExactlyOnceProjection;
import java.util.Optional;
import org.springframework.orm.jpa.JpaTransactionManager;
import shopping.cart.repository.ItemPopularityRepository;

public final class ItemPopularityProjection {

  private ItemPopularityProjection() {}

  // tag::read-side-with-role[]
  public static void init(
      ActorSystem<?> system,
      JpaTransactionManager transactionManager,
      ItemPopularityRepository repository) {

    ShardedDaemonProcess.get(system)
        .init(
            ProjectionBehavior.Command.class,
            "ItemPopularityProjection",
            ShoppingCart.TAGS.size(),
            index ->
                ProjectionBehavior.create(
                    createProjectionFor(system, transactionManager, repository, index)),
            ShardedDaemonProcessSettings.create(system).withRole("projection"), // <1>
            Optional.of(ProjectionBehavior.stopMessage()));
  }
  // end::read-side-with-role[]

  private static ExactlyOnceProjection<Offset, EventEnvelope<ShoppingCart.Event>>
      createProjectionFor(
          ActorSystem<?> system,
          JpaTransactionManager transactionManager,
          ItemPopularityRepository repository,
          int index) {

    return null; // not implemented here
  }
}
