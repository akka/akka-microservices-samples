package shopping.cart.repository;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories
@EnableTransactionManagement
public class SpringConfig {

  private final Config config;

  public SpringConfig(Config config) {
    this.config = config;
  }

  @Bean
  public LocalContainerEntityManagerFactoryBean entityManagerFactory() {

    HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
    vendorAdapter.setGenerateDdl(true);
    vendorAdapter.setDatabase(Database.POSTGRESQL);

    LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
    factory.setJpaVendorAdapter(vendorAdapter);
    factory.setPackagesToScan("shopping.cart");

    factory.setDataSource(dataSource());
    factory.setJpaProperties(additionalProperties());

    return factory;
  }

  @Bean
  public DataSource dataSource() {
    // FIXME: this needs to be wrapped in a connection pool
    DriverManagerDataSource dataSource = new DriverManagerDataSource();
    dataSource.setDriverClassName(config.getString("jdbc-connection-settings.driver"));
    dataSource.setUrl(config.getString("jdbc-connection-settings.url"));
    dataSource.setUsername(config.getString("jdbc-connection-settings.user"));
    dataSource.setPassword(config.getString("jdbc-connection-settings.password"));
    return dataSource;
  }

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
  public PlatformTransactionManager transactionManager() {
    return new JpaTransactionManager(Objects.requireNonNull(entityManagerFactory().getObject()));
  }

  @Bean
  public PersistenceExceptionTranslationPostProcessor exceptionTranslation() {
    return new PersistenceExceptionTranslationPostProcessor();
  }
}
