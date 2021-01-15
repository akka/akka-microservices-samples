package shopping.cart

import akka.Done
import akka.actor.typed.ActorSystem
import akka.persistence.jdbc.testkit.scaladsl.SchemaUtils
import akka.projection.jdbc.scaladsl.JdbcProjection
import org.slf4j.LoggerFactory
import shopping.cart.repository.{ DBsFromConfig, ScalikeJdbcSession }

import java.nio.file.Paths
import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext }

object CreateTableTestUtils {

  private var scalikeJdbc: DBsFromConfig = _

  def setupScalikeJdbcConnectionPool(system: ActorSystem[_]): Unit = {
    scalikeJdbc = new DBsFromConfig(system.settings.config)
    scalikeJdbc.loadGlobalSettings()
    scalikeJdbc.setup()
  }

  def closeScalikeJdbcConnectionPool(): Unit = {
    scalikeJdbc.closeAll()
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
