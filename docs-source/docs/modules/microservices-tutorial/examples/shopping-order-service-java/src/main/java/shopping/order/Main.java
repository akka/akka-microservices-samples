package shopping.order;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import akka.management.cluster.bootstrap.ClusterBootstrap;
import akka.management.javadsl.AkkaManagement;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shopping.order.proto.ShoppingOrderService;

public class Main {

  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) throws Exception {
    ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "ShoppingOrderService");
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

    Config config = system.settings().config();
    String grpcInterface = config.getString("shopping-order-service.grpc.interface");
    int grpcPort = config.getInt("shopping-order-service.grpc.port");
    ShoppingOrderService grpcService = new ShoppingOrderServiceImpl();
    ShoppingOrderServer.start(grpcInterface, grpcPort, system, grpcService);
  }
}
