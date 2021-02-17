package shopping.cart;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import akka.grpc.GrpcClientSettings;
import akka.management.cluster.bootstrap.ClusterBootstrap;
import akka.management.javadsl.AkkaManagement;
import akka.stream.alpakka.cassandra.javadsl.CassandraSession;
import akka.stream.alpakka.cassandra.javadsl.CassandraSessionRegistry;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shopping.cart.proto.ShoppingCartService;
import shopping.order.proto.ShoppingOrderService;
import shopping.order.proto.ShoppingOrderServiceClient;

public class Main {

  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {
    ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "ShoppingCartService");
    try {
      init(system, orderServiceClient(system));
    } catch (Exception e) {
      logger.error("Terminating due to initialization failure.", e);
      system.terminate();
    }
  }

  public static void init(ActorSystem<Void> system, ShoppingOrderService orderService) {
    AkkaManagement.get(system).start();
    ClusterBootstrap.get(system).start();

    ShoppingCart.init(system);

    // tag::ItemPopularityProjection[]
    CassandraSession session =
        CassandraSessionRegistry.get(system).sessionFor("akka.persistence.cassandra"); // <1>
    // use same keyspace for the item_popularity table as the offset store
    Config config = system.settings().config();
    String itemPopularityKeyspace =
        config.getString("akka.projection.cassandra.offset-store.keyspace");
    ItemPopularityRepository itemPopularityRepository =
        new ItemPopularityRepositoryImpl(session, itemPopularityKeyspace); // <2>

    ItemPopularityProjection.init(system, itemPopularityRepository); // <3>
    // end::ItemPopularityProjection[]

    PublishEventsProjection.init(system);

    SendOrderProjection.init(system, orderService);

    String grpcInterface = config.getString("shopping-cart-service.grpc.interface");
    int grpcPort = config.getInt("shopping-cart-service.grpc.port");
    ShoppingCartService grpcService = new ShoppingCartServiceImpl(system, itemPopularityRepository);
    ShoppingCartServer.start(grpcInterface, grpcPort, system, grpcService);
  }

  static ShoppingOrderService orderServiceClient(ActorSystem<?> system) {
    GrpcClientSettings orderServiceClientSettings =
        GrpcClientSettings.connectToServiceAt(
                system.settings().config().getString("shopping-order-service.host"),
                system.settings().config().getInt("shopping-order-service.port"),
                system)
            .withTls(false);

    return ShoppingOrderServiceClient.create(orderServiceClientSettings, system);
  }
}
