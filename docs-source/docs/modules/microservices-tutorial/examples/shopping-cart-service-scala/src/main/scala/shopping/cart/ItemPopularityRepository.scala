// tag::trait[]
// tag::impl[]
package shopping.cart

import scalikejdbc._
// end::trait[]
// end::impl[]

// tag::trait[]

trait ItemPopularityRepository {
  def update(itemId: String, delta: Int): Unit
  def getItem(itemId: String): Option[Long]
}
// end::trait[]

// tag::impl[]

class ItemPopularityRepositoryImpl() extends ItemPopularityRepository {

  override def update(itemId: String, delta: Int): Unit = {
    // TODO hook into transaction
    DB.localTx { implicit session =>
      // This uses the PostgreSQL `ON CONFLICT` feature
      // Alternatively, this can be implemented by first issuing the `UPDATE`
      // and checking for the updated rows count. If no rows got updated issue
      // the `INSERT` instead.
      sql"""
           INSERT INTO item_popularity (itemid, count) VALUES ($itemId, $delta)
           ON CONFLICT (itemid) DO UPDATE SET count = item_popularity.count + $delta
         """.executeUpdate().apply()
    }
  }

  override def getItem(itemId: String): Option[Long] = {
    DB.readOnly { implicit session =>
      sql"SELECT count FROM item_popularity WHERE itemid = $itemId"
        .map(_.long("count"))
        .toOption()
        .apply()
    }
  }
}
// end::impl[]
