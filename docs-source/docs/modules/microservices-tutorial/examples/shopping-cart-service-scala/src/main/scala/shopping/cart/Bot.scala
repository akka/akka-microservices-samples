/*
 * Copyright (C) 2020 Lightbend Inc. <https://www.lightbend.com>
 */
package shopping.cart

import java.util.concurrent.ThreadLocalRandom

import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Success

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.util.Timeout

object Bot {
  sealed trait Message
  object Tick extends Message
  case class Reply(shoppingCart: ShoppingCart.Summary, duration: FiniteDuration) extends Message
  case class Err(reason: String, duration: FiniteDuration) extends Message

  private implicit val askTimeout: Timeout = 30.seconds

  def apply(): Behavior[Message] = {
    Behaviors.setup { context =>
      val sharding = ClusterSharding(context.system)

      Behaviors.withTimers { timers =>
        timers.startTimerWithFixedDelay(Tick, 5.seconds)
        Behaviors.receiveMessage {
          case Tick =>
            val cartId = s"cart-${ThreadLocalRandom.current().nextInt(1000)}"
            val itemId = s"item-${ThreadLocalRandom.current().nextInt(10000)}"
            val ref = sharding.entityRefFor(ShoppingCart.EntityKey, cartId)

            val startTime = System.nanoTime()
            def duration() = (System.nanoTime() - startTime).nanos
            context.askWithStatus[ShoppingCart.AddItem, ShoppingCart.Summary](ref, ShoppingCart.AddItem(itemId, 1, _)) {
              case Success(summary) => Reply(summary, duration())
              case Failure(exc)     => Err(exc.getMessage, duration())
            }
            Behaviors.same
          case Reply(cart, duration) =>
            context.log.info("Update took {} ms, {}", duration.toMillis, cart)
            Behaviors.same
          case Err(reason, duration) =>
            context.log.info("Err after {} ms: {}", duration.toMillis, reason)
            Behaviors.same
        }
      }
    }
  }

}
