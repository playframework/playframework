/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.db

import javax.inject.{ Inject, Singleton }
import javax.sql.DataSource

import com.typesafe.config.Config
import play.api.libs.JNDI
import play.api.inject.Module
import play.api._

import scala.concurrent.duration.{ FiniteDuration, Duration }
import scala.util.{ Success, Try, Failure }

import com.zaxxer.hikari.{ HikariDataSource, HikariConfig }

/**
 * HikariCP runtime inject module.
 */
class HikariCPModule extends Module {
  def bindings(environment: Environment, configuration: Configuration) = {
    Seq(
      bind[ConnectionPool].to[HikariCPConnectionPool]
    )
  }
}

/**
 * HikariCP components (for compile-time injection).
 */
trait HikariCPComponents {
  def environment: Environment

  lazy val connectionPool: ConnectionPool = new HikariCPConnectionPool(environment)
}

@Singleton
class HikariCPConnectionPool @Inject() (environment: Environment) extends ConnectionPool {

  import HikariCPConnectionPool._

  /**
   * Create a data source with the given configuration.
   *
   * @param name the database name
   * @param configuration the data source configuration
   * @return a data source backed by a connection pool
   */
  override def create(name: String, dbConfig: DatabaseConfig, configuration: Config): DataSource = {
    val config = PlayConfig(configuration)

    Try {
      Logger.info(s"Creating Pool for datasource '$name'")

      val hikariConfig = new HikariCPConfig(dbConfig, config).toHikariConfig
      val datasource = new HikariDataSource(hikariConfig)

      // Bind in JNDI
      dbConfig.jndiName.foreach { name =>
        JNDI.initialContext.rebind(name, datasource)
        val visibleUrl = datasource.getJdbcUrl
        logger.info(s"""datasource [$visibleUrl] bound to JNDI as $name""")
      }

      datasource
    } match {
      case Success(datasource) => datasource
      case Failure(ex) => throw config.reportError(name, ex.getMessage, Some(ex))
    }
  }

  /**
   * Close the given data source.
   *
   * @param dataSource the data source to close
   */
  override def close(dataSource: DataSource) = {
    Logger.info("Shutting down connection pool.")
    dataSource match {
      case ds: HikariDataSource => ds.shutdown()
      case _ => sys.error("Unable to close data source: not a HikariDataSource")
    }
  }
}

/**
 * HikariCP config
 */
class HikariCPConfig(dbConfig: DatabaseConfig, configuration: PlayConfig) {

  def toHikariConfig: HikariConfig = {
    val hikariConfig = new HikariConfig()

    val config = configuration.get[PlayConfig]("hikaricp")

    // Essentials configurations
    config.getOptional[String]("dataSourceClassName") match {
      case Some(className) => hikariConfig.setDataSourceClassName(className)
      case None => Logger.debug("`dataSourceClassName` not present. Will use `url` instead.")
    }

    dbConfig.url.foreach(hikariConfig.setJdbcUrl)
    dbConfig.driver.foreach(hikariConfig.setDriverClassName)

    dbConfig.username.foreach(hikariConfig.setUsername)
    dbConfig.password.foreach(hikariConfig.setPassword)

    import scala.collection.JavaConverters._

    val dataSourceConfig = config.get[PlayConfig]("dataSource")
    dataSourceConfig.underlying.root().keySet().asScala.foreach { key =>
      hikariConfig.addDataSourceProperty(key, dataSourceConfig.get[String](key))
    }

    def toMillis(duration: Duration) = {
      if (duration.isFinite()) duration.toMillis
      else 0l
    }

    // Frequently used
    hikariConfig.setAutoCommit(config.get[Boolean]("autoCommit"))
    hikariConfig.setConnectionTimeout(toMillis(config.get[Duration]("connectionTimeout")))
    hikariConfig.setIdleTimeout(toMillis(config.get[Duration]("idleTimeout")))
    hikariConfig.setMaxLifetime(toMillis(config.get[Duration]("maxLifetime")))
    config.getOptional[String]("connectionTestQuery").foreach(hikariConfig.setConnectionTestQuery)
    config.getOptional[Int]("minimumIdle").foreach(hikariConfig.setMinimumIdle)
    hikariConfig.setMaximumPoolSize(config.get[Int]("maximumPoolSize"))
    config.getOptional[String]("poolName").foreach(hikariConfig.setPoolName)

    // Infrequently used
    hikariConfig.setInitializationFailFast(config.get[Boolean]("initializationFailFast"))
    hikariConfig.setIsolateInternalQueries(config.get[Boolean]("isolateInternalQueries"))
    hikariConfig.setAllowPoolSuspension(config.get[Boolean]("allowPoolSuspension"))
    hikariConfig.setReadOnly(config.get[Boolean]("readOnly"))
    hikariConfig.setRegisterMbeans(config.get[Boolean]("registerMbeans"))
    config.getOptional[String]("catalog").foreach(hikariConfig.setCatalog)
    config.getOptional[String]("transactionIsolation").foreach(hikariConfig.setTransactionIsolation)
    hikariConfig.setValidationTimeout(config.get[FiniteDuration]("validationTimeout").toMillis)
    hikariConfig.setLeakDetectionThreshold(toMillis(config.get[Duration]("leakDetectionThreshold")))

    hikariConfig.validate()
    hikariConfig
  }
}

object HikariCPConnectionPool {
  private val logger = Logger(classOf[HikariCPConnectionPool])
}
