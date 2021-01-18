package shopping.cart

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration._

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.cluster.MemberStatus
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.typed.Cluster
import akka.cluster.typed.Join
import akka.persistence.testkit.scaladsl.PersistenceInit
import akka.stream.alpakka.cassandra.scaladsl.CassandraSessionRegistry
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.scalatest.wordspec.AnyWordSpecLike

object ItemPopularityIntegrationSpec {
  private val uniqueQualifier = System.currentTimeMillis()
  private val keyspace =
    s"ItemPopularityIntegrationSpec_$uniqueQualifier"

  val config: Config = ConfigFactory
    .parseString(s"""
      akka.remote.artery.canonical {
        hostname = "127.0.0.1"
        port = 0
      }

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
    .withFallback(ConfigFactory.load())

}

class ItemPopularityIntegrationSpec
    extends ScalaTestWithActorTestKit(ItemPopularityIntegrationSpec.config)
    with AnyWordSpecLike {

  private lazy val itemPopularityRepository = {
    val session =
      CassandraSessionRegistry(system).sessionFor("akka.persistence.cassandra")
    // use same keyspace for the item_popularity table as the offset store
    val itemPopularityKeyspace =
      system.settings.config
        .getString("akka.projection.cassandra.offset-store.keyspace")
    new ItemPopularityRepositoryImpl(session, itemPopularityKeyspace)(
      system.executionContext)
  }

  override protected def beforeAll(): Unit = {
    // avoid concurrent creation of keyspace and tables
    val timeout = 10.seconds
    Await.result(
      PersistenceInit.initializeDefaultPlugins(system, timeout),
      timeout)
    CreateTableTestUtils.createTables(system)

    ShoppingCart.init(system)

    ItemPopularityProjection.init(system, itemPopularityRepository)

    super.beforeAll()
  }

  "Item popularity projection" should {
    "init and join Cluster" in {
      Cluster(system).manager ! Join(Cluster(system).selfMember.address)

      // let the node join and become Up
      eventually {
        Cluster(system).selfMember.status should ===(MemberStatus.Up)
      }
    }

    "should consume cart events and update popularity count" in {
      val sharding = ClusterSharding(system)
      val cartId1 = "cart1"
      val cartId2 = "cart2"
      val item1 = "item1"
      val item2 = "item2"

      val cart1 = sharding.entityRefFor(ShoppingCart.EntityKey, cartId1)
      val cart2 = sharding.entityRefFor(ShoppingCart.EntityKey, cartId2)

      val reply1: Future[ShoppingCart.Summary] =
        cart1.askWithStatus(ShoppingCart.AddItem(item1, 3, _))
      reply1.futureValue.items.values.sum should ===(3)

      eventually {
        itemPopularityRepository.getItem(item1).futureValue.get should ===(3)
      }

      val reply2: Future[ShoppingCart.Summary] =
        cart1.askWithStatus(ShoppingCart.AddItem(item2, 5, _))
      reply2.futureValue.items.values.sum should ===(3 + 5)
      // another cart
      val reply3: Future[ShoppingCart.Summary] =
        cart2.askWithStatus(ShoppingCart.AddItem(item2, 4, _))
      reply3.futureValue.items.values.sum should ===(4)

      eventually {
        itemPopularityRepository.getItem(item2).futureValue.get should ===(
          5 + 4)
      }
      itemPopularityRepository.getItem(item1).futureValue.get should ===(3)
    }

  }
}
