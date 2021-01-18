package shopping.cart;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import akka.actor.testkit.typed.javadsl.ActorTestKit;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.MemberStatus;
import akka.cluster.typed.Cluster;
import akka.grpc.GrpcClientSettings;
import akka.kafka.ConsumerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.persistence.testkit.javadsl.PersistenceInit;
import akka.testkit.SocketUtil;
import com.google.protobuf.Any;
import com.google.protobuf.CodedInputStream;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.jdk.CollectionConverters;
import shopping.cart.proto.*;
import shopping.order.proto.OrderRequest;
import shopping.order.proto.OrderResponse;
import shopping.order.proto.ShoppingOrderService;

public class IntegrationTest {

  private static final Logger logger = LoggerFactory.getLogger(IntegrationTest.class);

  private static final long UNIQUE_QUALIFIER = System.currentTimeMillis();
  private static final String KEYSPACE = "ItemPopularityIntegrationTest_" + UNIQUE_QUALIFIER;

  private static Config sharedConfig() {
    return ConfigFactory.parseString(
            "akka.persistence.cassandra.journal.keyspace = "
                + KEYSPACE
                + "\n"
                + "akka.persistence.cassandra.snapshot.keyspace = "
                + KEYSPACE
                + "\n"
                + "akka.projection.cassandra.offset-store.keyspace = "
                + KEYSPACE
                + "\n"
                + "shopping-cart-service.kafka.topic = \"shopping_cart_events_"
                + UNIQUE_QUALIFIER
                + "\"")
        .withFallback(ConfigFactory.load("integration-test.conf"));
  }

  private static Config nodeConfig(
      int grcpPort, List<Integer> managementPorts, int managementPortIndex) {
    return ConfigFactory.parseString(
        "shopping-cart-service.grpc.port = "
            + grcpPort
            + "\n"
            + "akka.management.http.port = "
            + managementPorts.get(managementPortIndex)
            + "\n"
            + "akka.discovery.config.services.shopping-cart-service.endpoints = [\n"
            + "  { host = \"127.0.0.1\", port = "
            + managementPorts.get(0)
            + "},\n"
            + "  { host = \"127.0.0.1\", port = "
            + managementPorts.get(1)
            + "},\n"
            + "]");
  }

  private static class TestNodeFixture {

    private final ActorTestKit testKit;
    private final ActorSystem<?> system;
    private final GrpcClientSettings clientSettings;
    private shopping.cart.proto.ShoppingCartService client = null;

    public TestNodeFixture(int grcpPort, List<Integer> managementPorts, int managementPortIndex) {
      testKit =
          ActorTestKit.create(
              "IntegrationTest",
              nodeConfig(grcpPort, managementPorts, managementPortIndex)
                  .withFallback(sharedConfig()));
      system = testKit.system();
      clientSettings =
          GrpcClientSettings.connectToServiceAt("127.0.0.1", grcpPort, system).withTls(false);
    }

    public shopping.cart.proto.ShoppingCartService getClient() {
      if (client == null) {
        client = shopping.cart.proto.ShoppingCartServiceClient.create(clientSettings, system);
      }
      return client;
    }
  }

  private static void initializeKafkaTopicProbe(
      ActorSystem<?> system, TestProbe<Object> kafkaTopicProbe) {
    String topic = system.settings().config().getString("shopping-cart-service.kafka.topic");
    String groupId = UUID.randomUUID().toString();
    ConsumerSettings<String, byte[]> consumerSettings =
        ConsumerSettings.create(system, new StringDeserializer(), new ByteArrayDeserializer())
            .withBootstrapServers("localhost:9092") // provided by Docker compose
            .withGroupId(groupId);
    Consumer.plainSource(consumerSettings, Subscriptions.topics(topic))
        .map(
            record -> {
              byte[] bytes = record.value();
              Any x = Any.parseFrom(bytes);
              String typeUrl = x.getTypeUrl();
              CodedInputStream inputBytes = x.getValue().newCodedInput();
              final Object event;
              switch (typeUrl) {
                case "shopping-cart-service/shoppingcart.ItemAdded":
                  event = shopping.cart.proto.ItemAdded.parseFrom(inputBytes);
                  break;
                case "shopping-cart-service/shoppingcart.ItemQuantityAdjusted":
                  event = shopping.cart.proto.ItemQuantityAdjusted.parseFrom(inputBytes);
                  break;
                case "shopping-cart-service/shoppingcart.ItemRemoved":
                  event = shopping.cart.proto.ItemRemoved.parseFrom(inputBytes);
                  break;
                case "shopping-cart-service/shoppingcart.CheckedOut":
                  event = shopping.cart.proto.CheckedOut.parseFrom(inputBytes);
                  break;
                default:
                  throw new IllegalArgumentException("Unknown record type [" + typeUrl + "]");
              }
              return event;
            })
        .runForeach(event -> kafkaTopicProbe.getRef().tell(event), system)
        .whenComplete(
            (ok, ex) -> {
              if (ex != null) logger.error("Test consumer of " + topic + " failed", ex);
            });
  }

