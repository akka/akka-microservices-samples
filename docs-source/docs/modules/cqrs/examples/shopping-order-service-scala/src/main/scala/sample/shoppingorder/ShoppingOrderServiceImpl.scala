package sample.shoppingorder

import scala.concurrent.Future

import org.slf4j.LoggerFactory
import sample.shoppingorder.proto.OrderRequest
import sample.shoppingorder.proto.OrderResponse

class ShoppingOrderServiceImpl extends proto.ShoppingOrderService {

  private val logger = LoggerFactory.getLogger(getClass)

  override def order(in: OrderRequest): Future[OrderResponse] = {
    logger.info("Order {} items from cart {}.", in.items.size, in.cartId)
    Future.successful(OrderResponse(ok = true))
  }
}
