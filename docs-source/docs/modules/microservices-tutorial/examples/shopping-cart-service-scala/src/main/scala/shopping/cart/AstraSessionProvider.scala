package shopping.cart

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.compat.java8.FutureConverters._

import akka.actor.ActorSystem
import akka.stream.alpakka.cassandra.CqlSessionProvider
import akka.stream.alpakka.cassandra.DriverConfigLoaderFromConfig
import com.datastax.oss.driver.api.core.CqlSession
import com.typesafe.config.Config

class AstraSessionProvider(system: ActorSystem, config: Config) extends CqlSessionProvider {
  override def connect()(implicit ec: ExecutionContext): Future[CqlSession] = {
    val driverConfig = CqlSessionProvider.driverConfig(system, config)
    val driverConfigLoader = DriverConfigLoaderFromConfig.fromConfig(driverConfig)
    CqlSession
      .builder()
      .withConfigLoader(driverConfigLoader)
      .withCloudSecureConnectBundle(
        getClass.getClassLoader.getResourceAsStream("cassandra-astra-secure-connect-akka-patriknw.zip"))
      .withAuthCredentials("patriknw", "xxx")
      .buildAsync()
      .toScala
  }
}
