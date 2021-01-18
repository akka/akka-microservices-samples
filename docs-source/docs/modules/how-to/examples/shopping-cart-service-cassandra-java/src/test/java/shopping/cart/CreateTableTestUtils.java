package shopping.cart;

import static java.util.concurrent.TimeUnit.SECONDS;

import akka.actor.typed.ActorSystem;
import akka.projection.cassandra.javadsl.CassandraProjection;
import akka.stream.alpakka.cassandra.javadsl.CassandraSession;
import akka.stream.alpakka.cassandra.javadsl.CassandraSessionRegistry;
import org.slf4j.LoggerFactory;

public class CreateTableTestUtils {
  public static void createTables(ActorSystem<?> system) throws Exception {
    // ok to block here, main thread
    CassandraProjection.createOffsetTableIfNotExists(system).toCompletableFuture().get(30, SECONDS);

    // use same keyspace for the item_popularity table as the offset store
    String keyspace =
        system.settings().config().getString("akka.projection.cassandra.offset-store.keyspace");
    CassandraSession session =
        CassandraSessionRegistry.get(system).sessionFor("akka.persistence.cassandra");

    session
        .executeDDL(
            "CREATE TABLE IF NOT EXISTS "
                + keyspace
                + "."
                + ItemPopularityRepositoryImpl.POPULARITY_TABLE
                + " (\n"
                + "item_id text,\n"
                + "count counter,\n"
                + "PRIMARY KEY (item_id))")
        .toCompletableFuture()
        .get(30, SECONDS);

    LoggerFactory.getLogger(CreateTableTestUtils.class)
        .info("Created keyspace [{}] and tables", keyspace);
  }
}
