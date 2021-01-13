package shopping.cart.repository

import akka.japi.function.Function
import akka.projection.jdbc.JdbcSession
import scalikejdbc._

import java.sql.Connection

final class ScalikeJdbcSession extends JdbcSession {
  private implicit val db: DB = DB.connect()

  override def withConnection[Result](
      func: Function[Connection, Result]): Result = {
    DB.autoCommitWithConnection(func(_))
  }

  override def commit(): Unit = ()

  override def rollback(): Unit = ()

  override def close(): Unit = db.close()
}
