/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.db

import com.zaxxer.hikari.HikariConfig
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.Configuration

import scala.concurrent.duration._

class HikariCPConfigSpec extends Specification {
  "When reading configuration" should {
    "set dataSourceClassName when present" in new Configs {
      val config = from("hikaricp.dataSourceClassName" -> "org.postgresql.ds.PGPoolingDataSource")
      new HikariCPConfig(
        DatabaseConfig(None, None, None, None, None),
        config
      ).toHikariConfig.getDataSourceClassName must beEqualTo(
        "org.postgresql.ds.PGPoolingDataSource"
      )
    }

    "set dataSource sub properties" in new Configs {
      val config = from(
        "hikaricp.dataSource.user"     -> "user",
        "hikaricp.dataSource.password" -> "password"
      )
      val hikariConfig: HikariConfig = new HikariCPConfig(dbConfig, config).toHikariConfig

      hikariConfig.getDataSourceProperties.getProperty("user") must beEqualTo("user")
      hikariConfig.getDataSourceProperties.getProperty("password") must beEqualTo("password")
    }

    "set database url" in new Configs {
      new HikariCPConfig(dbConfig, reference).toHikariConfig.getJdbcUrl must beEqualTo("jdbc:h2:mem:")
    }

    "set connectionInitSql config" in new Configs {
      val config = from("hikaricp.connectionInitSql" -> "SELECT 1")
      new HikariCPConfig(dbConfig, config).toHikariConfig.getConnectionInitSql must beEqualTo("SELECT 1")
    }

    "respect the defaults as" in {
      "autoCommit to true" in new Configs {
        new HikariCPConfig(dbConfig, reference).toHikariConfig.isAutoCommit must beTrue
      }

      "connectionTimeout to 30 seconds" in new Configs {
        new HikariCPConfig(dbConfig, reference).toHikariConfig.getConnectionTimeout must beEqualTo(30.seconds.toMillis)
      }

      "idleTimeout to 10 minutes" in new Configs {
        new HikariCPConfig(dbConfig, reference).toHikariConfig.getIdleTimeout must beEqualTo(10.minutes.toMillis)
      }

      "keepaliveTime to 0" in new Configs {
        new HikariCPConfig("foo", dbConfig, reference).toHikariConfig.getKeepaliveTime must beEqualTo(0L)
      }

      "maxLifetime to 30 minutes" in new Configs {
        new HikariCPConfig(dbConfig, reference).toHikariConfig.getMaxLifetime must beEqualTo(30.minutes.toMillis)
      }

      "validationTimeout to 5 seconds" in new Configs {
        new HikariCPConfig(dbConfig, reference).toHikariConfig.getValidationTimeout must beEqualTo(5.seconds.toMillis)
      }

      "minimumIdle to 10" in new Configs {
        new HikariCPConfig(dbConfig, reference).toHikariConfig.getMinimumIdle must beEqualTo(10)
      }

      "maximumPoolSize to 10" in new Configs {
        new HikariCPConfig(dbConfig, reference).toHikariConfig.getMaximumPoolSize must beEqualTo(10)
      }

      "initializationFailTimeout to -1" in new Configs {
        new HikariCPConfig(dbConfig, reference).toHikariConfig.getInitializationFailTimeout must beEqualTo(-1)
      }

      "isolateInternalQueries to false" in new Configs {
        new HikariCPConfig(dbConfig, reference).toHikariConfig.isIsolateInternalQueries must beFalse
      }

      "allowPoolSuspension to false" in new Configs {
        new HikariCPConfig(dbConfig, reference).toHikariConfig.isAllowPoolSuspension must beFalse
      }

      "readOnly to false" in new Configs {
        new HikariCPConfig(dbConfig, reference).toHikariConfig.isReadOnly must beFalse
      }

      "registerMBeans to false" in new Configs {
        new HikariCPConfig(dbConfig, reference).toHikariConfig.isRegisterMbeans must beFalse
      }

      "leakDetectionThreshold to 0 (zero)" in new Configs {
        new HikariCPConfig(dbConfig, reference).toHikariConfig.getLeakDetectionThreshold must beEqualTo(0)
      }
    }

    "override the defaults for property" in {
      "autoCommit" in new Configs {
        val config = from("hikaricp.autoCommit" -> "false")
        new HikariCPConfig(dbConfig, config).toHikariConfig.isAutoCommit must beFalse
      }

      "connectionTimeout" in new Configs {
        val config = from("hikaricp.connectionTimeout" -> "40 seconds")
        new HikariCPConfig(dbConfig, config).toHikariConfig.getConnectionTimeout must beEqualTo(40.seconds.toMillis)
      }

      "idleTimeout" in new Configs {
        val config = from("hikaricp.idleTimeout" -> "5 minutes")
        new HikariCPConfig(dbConfig, config).toHikariConfig.getIdleTimeout must beEqualTo(5.minutes.toMillis)
      }

      "keepaliveTime" in new Configs {
        val config = from("hikaricp.keepaliveTime" -> "5 minutes")
        new HikariCPConfig("foo", dbConfig, config).toHikariConfig.getKeepaliveTime must beEqualTo(5.minutes.toMillis)
      }

      "maxLifetime" in new Configs {
        val config = from("hikaricp.maxLifetime" -> "15 minutes")
        new HikariCPConfig(dbConfig, config).toHikariConfig.getMaxLifetime must beEqualTo(15.minutes.toMillis)
      }

      "validationTimeout" in new Configs {
        val config = from("hikaricp.validationTimeout" -> "10 seconds")
        new HikariCPConfig(dbConfig, config).toHikariConfig.getValidationTimeout must beEqualTo(10.seconds.toMillis)
      }

      "minimumIdle" in new Configs {
        val config = from(
          "hikaricp.minimumIdle"     -> "20",
          "hikaricp.maximumPoolSize" -> "40"
        )
        new HikariCPConfig(dbConfig, config).toHikariConfig.getMinimumIdle must beEqualTo(20)
      }

      "maximumPoolSize" in new Configs {
        val config = from("hikaricp.maximumPoolSize" -> "20")
        new HikariCPConfig(dbConfig, config).toHikariConfig.getMaximumPoolSize must beEqualTo(20)
      }

      "initializationFailTimeout" in new Configs {
        val config = from("hikaricp.initializationFailTimeout" -> "10")
        new HikariCPConfig(dbConfig, config).toHikariConfig.getInitializationFailTimeout must beEqualTo(10)
      }

      "isolateInternalQueries" in new Configs {
        val config = from("hikaricp.isolateInternalQueries" -> "true")
        new HikariCPConfig(dbConfig, config).toHikariConfig.isIsolateInternalQueries must beTrue
      }

      "allowPoolSuspension" in new Configs {
        val config = from("hikaricp.allowPoolSuspension" -> "true")
        new HikariCPConfig(dbConfig, config).toHikariConfig.isAllowPoolSuspension must beTrue
      }

      "readOnly" in new Configs {
        val config = from("hikaricp.readOnly" -> "true")
        new HikariCPConfig(dbConfig, config).toHikariConfig.isReadOnly must beTrue
      }

      "leakDetectionThreshold" in new Configs {
        val config = from("hikaricp.leakDetectionThreshold" -> "3 seconds")
        new HikariCPConfig(dbConfig, config).toHikariConfig.getLeakDetectionThreshold must beEqualTo(3000L)
      }

      "exceptionOverrideClassName" in new Configs {
        val className = classOf[MyTestExceptionOverride].getName

        val config = from("hikaricp.exceptionOverrideClassName" -> className)
        new HikariCPConfig(dbConfig, config).toHikariConfig.getExceptionOverrideClassName must beEqualTo(
          className
        )
      }
    }
  }
}

class MyTestExceptionOverride extends com.zaxxer.hikari.SQLExceptionOverride {}

trait Configs extends Scope {
  val dbConfig                       = DatabaseConfig(Some("org.h2.Driver"), Some("jdbc:h2:mem:"), None, None, None)
  val reference                      = Configuration.reference.get[Configuration]("play.db.prototype")
  def from(props: (String, String)*) = reference ++ Configuration.from(props.toMap)
}
