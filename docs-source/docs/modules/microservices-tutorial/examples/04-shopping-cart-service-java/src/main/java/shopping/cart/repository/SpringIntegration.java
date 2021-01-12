package shopping.cart.repository;

import com.typesafe.config.Config;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class SpringIntegration {

  public static ApplicationContext applicationContext(Config config) {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.registerBean(Config.class, () -> config);
    context.register(SpringConfig.class);
    context.refresh();
    return context;
  }
}
