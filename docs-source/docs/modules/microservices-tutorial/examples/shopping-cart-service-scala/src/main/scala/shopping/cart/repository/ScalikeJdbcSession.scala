package shopping.cart.repository

import akka.japi.function.Function
import akka.projection.jdbc.JdbcSession
import org.slf4j.LoggerFactory
import scalikejdbc._

import java.sql.Connection

object ScalikeJdbcSession {
  def withSession[R](f: ScalikeJdbcSession => R): Unit = {
    val session = new ScalikeJdbcSession()
    try {
      f(session)
    } finally {
      session.close()
    }
  }
}

final class ScalikeJdbcSession extends JdbcSession {
  private val log = LoggerFactory.getLogger(getClass)

  val db: DB = DB.connect()

  override def withConnection[Result](
      func: Function[Connection, Result]): Result = {
    db.begin()
    db.withinTxWithConnection(func(_))
  }

  override def commit(): Unit = {
    db.commit()
    log.debug("committed {}", db)
  }

  override def rollback(): Unit = db.rollback()

  override def close(): Unit = () //db.close() //db.close()
}
