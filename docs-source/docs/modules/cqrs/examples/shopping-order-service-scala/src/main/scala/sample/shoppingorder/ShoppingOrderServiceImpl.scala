package sample.shoppingorder

import scala.concurrent.Future

import akka.actor.typed.ActorSystem
import org.slf4j.LoggerFactory
import sample.shoppingorder.proto.OrderRequest
import sample.shoppingorder.proto.OrderResponse

class ShoppingOrderServiceImpl(implicit system: ActorSystem[_]) extends proto.ShoppingOrderService {

  private val logger = LoggerFactory.getLogger(getClass)

  override def order(in: OrderRequest): Future[OrderResponse] = {
    logger.info("Order {} items from cart {}.", in.items.size, in.cartId)
    Future.successful(OrderResponse(ok = true))
  }
}
