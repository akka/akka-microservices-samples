package shopping.cart

import akka.actor.typed.{ ActorSystem, Behavior }
import akka.actor.typed.scaladsl.{ AbstractBehavior, ActorContext }
import akka.management.scaladsl.AkkaManagement

class Main(context: ActorContext[Nothing]) {
  val system = context.system
  // tag::start-akka-management[]
  AkkaManagement(system).start()
  // end::start-akka-management[]
}
