package shopping.cart;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
// tag::SendOrderProjection[]
import akka.grpc.GrpcClientSettings;
// end::SendOrderProjection[]
import akka.management.cluster.bootstrap.ClusterBootstrap;
import akka.management.javadsl.AkkaManagement;
import akka.stream.alpakka.cassandra.javadsl.CassandraSession;
import akka.stream.alpakka.cassandra.javadsl.CassandraSessionRegistry;
// tag::SendOrderProjection[]
import shopping.order.proto.ShoppingOrderService;
import shopping.order.proto.ShoppingOrderServiceClient;
// end::SendOrderProjection[]

// tag::ItemPopularityProjection[]
public class Main extends AbstractBehavior<Void> {

  public static void main(String[] args) throws Exception {
    ActorSystem<Void> system = ActorSystem.create(Main.create(), "ShoppingCartService");
  }

  public static Behavior<Void> create() {
    return Behaviors.setup(Main::new);
  }

  public Main(ActorContext<Void> context) {
    super(context);

    ActorSystem<?> system = context.getSystem();

    // FIXME no get(ClassicActorSystemProvider) for Java?
    AkkaManagement.get(system.classicSystem()).start();
    ClusterBootstrap.get(system.classicSystem()).start();

    ShoppingCart.init(system);

    // tag::ItemPopularityProjection[]
    CassandraSession session =
        CassandraSessionRegistry.get(system).sessionFor("akka.persistence.cassandra"); // <1>
    // use same keyspace for the item_popularity table as the offset store
    String itemPopularityKeyspace =
        system.settings().config().getString("akka.projection.cassandra.offset-store.keyspace");
    ItemPopularityRepository itemPopularityRepository =
        new ItemPopularityRepositoryImpl(session, itemPopularityKeyspace); // <2>

    ItemPopularityProjection.init(system, itemPopularityRepository); // <3>
    // end::ItemPopularityProjection[]

    String grpcInterface =
        system.settings().config().getString("shopping-cart-service.grpc.interface");
    int grpcPort = system.settings().config().getInt("shopping-cart-service.grpc.port");
    ShoppingCartServer.start(grpcInterface, grpcPort, system, itemPopularityRepository);

    // tag::PublishEventsProjection[]
    PublishEventsProjection.init(system);
    // end::PublishEventsProjection[]

    // tag::SendOrderProjection[]
    ShoppingOrderService orderService = orderServiceClient(system);
    SendOrderProjection.init(system, orderService);
    // end::SendOrderProjection[]
  }

  // can be overridden in tests
  // tag::SendOrderProjection[]
  protected ShoppingOrderService orderServiceClient(ActorSystem<?> system) {
    GrpcClientSettings orderServiceClientSettings =
        GrpcClientSettings.connectToServiceAt(
                system.settings().config().getString("shopping-order-service.host"),
                system.settings().config().getInt("shopping-order-service.port"),
                system)
            .withTls(false);

    return ShoppingOrderServiceClient.create(orderServiceClientSettings, system);
  }
  // end::SendOrderProjection[]

  @Override
  public Receive<Void> createReceive() {
    return newReceiveBuilder().build();
  }
}
