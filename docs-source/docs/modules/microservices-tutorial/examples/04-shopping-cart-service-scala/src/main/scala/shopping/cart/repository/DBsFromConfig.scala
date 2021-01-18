package shopping.cart.repository

import akka.actor.typed.ActorSystem
import com.typesafe.config.Config
import scalikejdbc.config.{
  DBs,
  NoEnvPrefix,
  TypesafeConfig,
  TypesafeConfigReader
}

/**
 * Initiate the ScalikeJDBC connection pool from a given Config.
 */
class DBsFromConfig(val config: Config)
    extends DBs
    with TypesafeConfigReader
    with TypesafeConfig
    with NoEnvPrefix

object DBsFromConfig {

  /**
   * Initiate the ScalikeJDBC connection pool configuration and shutdown.
   */
  def init(system: ActorSystem[_]): Unit = {
    val scalikeJdbc: DBsFromConfig = fromConfig(system.settings.config)
    system.whenTerminated.map { _ =>
      scalikeJdbc.closeAll()
    }(scala.concurrent.ExecutionContext.Implicits.global)
  }

  def fromConfig(config: Config): DBsFromConfig = {
    val scalikeJdbc: DBsFromConfig = new DBsFromConfig(config)
    scalikeJdbc.loadGlobalSettings()
    scalikeJdbc.setup()
    scalikeJdbc
  }
}
