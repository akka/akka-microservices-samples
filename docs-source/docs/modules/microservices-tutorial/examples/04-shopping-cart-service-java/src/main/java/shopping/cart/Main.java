package shopping.cart;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import akka.management.cluster.bootstrap.ClusterBootstrap;
import akka.management.javadsl.AkkaManagement;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.jpa.JpaTransactionManager;
import shopping.cart.proto.ShoppingCartService;
import shopping.cart.repository.ItemPopularityRepository;
import shopping.cart.repository.SpringIntegration;

public class Main {

  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {
    ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "ShoppingCartService");
    try {
      init(system);
    } catch (Exception e) {
      logger.error("Terminating due to initialization failure.", e);
      system.terminate();
    }
  }

  public static void init(ActorSystem<Void> system) {
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

    Config config = system.settings().config();
    String grpcInterface = config.getString("shopping-cart-service.grpc.interface");
    int grpcPort = config.getInt("shopping-cart-service.grpc.port");
    ShoppingCartService grpcService = new ShoppingCartServiceImpl(system, itemPopularityRepository);
    ShoppingCartServer.start(grpcInterface, grpcPort, system, grpcService);
  }
}
