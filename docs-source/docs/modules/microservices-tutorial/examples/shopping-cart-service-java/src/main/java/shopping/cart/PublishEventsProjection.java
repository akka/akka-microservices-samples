package shopping.cart;

import akka.actor.CoordinatedShutdown;
import akka.actor.typed.ActorSystem;
import akka.cluster.sharding.typed.ShardedDaemonProcessSettings;
import akka.cluster.sharding.typed.javadsl.ShardedDaemonProcess;
import akka.kafka.ProducerSettings;
import akka.kafka.javadsl.SendProducer;
import akka.persistence.jdbc.query.javadsl.JdbcReadJournal;
import akka.persistence.query.Offset;
import akka.projection.ProjectionBehavior;
import akka.projection.ProjectionId;
import akka.projection.eventsourced.EventEnvelope;
import akka.projection.eventsourced.javadsl.EventSourcedProvider;
import akka.projection.javadsl.AtLeastOnceProjection;
import akka.projection.javadsl.SourceProvider;
import akka.projection.jdbc.javadsl.JdbcProjection;
import java.util.Optional;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.orm.jpa.JpaTransactionManager;
import shopping.cart.repository.HibernateJdbcSession;

public final class PublishEventsProjection {

  private PublishEventsProjection() {}

  public static void init(ActorSystem<?> system, JpaTransactionManager transactionManager) {
    SendProducer<String, byte[]> sendProducer = createProducer(system);
    String topic = system.settings().config().getString("shopping-cart-service.kafka.topic");

    ShardedDaemonProcess.get(system)
        .init(
            ProjectionBehavior.Command.class,
            "PublishEventsProjection",
            ShoppingCart.TAGS.size(),
            index ->
                ProjectionBehavior.create(
                    createProjectionFor(system, transactionManager, topic, sendProducer, index)),
            ShardedDaemonProcessSettings.create(system),
            Optional.of(ProjectionBehavior.stopMessage()));
  }

  private static SendProducer<String, byte[]> createProducer(ActorSystem<?> system) {
    ProducerSettings<String, byte[]> producerSettings =
        ProducerSettings.create(system, new StringSerializer(), new ByteArraySerializer());
    SendProducer<String, byte[]> sendProducer = new SendProducer<>(producerSettings, system);
    CoordinatedShutdown.get(system)
        .addTask(
            CoordinatedShutdown.PhaseActorSystemTerminate(),
            "close-sendProducer",
            () -> sendProducer.close());
    return sendProducer;
  }

  private static AtLeastOnceProjection<Offset, EventEnvelope<ShoppingCart.Event>>
      createProjectionFor(
          ActorSystem<?> system,
          JpaTransactionManager transactionManager,
          String topic,
          SendProducer<String, byte[]> sendProducer,
          int index) {
    String tag = ShoppingCart.TAGS.get(index);
    SourceProvider<Offset, EventEnvelope<ShoppingCart.Event>> sourceProvider =
        EventSourcedProvider.eventsByTag(system, JdbcReadJournal.Identifier(), tag);

    return JdbcProjection.atLeastOnceAsync(
        ProjectionId.of("PublishEventsProjection", tag),
        sourceProvider,
        () -> new HibernateJdbcSession(transactionManager),
        () -> new PublishEventsProjectionHandler(topic, sendProducer),
        system);
  }
}
