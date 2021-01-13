package shopping.cart

import akka.Done
import akka.actor.typed.ActorSystem
import akka.persistence.jdbc.testkit.scaladsl.SchemaUtils
import akka.projection.jdbc.scaladsl.JdbcProjection
import org.slf4j.LoggerFactory
import scalikejdbc.ConnectionPool
import shopping.cart.repository.ScalikeJdbcSession

import java.nio.file.Paths
import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext }

object CreateTableTestUtils {

  def setupScalikeJdbcConnectionPool(system: ActorSystem[_]): Unit = {
    val c = system.settings.config.getConfig("db.default")
    ConnectionPool.singleton(
      c.getString("url"),
      c.getString("user"),
      c.getString("password"))
  }

  def closeScalikeJdbcConnectionPool(): Unit = {
    ConnectionPool.close()
  }

  def dropAndRecreateTables(system: ActorSystem[_]): Unit = {
    implicit val sys: ActorSystem[_] = system
    implicit val ec: ExecutionContext = system.executionContext

    val scalikeJdbcSession = new ScalikeJdbcSession()
    try {
      // ok to block here, main thread
      Await.result(
        for {
          _ <- SchemaUtils.dropIfExists()
          _ <- SchemaUtils.createIfNotExists()
          _ <- JdbcProjection.dropOffsetTableIfExists(() => scalikeJdbcSession)
          _ <- JdbcProjection.createOffsetTableIfNotExists(() =>
            scalikeJdbcSession)
          _ <- SchemaUtils.applyScript(
            fromFileAsString("ddl-scripts/drop_user_tables.sql"))
          _ <- SchemaUtils.applyScript(
            fromFileAsString("ddl-scripts/create_user_tables.sql"))
        } yield Done,
        30.seconds)
    } finally {
      scalikeJdbcSession.close()
    }

    LoggerFactory
      .getLogger("shopping.cart.CreateTableTestUtils")
      .info("Created tables")
  }

  private def fromFileAsString(fileName: String): String = {
    val source = scala.io.Source.fromFile(Paths.get(fileName).toFile)
    val contents = source.mkString
    source.close()
    contents
  }
}
