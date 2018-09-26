/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.db

import javax.inject.{ Inject, Singleton }
import javax.sql.DataSource

import com.typesafe.config.Config
import com.zaxxer.hikari.{ HikariConfig, HikariDataSource }
import play.api._
import play.api.inject._
import play.api.libs.JNDI

import scala.concurrent.duration.{ Duration, FiniteDuration }
import scala.util.{ Failure, Success, Try }

/**
 * HikariCP runtime inject module.
 */
class HikariCPModule extends SimpleModule(bind[ConnectionPool].to[HikariCPConnectionPool])

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
    val config = Configuration(configuration)

    Try {
      Logger.info(s"Creating Pool for datasource '$name'")

      val hikariConfig = new HikariCPConfig(dbConfig, config).toHikariConfig
      val datasource = new HikariDataSource(hikariConfig)
      val wrappedDataSource = ConnectionPool.wrapToLogSql(datasource, configuration)

      // Bind in JNDI
      dbConfig.jndiName.foreach { jndiName =>
        JNDI.initialContext.rebind(jndiName, wrappedDataSource)
        logger.info(s"datasource [$name] bound to JNDI as $jndiName")
      }

      wrappedDataSource
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
    ConnectionPool.unwrap(dataSource) match {
      case ds: HikariDataSource => ds.close()
      case _ => sys.error("Unable to close data source: not a HikariDataSource")
    }
  }
}

/**
 * HikariCP config
 */
private[db] class HikariCPConfig(dbConfig: DatabaseConfig, configuration: Configuration) {

  def toHikariConfig: HikariConfig = {
    val hikariConfig = new HikariConfig()

    val config = configuration.get[Configuration]("hikaricp")

    // Essentials configurations
    config.get[Option[String]]("dataSourceClassName").foreach(hikariConfig.setDataSourceClassName)

    dbConfig.url.foreach(hikariConfig.setJdbcUrl)
    dbConfig.driver.foreach(hikariConfig.setDriverClassName)

    dbConfig.username.foreach(hikariConfig.setUsername)
    dbConfig.password.foreach(hikariConfig.setPassword)

    import scala.collection.JavaConverters._

    val dataSourceConfig = config.get[Configuration]("dataSource")
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
    config.get[Option[String]]("connectionTestQuery").foreach(hikariConfig.setConnectionTestQuery)
    config.get[Option[Int]]("minimumIdle").foreach(hikariConfig.setMinimumIdle)
    hikariConfig.setMaximumPoolSize(config.get[Int]("maximumPoolSize"))
    config.get[Option[String]]("poolName").foreach(hikariConfig.setPoolName)

    // Infrequently used
    hikariConfig.setInitializationFailTimeout(config.get[Long]("initializationFailTimeout"))
    hikariConfig.setIsolateInternalQueries(config.get[Boolean]("isolateInternalQueries"))
    hikariConfig.setAllowPoolSuspension(config.get[Boolean]("allowPoolSuspension"))
    hikariConfig.setReadOnly(config.get[Boolean]("readOnly"))
    hikariConfig.setRegisterMbeans(config.get[Boolean]("registerMbeans"))
    config.get[Option[String]]("connectionInitSql").foreach(hikariConfig.setConnectionInitSql)
    config.get[Option[String]]("catalog").foreach(hikariConfig.setCatalog)
    config.get[Option[String]]("transactionIsolation").foreach(hikariConfig.setTransactionIsolation)
    hikariConfig.setValidationTimeout(config.get[FiniteDuration]("validationTimeout").toMillis)
    hikariConfig.setLeakDetectionThreshold(toMillis(config.get[Duration]("leakDetectionThreshold")))

    hikariConfig.validate()
    hikariConfig
  }
}

object HikariCPConnectionPool {
  private val logger = Logger(classOf[HikariCPConnectionPool])
}
