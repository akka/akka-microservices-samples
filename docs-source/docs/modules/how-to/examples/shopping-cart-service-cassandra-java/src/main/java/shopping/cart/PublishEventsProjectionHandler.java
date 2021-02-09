// tag::handler[]
package shopping.cart;

import static akka.Done.done;

import akka.Done;
import akka.kafka.javadsl.SendProducer;
import akka.projection.eventsourced.EventEnvelope;
import akka.projection.javadsl.Handler;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.util.concurrent.CompletionStage;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PublishEventsProjectionHandler
    extends Handler<EventEnvelope<ShoppingCart.Event>> {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final String topic;
  private final SendProducer<String, byte[]> sendProducer; // <1>

  public PublishEventsProjectionHandler(String topic, SendProducer<String, byte[]> sendProducer) {
    this.topic = topic;
    this.sendProducer = sendProducer;
  }

  @Override
  public CompletionStage<Done> process(EventEnvelope<ShoppingCart.Event> envelope)
      throws Exception {
    ShoppingCart.Event event = envelope.event();

    // using the cartId as the key and `DefaultPartitioner` will select partition based on the key
    // so that events for same cart always ends up in same partition
    String key = event.cartId;
    ProducerRecord<String, byte[]> producerRecord =
        new ProducerRecord<>(topic, key, serialize(event)); // <2>
    return sendProducer
        .send(producerRecord)
        .thenApply(
            recordMetadata -> {
              logger.info(
                  "Published event [{}] to topic/partition {}/{}",
                  event,
                  topic,
                  recordMetadata.partition());
              return done();
            });
  }

  private static byte[] serialize(ShoppingCart.Event event) {
    final ByteString protoMessage;
    final String fullName;
    if (event instanceof ShoppingCart.ItemAdded) {
      ShoppingCart.ItemAdded itemAdded = (ShoppingCart.ItemAdded) event;
      protoMessage =
          shopping.cart.proto.ItemAdded.newBuilder()
              .setCartId(itemAdded.cartId)
              .setItemId(itemAdded.itemId)
              .setQuantity(itemAdded.quantity)
              .build()
              .toByteString();
      fullName = shopping.cart.proto.ItemAdded.getDescriptor().getFullName();
      // end::handler[]
    } else if (event instanceof ShoppingCart.ItemQuantityAdjusted) {
      ShoppingCart.ItemQuantityAdjusted itemQuantityAdjusted =
          (ShoppingCart.ItemQuantityAdjusted) event;
      protoMessage =
          shopping.cart.proto.ItemQuantityAdjusted.newBuilder()
              .setCartId(itemQuantityAdjusted.cartId)
              .setItemId(itemQuantityAdjusted.itemId)
              .setQuantity(itemQuantityAdjusted.newQuantity)
              .build()
              .toByteString();
      fullName = shopping.cart.proto.ItemQuantityAdjusted.getDescriptor().getFullName();
    } else if (event instanceof ShoppingCart.ItemRemoved) {
      ShoppingCart.ItemRemoved itemRemoved = (ShoppingCart.ItemRemoved) event;
      protoMessage =
          shopping.cart.proto.ItemRemoved.newBuilder()
              .setCartId(itemRemoved.cartId)
              .setItemId(itemRemoved.itemId)
              .build()
              .toByteString();
      fullName = shopping.cart.proto.ItemRemoved.getDescriptor().getFullName();
      // tag::handler[]
    } else if (event instanceof ShoppingCart.CheckedOut) {
      ShoppingCart.CheckedOut checkedOut = (ShoppingCart.CheckedOut) event;
      protoMessage =
          shopping.cart.proto.CheckedOut.newBuilder()
              .setCartId(checkedOut.cartId)
              .build()
              .toByteString();
      fullName = shopping.cart.proto.CheckedOut.getDescriptor().getFullName();
    } else {
      throw new IllegalArgumentException("Unknown event type: " + event.getClass());
    }
    // pack in Any so that type information is included for deserialization
    return Any.newBuilder()
        .setValue(protoMessage)
        .setTypeUrl("shopping-cart-service/" + fullName)
        .build()
        .toByteArray(); // <3>
  }
}
// end::handler[]
