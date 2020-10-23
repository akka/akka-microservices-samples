// tag::projection[]
package shopping.cart;

import akka.actor.typed.ActorSystem;
import akka.cluster.sharding.typed.ShardedDaemonProcessSettings;
import akka.cluster.sharding.typed.javadsl.ShardedDaemonProcess;
import akka.persistence.cassandra.query.javadsl.CassandraReadJournal;
import akka.persistence.query.Offset;
import akka.projection.ProjectionBehavior;
import akka.projection.ProjectionId;
import akka.projection.cassandra.javadsl.CassandraProjection;
import akka.projection.eventsourced.EventEnvelope;
import akka.projection.eventsourced.javadsl.EventSourcedProvider;
import akka.projection.javadsl.AtLeastOnceProjection;
import akka.projection.javadsl.SourceProvider;
import java.util.Optional;

public final class ItemPopularityProjection {

  private ItemPopularityProjection() {}

  // tag::howto-read-side-without-role[]
  public static void init(ActorSystem<?> system, ItemPopularityRepository repository) {
    ShardedDaemonProcess.get(system)
        .init( // <1>
            ProjectionBehavior.Command.class,
            "ItemPopularityProjection",
            ShoppingCart.TAGS.size(),
            index -> ProjectionBehavior.create(createProjectionFor(system, repository, index)),
            ShardedDaemonProcessSettings.create(system),
            Optional.of(ProjectionBehavior.stopMessage()));
  }
  // end::howto-read-side-without-role[]

  private static AtLeastOnceProjection<Offset, EventEnvelope<ShoppingCart.Event>>
      createProjectionFor(ActorSystem<?> system, ItemPopularityRepository repository, int index) {
    String tag = ShoppingCart.TAGS.get(index); // <2>

    SourceProvider<Offset, EventEnvelope<ShoppingCart.Event>> sourceProvider = // <3>
        EventSourcedProvider.eventsByTag(
            system,
            CassandraReadJournal.Identifier(), // <4>
            tag);

    return CassandraProjection.atLeastOnce( // <5>
        ProjectionId.of("ItemPopularityProjection", tag),
        sourceProvider,
        () -> new ItemPopularityProjectionHandler(tag, repository));
  }
}
// end::projection[]
