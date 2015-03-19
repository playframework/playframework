package play.api.db

import java.util.Properties
import com.zaxxer.hikari.HikariConfig
import play.api.Configuration
import com.typesafe.config.ConfigFactory

import org.specs2.specification.Scope
import org.specs2.mutable.Specification

class HikariCPConfigSpec extends Specification {

  "When reading configuration" should {

    "set dataSourceClassName when present" in {
      val properties = new Properties()
      properties.setProperty("dataSourceClassName", "org.postgresql.ds.PGPoolingDataSource")
      properties.setProperty("username", "user")
      val config = new Configuration(ConfigFactory.parseProperties(properties))
      new HikariCPConfig(config).toHikariConfig.getDataSourceClassName must beEqualTo("org.postgresql.ds.PGPoolingDataSource")
    }

    "discard configuration not related to hikari config" in new Configs {
      val props = valid
      props.setProperty("just.some.garbage", "garbage")
      new HikariCPConfig(asConfig(props)).toHikariConfig
      success // won't fail because of garbage property
    }

    "set dataSource sub properties" in new Configs {
      val props = valid
      props.setProperty("dataSource.user", "user")
      props.setProperty("dataSource.password", "password")
      val hikariConfig: HikariConfig = new HikariCPConfig(asConfig(props)).toHikariConfig

      hikariConfig.getDataSourceProperties.getProperty("user") must beEqualTo("user")
      hikariConfig.getDataSourceProperties.getProperty("password") must beEqualTo("password")
    }

    "set database url" in {
      "when jdbcUrl is present" in {
        val properties = new Properties()
        properties.setProperty("jdbcUrl", "jdbc:postgresql://host/database")
        val config = new Configuration(ConfigFactory.parseProperties(properties))
        new HikariCPConfig(config).toHikariConfig.getJdbcUrl must beEqualTo("jdbc:postgresql://host/database")
      }

      "when url present" in {
        val properties = new Properties()
        properties.setProperty("url", "jdbc:postgresql://host/database")
        val config = new Configuration(ConfigFactory.parseProperties(properties))
        new HikariCPConfig(config).toHikariConfig.getJdbcUrl must beEqualTo("jdbc:postgresql://host/database")
      }

      "accept postgres special format" in {
        val properties = new Properties()
        properties.setProperty("url", "postgres://user:password@localhost/database")
        val config = new Configuration(ConfigFactory.parseProperties(properties))
        val hikariConfig = new HikariCPConfig(config).toHikariConfig
        hikariConfig.getJdbcUrl must beEqualTo("jdbc:postgresql://localhost/database")
        hikariConfig.getUsername must beEqualTo("user")
        hikariConfig.getPassword must beEqualTo("password")
      }

      "accept mysql special format" in {
        val properties = new Properties()
        properties.setProperty("url", "mysql://user:password@localhost/database")
        val config = new Configuration(ConfigFactory.parseProperties(properties))
        val hikariConfig = new HikariCPConfig(config).toHikariConfig
        hikariConfig.getJdbcUrl must startWith("jdbc:mysql://localhost/database")
        hikariConfig.getUsername must beEqualTo("user")
        hikariConfig.getPassword must beEqualTo("password")
      }
    }

    "respect the defaults as" in {
      "autoCommit to true" in new Configs {
        new HikariCPConfig(config).toHikariConfig.isAutoCommit must beTrue
      }

      "connectionTimeout to 30 seconds" in new Configs {
        new HikariCPConfig(config).toHikariConfig.getConnectionTimeout must beEqualTo(30.seconds.inMillis)
      }

      "idleTimeout to 10 minutes" in new Configs {
        new HikariCPConfig(config).toHikariConfig.getIdleTimeout must beEqualTo(10.minutes.inMillis)
      }

      "maxLifetime to 30 minutes" in new Configs {
        new HikariCPConfig(config).toHikariConfig.getMaxLifetime must beEqualTo(30.minutes.inMillis)
      }

      "validationTimeout to 5 seconds" in new Configs {
        new HikariCPConfig(config).toHikariConfig.getValidationTimeout must beEqualTo(5.seconds.inMillis)
      }

      "minimumIdle to 10" in new Configs {
        new HikariCPConfig(config).toHikariConfig.getMinimumIdle must beEqualTo(10)
      }

      "maximumPoolSize to 10" in new Configs {
        new HikariCPConfig(config).toHikariConfig.getMaximumPoolSize must beEqualTo(10)
      }

      "initializationFailFast to true" in new Configs {
        new HikariCPConfig(config).toHikariConfig.isInitializationFailFast must beTrue
      }

      "isolateInternalQueries to false" in new Configs {
        new HikariCPConfig(config).toHikariConfig.isIsolateInternalQueries must beFalse
      }

      "allowPoolSuspension to false" in new Configs {
        new HikariCPConfig(config).toHikariConfig.isAllowPoolSuspension must beFalse
      }

      "readOnly to false" in new Configs {
        new HikariCPConfig(config).toHikariConfig.isReadOnly must beFalse
      }

      "registerMBeans to false" in new Configs {
        new HikariCPConfig(config).toHikariConfig.isRegisterMbeans must beFalse
      }

      "leakDetectionThreshold to 0 (zero)" in new Configs {
        new HikariCPConfig(config).toHikariConfig.getLeakDetectionThreshold must beEqualTo(0)
      }
    }

    "override the defaults for property" in {
      "autoCommit" in new Configs {
        val props = valid
        props.setProperty("autoCommit", "false")
        new HikariCPConfig(asConfig(props)).toHikariConfig.isAutoCommit must beFalse
      }

      "connectionTimeout" in new Configs {
        val props = valid
        props.setProperty("connectionTimeout", "40 seconds")
        new HikariCPConfig(asConfig(props)).toHikariConfig.getConnectionTimeout must beEqualTo(40.seconds.inMillis)
      }

      "idleTimeout" in new Configs {
        val props = valid
        props.setProperty("idleTimeout", "5 minutes")
        new HikariCPConfig(asConfig(props)).toHikariConfig.getIdleTimeout must beEqualTo(5.minutes.inMillis)
      }

      "maxLifetime" in new Configs {
        val props = valid
        props.setProperty("maxLifetime", "15 minutes")
        new HikariCPConfig(asConfig(props)).toHikariConfig.getMaxLifetime must beEqualTo(15.minutes.inMillis)
      }

      "validationTimeout" in new Configs {
        val props = valid
        props.setProperty("validationTimeout", "10 seconds")
        new HikariCPConfig(asConfig(props)).toHikariConfig.getValidationTimeout must beEqualTo(10.seconds.inMillis)
      }

      "minimumIdle" in new Configs {
        val props = valid
        props.setProperty("minimumIdle", "20")
        props.setProperty("maximumPoolSize", "40")
        new HikariCPConfig(asConfig(props)).toHikariConfig.getMinimumIdle must beEqualTo(20)
      }

      "maximumPoolSize" in new Configs {
        val props = valid
        props.setProperty("maximumPoolSize", "20")
        new HikariCPConfig(asConfig(props)).toHikariConfig.getMaximumPoolSize must beEqualTo(20)
      }

      "initializationFailFast" in new Configs {
        val props = valid
        props.setProperty("initializationFailFast", "false")
        new HikariCPConfig(asConfig(props)).toHikariConfig.isInitializationFailFast must beFalse
      }

      "isolateInternalQueries" in new Configs {
        val props = valid
        props.setProperty("isolateInternalQueries", "true")
        new HikariCPConfig(asConfig(props)).toHikariConfig.isIsolateInternalQueries must beTrue
      }

      "allowPoolSuspension" in new Configs {
        val props = valid
        props.setProperty("allowPoolSuspension", "true")
        new HikariCPConfig(asConfig(props)).toHikariConfig.isAllowPoolSuspension must beTrue
      }

      "readOnly" in new Configs {
        val props = valid
        props.setProperty("readOnly", "true")
        new HikariCPConfig(asConfig(props)).toHikariConfig.isReadOnly must beTrue
      }

      "leakDetectionThreshold" in new Configs {
        val props = valid
        props.setProperty("leakDetectionThreshold", "3 seconds")
        new HikariCPConfig(asConfig(props)).toHikariConfig.getLeakDetectionThreshold must beEqualTo(3000L)
      }
    }
  }
}

trait Configs extends Scope {
  val config = asConfig(valid)
  val invalid = new Properties()

  def valid = {
    val properties = new Properties()
    properties.setProperty("dataSourceClassName", "org.postgresql.ds.PGPoolingDataSource")
    properties.setProperty("username", "user")
    properties
  }

  def asConfig(props: Properties) = new Configuration(ConfigFactory.parseProperties(props))
}