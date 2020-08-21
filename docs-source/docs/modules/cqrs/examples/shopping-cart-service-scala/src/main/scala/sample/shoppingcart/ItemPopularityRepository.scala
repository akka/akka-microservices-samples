package sample.shoppingcart

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import akka.Done
import akka.stream.alpakka.cassandra.scaladsl.CassandraSession

trait ItemPopularityRepository {

  def update(itemId: String, delta: Int): Future[Done]
  def getItem(itemId: String): Future[Option[Long]]
}

object ItemPopularityRepositoryImpl {
  val popularityTable = "item_popularity"

  def createItemPopularityTable(session: CassandraSession, keyspace: String): Future[Done] = {
    session.executeDDL(s"""CREATE TABLE IF NOT EXISTS $keyspace.$popularityTable (
      item_id text,
      count counter, // <1>
      PRIMARY KEY (item_id))
      """)
  }
}

class ItemPopularityRepositoryImpl(session: CassandraSession, keyspace: String)(implicit val ec: ExecutionContext)
    extends ItemPopularityRepository {
  import ItemPopularityRepositoryImpl.popularityTable

  override def update(itemId: String, delta: Int): Future[Done] = {
    session.executeWrite(
      s"UPDATE $keyspace.$popularityTable SET count = count + ? WHERE item_id = ?",
      java.lang.Long.valueOf(delta),
      itemId)
  }

  override def getItem(itemId: String): Future[Option[Long]] = {
    session
      .selectOne(s"SELECT item_id, count FROM $keyspace.$popularityTable WHERE item_id = ?", itemId)
      .map(opt => opt.map(row => row.getLong("count").longValue()))
  }
}
