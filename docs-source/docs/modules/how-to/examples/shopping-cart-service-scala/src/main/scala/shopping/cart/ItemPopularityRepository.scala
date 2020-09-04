package shopping.cart

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import akka.Done
import akka.stream.alpakka.cassandra.scaladsl.CassandraSession

trait ItemPopularityRepository {

  def update(itemId: String, delta: Int): Future[Done]
  def getItem(itemId: String): Future[Option[Long]]
}