  private static TestNodeFixture testNode1;
  private static TestNodeFixture testNode2;
  private static TestNodeFixture testNode3;
  private static List<ActorSystem<?>> systems;
  private static TestProbe<Object> kafkaTopicProbe;
  private static TestProbe<OrderRequest> orderServiceProbe;
  private static ShoppingOrderService testOrderService;
  private static final Duration requestTimeout = Duration.ofSeconds(10);

  @BeforeClass
  public static void setup() throws Exception {
    List<InetSocketAddress> inetSocketAddresses =
        CollectionConverters.SeqHasAsJava(
                SocketUtil.temporaryServerAddresses(6, "127.0.0.1", false))
            .asJava();
    List<Integer> grpcPorts =
        inetSocketAddresses.subList(0, 3).stream()
            .map(InetSocketAddress::getPort)
            .collect(Collectors.toList());
    List<Integer> managementPorts =
        inetSocketAddresses.subList(3, 6).stream()
            .map(InetSocketAddress::getPort)
            .collect(Collectors.toList());
    testNode1 = new TestNodeFixture(grpcPorts.get(0), managementPorts, 0);
    testNode2 = new TestNodeFixture(grpcPorts.get(1), managementPorts, 1);
    testNode3 = new TestNodeFixture(grpcPorts.get(2), managementPorts, 2);
    systems = Arrays.asList(testNode1.system, testNode2.system, testNode3.system);

    kafkaTopicProbe = testNode1.testKit.createTestProbe();
    orderServiceProbe = testNode1.testKit.createTestProbe();
    // stub of the ShoppingOrderService
    testOrderService =
        new ShoppingOrderService() {
          @Override
          public CompletionStage<OrderResponse> order(OrderRequest in) {
            orderServiceProbe.getRef().tell(in);
            return CompletableFuture.completedFuture(
                OrderResponse.newBuilder().setOk(true).build());
          }
        };
    // avoid concurrent creation of keyspace and tables
    PersistenceInit.initializeDefaultPlugins(testNode1.system, Duration.ofSeconds(10))
        .toCompletableFuture()
        .get(10, SECONDS);
    CreateTableTestUtils.createTables(testNode1.system);

    testNode1.testKit.spawn(createMainBehavior(), "guardian");
    testNode2.testKit.spawn(createMainBehavior(), "guardian");
    testNode3.testKit.spawn(createMainBehavior(), "guardian");

    // wait for all nodes to have joined the cluster, become up and see all other nodes as up
    TestProbe<Object> upProbe = testNode1.testKit.createTestProbe();
    systems.forEach(
        system -> {
          upProbe.awaitAssert(
              Duration.ofSeconds(15),
              () -> {
                Cluster cluster = Cluster.get(system);
                assertEquals(MemberStatus.up(), cluster.selfMember().status());
                cluster
                    .state()
                    .getMembers()
                    .iterator()
                    .forEachRemaining(member -> assertEquals(MemberStatus.up(), member.status()));
                return null;
              });
        });

    initializeKafkaTopicProbe(testNode1.system, kafkaTopicProbe);
  }

  @AfterClass
  public static void tearDown() {
    testNode3.testKit.shutdownTestKit();
    testNode2.testKit.shutdownTestKit();
    testNode1.testKit.shutdownTestKit();
  }

  public static Behavior<Void> createMainBehavior() {
    return Behaviors.setup(
        context ->
            new Main(context) {
              @Override
              protected ShoppingOrderService orderServiceClient(ActorSystem<?> system) {
                return testOrderService;
              }
            });
  }

