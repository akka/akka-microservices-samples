package shopping.cart.repository

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
