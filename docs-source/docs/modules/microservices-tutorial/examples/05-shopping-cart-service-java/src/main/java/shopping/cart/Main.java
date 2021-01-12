package shopping.cart;


import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.DispatcherSelector;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.management.cluster.bootstrap.ClusterBootstrap;
import akka.management.javadsl.AkkaManagement;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.jpa.JpaTransactionManager;
import shopping.cart.proto.ShoppingCartService;
import shopping.cart.repository.AsyncItemPopularityRepository;
import shopping.cart.repository.ItemPopularityRepository;
import shopping.cart.repository.SpringIntegration;

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

    AkkaManagement.get(system).start();
    ClusterBootstrap.get(system).start();

    ShoppingCart.init(system);

    ApplicationContext springContext =
        SpringIntegration.applicationContext(system.settings().config());
    JpaTransactionManager transactionManager = springContext.getBean(JpaTransactionManager.class);

    ItemPopularityRepository itemPopularityRepository =
        springContext.getBean(ItemPopularityRepository.class);

    ItemPopularityProjection.init(system, transactionManager, itemPopularityRepository);

    String grpcInterface =
        system.settings().config().getString("shopping-cart-service.grpc.interface");
    int grpcPort = system.settings().config().getInt("shopping-cart-service.grpc.port");

    AsyncItemPopularityRepository asyncItemPopularityRepository =
        new AsyncItemPopularityRepository(
            system.dispatchers().lookup(DispatcherSelector.blocking()), itemPopularityRepository);
    ShoppingCartService grpcService = new ShoppingCartServiceImpl(system, asyncItemPopularityRepository);

    ShoppingCartServer.start(grpcInterface, grpcPort, system, grpcService);

    // tag::PublishEventsProjection[]
    PublishEventsProjection.init(system, transactionManager);
    // end::PublishEventsProjection[]

  }

  @Override
  public Receive<Void> createReceive() {
    return newReceiveBuilder().build();
  }
}
