package shopping.cart

import akka.Done
import akka.actor.typed.ActorSystem
import akka.persistence.jdbc.testkit.scaladsl.SchemaUtils
import akka.projection.jdbc.scaladsl.JdbcProjection
import com.typesafe.config.Config
import org.slf4j.LoggerFactory
import shopping.cart.dbaccess.{ DBsFromConfig, ScalikeJdbcSession }

import java.nio.file.Paths
import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }

object CreateTableTestUtils {

  private var scalikeJdbc: DBsFromConfig = _

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
        _ <- dropUserTables()
        _ <- SchemaUtils.applyScript(
          fromFileAsString("ddl-scripts/create_user_tables.sql"))
      } yield Done,
      30.seconds)

    LoggerFactory
      .getLogger("shopping.cart.CreateTableTestUtils")
      .info("Created tables")
  }

  private def dropUserTables()(
      implicit system: ActorSystem[_]): Future[Done] = {
    val path = Paths.get("ddl-scripts/create_user_tables.sql")
    if (path.toFile().exists()) {
      SchemaUtils.applyScript("DROP TABLE IF EXISTS public.item_popularity;")
    } else Future.successful(Done)
  }

  private def fromFileAsString(fileName: String): String = {
    val source = scala.io.Source.fromFile(Paths.get(fileName).toFile)
    val contents = source.mkString
    source.close()
    contents
  }
}
