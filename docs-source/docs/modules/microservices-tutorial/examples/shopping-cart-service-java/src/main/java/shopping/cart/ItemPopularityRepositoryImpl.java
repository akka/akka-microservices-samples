package shopping.cart;

import akka.Done;
import akka.stream.alpakka.cassandra.javadsl.CassandraSession;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class ItemPopularityRepositoryImpl implements ItemPopularityRepository {

    private static final String POPULARITY_TABLE = "item_popularity";

    public static CompletionStage<Done> createItemPopularityTable(CassandraSession session, String keyspace) {
        return session.executeDDL("CREATE TABLE IF NOT EXISTS " + keyspace + "." + POPULARITY_TABLE + " (\n" +
        "item_id text,\n" +
        "count counter,\n" + // <1>
        "PRIMARY KEY (item_id))"
        );
    }

    private final CassandraSession session;
    private final String table;

    public ItemPopularityRepositoryImpl(CassandraSession session, String keyspace) {
        this.session = session;
        this.table = keyspace + "." + POPULARITY_TABLE;
    }

    @Override
    public CompletionStage<Done> update(String itemId, int delta) {
        return session.executeWrite(
                "UPDATE " + table + " SET count = count + ? WHERE item_id = ?",
                Long.valueOf(delta),
                itemId
        );
    }

    @Override
    public CompletionStage<Optional<Long>> getItem(String itemId) {
        return session.selectOne("SELECT item_id, count FROM " + table + " WHERE item_id = ?", itemId)
                .thenApply(opt -> opt.map(row -> row.getLong("count")));
    }
}
