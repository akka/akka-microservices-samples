package shopping.cart;

import static java.util.concurrent.TimeUnit.SECONDS;

import akka.actor.typed.ActorSystem;
import akka.persistence.jdbc.testkit.javadsl.SchemaUtils;
import akka.projection.jdbc.javadsl.JdbcProjection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;
import org.springframework.orm.jpa.JpaTransactionManager;
import shopping.cart.repository.HibernateJdbcSession;

public class CreateTableTestUtils {

  /**
   * Test utility to create journal and projection tables for tests environment. JPA/Hibernate
   * tables are auto created (drop-and-create) using settings flag, see persistence-test.conf
   */
  public static void createTables(JpaTransactionManager transactionManager, ActorSystem<?> system)
      throws Exception {

    // create schemas
    // ok to block here, main test thread
    SchemaUtils.dropIfExists(system).toCompletableFuture().get(30, SECONDS);
    SchemaUtils.createIfNotExists(system).toCompletableFuture().get(30, SECONDS);

    JdbcProjection.dropOffsetTableIfExists(
            () -> new HibernateJdbcSession(transactionManager), system)
        .toCompletableFuture()
        .get(30, SECONDS);
    JdbcProjection.createOffsetTableIfNotExists(
            () -> new HibernateJdbcSession(transactionManager), system)
        .toCompletableFuture()
        .get(30, SECONDS);

    dropCreateUserTables(system);

    LoggerFactory.getLogger(CreateTableTestUtils.class).info("Tables created");
  }

  /**
   * Drops and create user tables (if applicable)
   *
   * <p>This file is only created on step 4 (projection query). Calling this method has no effect on
   * previous steps.
   */
  private static void dropCreateUserTables(ActorSystem<?> system) throws Exception {

    Path path = Paths.get("ddl-scripts/create_user_tables.sql");
    if (path.toFile().exists()) {
      SchemaUtils.applyScript("DROP TABLE IF EXISTS public.item_popularity;", system)
          .toCompletableFuture()
          .get(30, SECONDS);

      String script = Files.lines(path, StandardCharsets.UTF_8).collect(Collectors.joining("\n"));
      SchemaUtils.applyScript(script, system).toCompletableFuture().get(30, SECONDS);
      ;
    }
  }
}
