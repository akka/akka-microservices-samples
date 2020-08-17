package sample.shoppingcart

import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.ClusterShardingSettings
import akka.cluster.sharding.typed.ShardedDaemonProcessSettings
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.SendProducer
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.Offset
import akka.projection.ProjectionBehavior
import akka.projection.ProjectionId
import akka.projection.cassandra.scaladsl.CassandraProjection
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.scaladsl.AtLeastOnceProjection
import akka.projection.scaladsl.SourceProvider
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringSerializer

object PublishEventsProjection {

  private def createProjectionFor(
      system: ActorSystem[_],
      topic: String,
      sendProducer: SendProducer[String, Array[Byte]],
      index: Int): AtLeastOnceProjection[Offset, EventEnvelope[ShoppingCart.Event]] = {
    val tag = s"${ShoppingCart.TagPrefix}-$index"
    val sourceProvider: SourceProvider[Offset, EventEnvelope[ShoppingCart.Event]] =
      EventSourcedProvider.eventsByTag[ShoppingCart.Event](
        system = system,
        readJournalPluginId = CassandraReadJournal.Identifier,
        tag = tag)

    CassandraProjection.atLeastOnce(
      projectionId = ProjectionId("cart-events", tag),
      sourceProvider,
      handler = () => new PublishEventsProjectionHandler(system, topic, sendProducer))
  }

  def init(system: ActorSystem[_], projectionParallelism: Int): Unit = {
    val topic = system.settings.config.getString("shopping-cart.kafka-topic")
    val bootstrapServers = system.settings.config.getString("shopping-cart.kafka-bootstrap-servers")
    val producerSettings =
      ProducerSettings(system, new StringSerializer, new ByteArraySerializer).withBootstrapServers(bootstrapServers)
    import akka.actor.typed.scaladsl.adapter._ // FIXME might not be needed in later Alpakka Kafka version?
    val sendProducer = SendProducer(producerSettings)(system.toClassic)

    CoordinatedShutdown(system).addTask(CoordinatedShutdown.PhaseBeforeActorSystemTerminate, "close sendProducer") {
      () =>
        sendProducer.close()
    }

    // we only want to run the daemon processes on the read-model nodes
    val shardingSettings = ClusterShardingSettings(system)
    val shardedDaemonProcessSettings =
      ShardedDaemonProcessSettings(system).withShardingSettings(shardingSettings.withRole("read-model"))

    ShardedDaemonProcess(system).init(
      name = "PublishEventsProjection",
      projectionParallelism,
      index => ProjectionBehavior(createProjectionFor(system, topic, sendProducer, index)),
      shardedDaemonProcessSettings,
      Some(ProjectionBehavior.Stop))
  }

}
