// tag::projection[]
package shopping.cart;

import akka.actor.typed.ActorSystem;
import akka.cluster.sharding.typed.ShardedDaemonProcessSettings;
import akka.cluster.sharding.typed.javadsl.ShardedDaemonProcess;
import akka.persistence.jdbc.query.javadsl.JdbcReadJournal;
import akka.persistence.query.Offset;
import akka.projection.ProjectionBehavior;
import akka.projection.ProjectionId;
import akka.projection.eventsourced.EventEnvelope;
import akka.projection.eventsourced.javadsl.EventSourcedProvider;
import akka.projection.javadsl.ExactlyOnceProjection;
import akka.projection.javadsl.SourceProvider;
import akka.projection.jdbc.javadsl.JdbcProjection;
import java.util.Optional;
import org.springframework.orm.jpa.JpaTransactionManager;
import shopping.cart.repository.HibernateJdbcSession;
import shopping.cart.repository.ItemPopularityRepository;

public final class ItemPopularityProjection {

  private ItemPopularityProjection() {}

  // tag::howto-read-side-without-role[]
  public static void init(
      ActorSystem<?> system,
      JpaTransactionManager transactionManager,
      ItemPopularityRepository repository) {

    ShardedDaemonProcess.get(system)
        .init( // <1>
            ProjectionBehavior.Command.class,
            "ItemPopularityProjection",
            ShoppingCart.TAGS.size(),
            index ->
                ProjectionBehavior.create(
                    createProjectionFor(system, transactionManager, repository, index)),
            ShardedDaemonProcessSettings.create(system),
            Optional.of(ProjectionBehavior.stopMessage()));
  }
  // end::howto-read-side-without-role[]

  private static ExactlyOnceProjection<Offset, EventEnvelope<ShoppingCart.Event>>
      createProjectionFor(
          ActorSystem<?> system,
          JpaTransactionManager transactionManager,
          ItemPopularityRepository repository,
          int index) {

    String tag = ShoppingCart.TAGS.get(index); // <2>

    SourceProvider<Offset, EventEnvelope<ShoppingCart.Event>> sourceProvider = // <3>
        EventSourcedProvider.eventsByTag(
            system,
            JdbcReadJournal.Identifier(), // <4>
            tag);

    return JdbcProjection.exactlyOnce( // <5>
        ProjectionId.of("ItemPopularityProjection", tag),
        sourceProvider,
        () -> new HibernateJdbcSession(transactionManager), // <6>
        () -> new ItemPopularityProjectionHandler(tag, repository), // <7>
        system);
  }
}
// end::projection[]
