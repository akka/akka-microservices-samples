package shopping.cart

import scala.concurrent.Future

import akka.Done

trait ItemPopularityRepository {

  def update(itemId: String, delta: Int): Future[Done]
  def getItem(itemId: String): Future[Option[Long]]
}