  @Test
  public void updateAndProjectFromDifferentNodesViaGrpc() throws Exception {
    // add from client1
    CompletionStage<Cart> response1 =
        testNode1
            .getClient()
            .addItem(
                AddItemRequest.newBuilder()
                    .setCartId("cart-1")
                    .setItemId("foo")
                    .setQuantity(42)
                    .build());
    Cart updatedCart1 = response1.toCompletableFuture().get(requestTimeout.getSeconds(), SECONDS);
    assertEquals("foo", updatedCart1.getItems(0).getItemId());
    assertEquals(42, updatedCart1.getItems(0).getQuantity());

    // first may take longer time
    ItemAdded published1 =
        kafkaTopicProbe.expectMessageClass(ItemAdded.class, Duration.ofSeconds(20));
    assertEquals("cart-1", published1.getCartId());
    assertEquals("foo", published1.getItemId());
    assertEquals(42, published1.getQuantity());

    // add from client2
    CompletionStage<Cart> response2 =
        testNode2
            .getClient()
            .addItem(
                AddItemRequest.newBuilder()
                    .setCartId("cart-2")
                    .setItemId("bar")
                    .setQuantity(17)
                    .build());
    Cart updatedCart2 = response2.toCompletableFuture().get(requestTimeout.getSeconds(), SECONDS);
    assertEquals("bar", updatedCart2.getItems(0).getItemId());
    assertEquals(17, updatedCart2.getItems(0).getQuantity());

    // update from client2
    CompletionStage<Cart> response3 =
        testNode2
            .getClient()
            .updateItem(
                UpdateItemRequest.newBuilder()
                    .setCartId("cart-2")
                    .setItemId("bar")
                    .setQuantity(18)
                    .build());
    Cart updatedCart3 = response3.toCompletableFuture().get(requestTimeout.getSeconds(), SECONDS);
    assertEquals("bar", updatedCart3.getItems(0).getItemId());
    assertEquals(18, updatedCart3.getItems(0).getQuantity());

    // ItemPopularityProjection has consumed the events and updated db
    TestProbe<Object> testProbe = testNode1.testKit.createTestProbe();
    testProbe.awaitAssert(
        () -> {
          // FIXME https://github.com/akka/akka/issues/29677 Supplier does not allow throwing
          // checked
          try {
            assertEquals(
                42,
                testNode1
                    .getClient()
                    .getItemPopularity(
                        GetItemPopularityRequest.newBuilder().setItemId("foo").build())
                    .toCompletableFuture()
                    .get(requestTimeout.getSeconds(), SECONDS)
                    .getPopularityCount());

            assertEquals(
                18,
                testNode1
                    .getClient()
                    .getItemPopularity(
                        GetItemPopularityRequest.newBuilder().setItemId("bar").build())
                    .toCompletableFuture()
                    .get(requestTimeout.getSeconds(), SECONDS)
                    .getPopularityCount());
            return null;
          } catch (Exception ex) {
            throw new RuntimeException(ex);
          }
        });

    ItemAdded published2 = kafkaTopicProbe.expectMessageClass(ItemAdded.class);
    assertEquals("cart-2", published2.getCartId());
    assertEquals("bar", published2.getItemId());
    assertEquals(17, published2.getQuantity());

    ItemQuantityAdjusted published3 =
        kafkaTopicProbe.expectMessageClass(ItemQuantityAdjusted.class);
    assertEquals("cart-2", published3.getCartId());
    assertEquals("bar", published3.getItemId());
    assertEquals(18, published3.getQuantity());

    CompletionStage<Cart> response4 =
        testNode2.getClient().checkout(CheckoutRequest.newBuilder().setCartId("cart-2").build());
    Cart cart4 = response4.toCompletableFuture().get(requestTimeout.getSeconds(), SECONDS);
    assertTrue(cart4.getCheckedOut());

    OrderRequest orderRequest = orderServiceProbe.expectMessageClass(OrderRequest.class);
    assertEquals("cart-2", orderRequest.getCartId());
    assertEquals("bar", orderRequest.getItems(0).getItemId());
    assertEquals(18, orderRequest.getItems(0).getQuantity());

    CheckedOut published4 = kafkaTopicProbe.expectMessageClass(CheckedOut.class);
    assertEquals("cart-2", published4.getCartId());
  }
}
