package shopping.cart;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.DispatcherSelector;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
// tag::SendOrderProjection[]
import akka.grpc.GrpcClientSettings;
// end::SendOrderProjection[]
import akka.management.cluster.bootstrap.ClusterBootstrap;
import akka.management.javadsl.AkkaManagement;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.jpa.JpaTransactionManager;
import shopping.cart.proto.ShoppingCartService;
import shopping.cart.repository.AsyncItemPopularityRepository;
import shopping.cart.repository.ItemPopularityRepository;
import shopping.cart.repository.SpringIntegration;
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

    PublishEventsProjection.init(system, transactionManager);

    ShoppingOrderService orderService = orderServiceClient(system);
    SendOrderProjection.init(system, transactionManager, orderService);
  }

  // tag::SendOrderProjection[]
  // can be overridden in tests
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
