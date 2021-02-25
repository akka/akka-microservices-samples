package shopping.cart.repository;

import akka.actor.typed.ActorSystem;
import com.typesafe.config.Config;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Provides an integration point for initializing a Spring {@link ApplicationContext} configured for
 * working with Akka Projections.
 */
public class SpringIntegration {

  /**
   * Returns a Spring {@link ApplicationContext} configured to provide all the infrastructure
   * necessary for working with Akka Projections.
   */
  public static ApplicationContext applicationContext(ActorSystem<?> system) {
    Config config = system.settings().config();
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    // register the Config as a bean so it can be later injected into SpringConfig
    context.registerBean(Config.class, () -> config);
    context.register(SpringConfig.class);
    context.refresh();

    // Make sure the Spring context is closed when the actor system terminates
    system.getWhenTerminated().thenAccept(done -> context.close());

    return context;
  }
}
