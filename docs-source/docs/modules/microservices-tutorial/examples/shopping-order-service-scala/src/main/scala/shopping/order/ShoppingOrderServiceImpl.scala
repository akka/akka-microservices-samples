package shopping.order

import scala.concurrent.Future

import org.slf4j.LoggerFactory
import shopping.order.proto.OrderRequest
import shopping.order.proto.OrderResponse

class ShoppingOrderServiceImpl
    extends proto.ShoppingOrderService {

  private val logger = LoggerFactory.getLogger(getClass)

  override def order(
      in: OrderRequest): Future[OrderResponse] = {
    val totalNumberOfItems =
      in.items.iterator.map(_.quantity).sum
    logger.info(
      "Order {} items from cart {}.",
      totalNumberOfItems,
      in.cartId)
    Future.successful(OrderResponse(ok = true))
  }
}
