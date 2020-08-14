package sample.shoppingcart

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.eventstream.EventStream
import akka.pattern.StatusReply
import akka.projection.testkit.scaladsl.ProjectionTestKit
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

object ProjectionSpec {
  private val keyspace = s"ProjectionSpec_${System.currentTimeMillis()}"

  def config =
    ConfigFactory
      .parseString(s"""
      akka.actor.provider=local
      akka.persistence.cassandra {		
         events-by-tag {		
           eventual-consistency-delay = 200ms		
         }		
       		
         query {		
           refresh-interval = 500 ms		
         }		
       		
         journal.keyspace = $keyspace
         journal.keyspace-autocreate = on
         journal.tables-autocreate = on
         snapshot.keyspace = $keyspace
         snapshot.keyspace-autocreate = on
         snapshot.tables-autocreate = on		
       }
       datastax-java-driver {		
         basic.contact-points = ["127.0.0.1:9042"]		
         basic.load-balancing-policy.local-datacenter = "datacenter1"		
       }
       akka.projection.cassandra.offset-store.keyspace = $keyspace
    """)
      .withFallback(ConfigFactory.load()) // re-use application.conf other settings
}

class ProjectionSpec
    extends ScalaTestWithActorTestKit(ProjectionSpec.config)
    with AnyWordSpecLike
    with BeforeAndAfterAll {
  val projectionTestKit = ProjectionTestKit(testKit)

  override protected def beforeAll(): Unit = {
    Main.createTables(system)

    super.beforeAll()
  }

  "The events from the Shopping Cart" should {

    "be published to the system event stream by the projection" in {
      val cartProbe = createTestProbe[Any]()
      val cart = spawn(ShoppingCart("cart-1", Set(s"${ShoppingCart.TagPrefix}-0")))
      cart ! ShoppingCart.AddItem("25", 12, cartProbe.ref)
      cartProbe.expectMessageType[StatusReply[ShoppingCart.Summary]].isSuccess should ===(true)

      val eventProbe = createTestProbe[ShoppingCart.Event]()
      system.eventStream ! EventStream.Subscribe(eventProbe.ref)
      projectionTestKit.run(EventProcessor.createProjectionFor(system, 0)) {
        val added = eventProbe.expectMessageType[ShoppingCart.ItemAdded]
        added.cartId should ===("cart-1")
        added.itemId should ===("25")
        added.quantity should ===(12)
      }
    }
  }

}
