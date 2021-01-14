package shopping.cart.repository;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/** Configure the necessary components required for integration with Akka Projections */
@Configuration
@EnableJpaRepositories
@EnableTransactionManagement
public class SpringConfig {

  private final Config config;

  public SpringConfig(Config config) {
    this.config = config;
  }

  /**
   * Configures a {@link JpaTransactionManager} to be used by Akka Projections. The transaction
   * manager should be used to construct a {@link shopping.cart.repository.HibernateJdbcSession}
   * that is then used to configure the {@link akka.projection.jdbc.javadsl.JdbcProjection}.
   */
  @Bean
  public PlatformTransactionManager transactionManager() {
    return new JpaTransactionManager(Objects.requireNonNull(entityManagerFactory().getObject()));
  }

  /** An EntityManager factory using the configured database connection settings. */
  @Bean
  public LocalContainerEntityManagerFactoryBean entityManagerFactory() {

    HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
    vendorAdapter.setGenerateDdl(true);
    vendorAdapter.setDatabase(Database.POSTGRESQL);

    LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
    factory.setJpaVendorAdapter(vendorAdapter);
    factory.setPackagesToScan("shopping.cart");
    // set the DataSource configured with settings in jdbc-connection-settings
    factory.setDataSource(dataSource());
    // load additional properties from config jdbc-connection-settings.additional-properties
    factory.setJpaProperties(additionalProperties());

    return factory;
  }

  /**
   * Returns a {@link DataSource} configured with the settings in {@code
   * jdbc-connection-settings.driver}. See src/main/resources/persistence.conf and
   * src/main/resources/local-shared.conf
   */
  @Bean
  public DataSource dataSource() {

    HikariDataSource dataSource = new HikariDataSource();

    // pool configuration
    dataSource.setPoolName("read-side-connection-pool");
    dataSource.setMaximumPoolSize(
        config.getInt("jdbc-connection-settings.connection-pool.max-pool-size"));

    long timeout =
        config.getDuration(
            "jdbc-connection-settings.connection-pool.timeout", TimeUnit.MILLISECONDS);
    dataSource.setConnectionTimeout(timeout);

    // database configuration
    dataSource.setDriverClassName(config.getString("jdbc-connection-settings.driver"));
    dataSource.setJdbcUrl(config.getString("jdbc-connection-settings.url"));
    dataSource.setUsername(config.getString("jdbc-connection-settings.user"));
    dataSource.setPassword(config.getString("jdbc-connection-settings.password"));
    dataSource.setAutoCommit(false);

    return dataSource;
  }

  /**
   * Additional JPA properties can be passed through config settings under {@code
   * jdbc-connection-settings.additional-properties}. The properties must be defined as key/value
   * pairs of String/String.
   */
  Properties additionalProperties() {
    Properties properties = new Properties();

    Config additionalProperties =
        this.config.getConfig("jdbc-connection-settings.additional-properties");
    Set<Map.Entry<String, ConfigValue>> entries = additionalProperties.entrySet();

    for (Map.Entry<String, ConfigValue> entry : entries) {
      Object value = entry.getValue().unwrapped();
      if (value != null) properties.setProperty(entry.getKey(), value.toString());
    }

    return properties;
  }

  @Bean
  public PersistenceExceptionTranslationPostProcessor exceptionTranslation() {
    return new PersistenceExceptionTranslationPostProcessor();
  }
}
