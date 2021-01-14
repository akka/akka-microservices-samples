package shopping.cart;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.management.cluster.bootstrap.ClusterBootstrap;
import akka.management.javadsl.AkkaManagement;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.jpa.JpaTransactionManager;
import shopping.cart.proto.ShoppingCartService;
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

    // tag::ItemPopularityProjection[]
    // tag::repo-instance[]
    ApplicationContext springContext =
        SpringIntegration.applicationContext(system.settings().config()); // <1>

    ItemPopularityRepository itemPopularityRepository =
        springContext.getBean(ItemPopularityRepository.class); // <2>
    // end::repo-instance[]

    // end::ItemPopularityProjection[]

    // tag::ItemPopularityProjection[]
    JpaTransactionManager transactionManager =
        springContext.getBean(JpaTransactionManager.class); // <3>

    ItemPopularityProjection.init(system, transactionManager, itemPopularityRepository); // <4>
    // end::ItemPopularityProjection[]

    String grpcInterface =
        system.settings().config().getString("shopping-cart-service.grpc.interface");
    int grpcPort = system.settings().config().getInt("shopping-cart-service.grpc.port");

    ShoppingCartService grpcService = new ShoppingCartServiceImpl(system, itemPopularityRepository);

    ShoppingCartServer.start(grpcInterface, grpcPort, system, grpcService);
  }

  @Override
  public Receive<Void> createReceive() {
    return newReceiveBuilder().build();
  }
}
