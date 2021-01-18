// tag::trait[]
// tag::impl[]
package shopping.cart

import scala.concurrent.Future
import akka.Done
// end::trait[]
import scala.concurrent.ExecutionContext
import akka.stream.alpakka.cassandra.scaladsl.CassandraSession
// end::impl[]

// tag::trait[]

trait ItemPopularityRepository {
  def update(itemId: String, delta: Int): Future[Done]
  def getItem(itemId: String): Future[Option[Long]]
}
// end::trait[]

// tag::impl[]

object ItemPopularityRepositoryImpl {
  val popularityTable = "item_popularity"
}

class ItemPopularityRepositoryImpl(session: CassandraSession, keyspace: String)(
    implicit val ec: ExecutionContext)
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
      .selectOne(
        s"SELECT item_id, count FROM $keyspace.$popularityTable WHERE item_id = ?",
        itemId)
      .map(opt => opt.map(row => row.getLong("count").longValue()))
  }
}
// end::impl[]
