package shopping.cart;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.management.cluster.bootstrap.ClusterBootstrap;
import akka.management.javadsl.AkkaManagement;
import akka.stream.alpakka.cassandra.javadsl.CassandraSession;
import akka.stream.alpakka.cassandra.javadsl.CassandraSessionRegistry;

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
  }

  @Override
  public Receive<Void> createReceive() {
    return newReceiveBuilder().build();
  }
}
