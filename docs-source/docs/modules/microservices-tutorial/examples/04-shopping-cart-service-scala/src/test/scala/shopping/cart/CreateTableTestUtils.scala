package shopping.cart

import akka.Done
import akka.actor.typed.ActorSystem
import akka.persistence.jdbc.testkit.scaladsl.SchemaUtils
import akka.projection.jdbc.scaladsl.JdbcProjection
import com.typesafe.config.Config
import org.slf4j.LoggerFactory
import shopping.cart.repository.{ DBsFromConfig, ScalikeJdbcSession }

import java.nio.file.Paths
import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }

object CreateTableTestUtils {

  private var scalikeJdbc: DBsFromConfig = _
  private val createUserTablesFile =
    Paths.get("ddl-scripts/create_user_tables.sql").toFile

  def setupScalikeJdbcConnectionPool(config: Config): Unit = {
    scalikeJdbc = DBsFromConfig.fromConfig(config)
  }

  def closeScalikeJdbcConnectionPool(): Unit = {
    scalikeJdbc.closeAll()
  }

  def dropAndRecreateTables(system: ActorSystem[_]): Unit = {
    implicit val sys: ActorSystem[_] = system
    implicit val ec: ExecutionContext = system.executionContext

    // ok to block here, main thread
    Await.result(
      for {
        _ <- SchemaUtils.dropIfExists()
        _ <- SchemaUtils.createIfNotExists()
        _ <- JdbcProjection.dropOffsetTableIfExists(() =>
          new ScalikeJdbcSession())
        _ <- JdbcProjection.createOffsetTableIfNotExists(() =>
          new ScalikeJdbcSession())
      } yield Done,
      30.seconds)
    if (createUserTablesFile.exists()) {
      Await.result(
        for {
          _ <- dropUserTables()
          _ <- SchemaUtils.applyScript(createUserTablesSql)
        } yield Done,
        30.seconds)
    }
    LoggerFactory
      .getLogger("shopping.cart.CreateTableTestUtils")
      .info("Created tables")
  }

  private def dropUserTables()(
      implicit system: ActorSystem[_]): Future[Done] = {
    SchemaUtils.applyScript("DROP TABLE IF EXISTS public.item_popularity;")
  }

  private def createUserTablesSql: String = {
    val source = scala.io.Source.fromFile(createUserTablesFile)
    val contents = source.mkString
    source.close()
    contents
  }
}
