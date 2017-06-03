/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.db

import java.sql.{ Connection, SQLTransientConnectionException }
import javax.inject.{ Inject, Singleton }
import javax.sql.DataSource

import com.typesafe.config.Config
import com.zaxxer.hikari.pool.HikariPool
import com.zaxxer.hikari.{ HikariConfig, HikariDataSource }
import play.api._
import play.api.db.internal.SqlLogging
import play.api.inject._
import play.api.libs.JNDI

import scala.concurrent.Future
import scala.concurrent.duration.{ Duration, FiniteDuration }

/**
 * HikariCP runtime inject module.
 */
class HikariCPModule extends SimpleModule(bind[AsyncConnectionPool].to[HikariCPConnectionPool])

/**
 * HikariCP components (for compile-time injection).
 */
trait HikariCPComponents {
  def environment: Environment

  lazy val connectionPool: AsyncConnectionPool = new HikariCPConnectionPool(environment)
}

@Singleton
class HikariCPConnectionPool @Inject() (environment: Environment) extends AsyncConnectionPool {

  import HikariCPConnectionPool._

  /**
   * Create a data source with the given configuration.
   *
   * @param name the database name
   * @param configuration the data source configuration
   * @return a data source backed by a c pool
   */
  override def createAsync(name: String, dbConfig: DatabaseConfig, configuration: Config): AsyncDataSource = {
    val config = Configuration(configuration)

    try {

      Logger.info(s"Creating Pool for datasource '$name'")
      // Get the
      val hikariConfig = new HikariCPConfig(dbConfig, config).toHikariConfig
      val hikariDataSource = new HikariDataSource(hikariConfig)

      val loggingEnabled = SqlLogging.isSqlLoggingConfigured(configuration)
      def wrapConnectionIfLogging(c: Connection): Connection = {
        if (loggingEnabled) SqlLogging.wrapConnectionForLogging(c) else c
      }

      // If configured, wrap with a DataSource that does logging
      val loggingDataSource: DataSource = if (loggingEnabled) {
        SqlLogging.wrapDataSourceForLogging(hikariDataSource)
      } else hikariDataSource

      // Get the HikariPool object associated with the connection.
      // We need access to this class so that we can query the pool
      // directly for a connection.
      val pool: HikariPool = {
        val poolField = classOf[HikariDataSource].getDeclaredField("pool")
        poolField.setAccessible(true)
        poolField.get(hikariDataSource).asInstanceOf[HikariPool]
      }

      val asyncDataSource: AsyncDataSource = new AsyncDataSource.Wrapper(loggingDataSource, hikariDataSource) {
        override def getConnectionAsync: Future[Connection] = {
          try {
            // Try to get the connection immediately from the the pool.
            // If we can't then Hikari will throw a special exception and
            // we can ask the AsyncDataSourceService to get the connection
            // on our behalf and give it to us when it's ready.
            val c: Connection = pool.getConnection(0L)
            // Since we're bypassing the DataSource we should wrap the
            // connection ourselves. (Usually the DataSource would do it
            // for us.)
            val wrapped: Connection = wrapConnectionIfLogging(c)
            Future.successful(wrapped)
          } catch {
            case _: SQLTransientConnectionException =>
              // The HikariPool throws this type of exception to indicate
              // that a timeout occurred while waiting for a connection.
              // Since we only waited for 0ms this is a common thing to happen.
              // Since there's no connection immediately available so now we
              // fall back to our default implementation, i.e. we ask the service
              // to get it for us on another thread and return the connection to
              // us via a future.
              //
              // Note: There is a chance that this exception was thrown for
              // another reason, e.g. by the database driver. However it is safe
              // to retry since the exception should only be thrown for transient
              // errors and retrying is explicitly permitted in the exception
              // class description.
              super.getConnectionAsync
          }
        }
      }

      // Bind in JNDI
      dbConfig.jndiName.foreach { jndiName =>
        JNDI.initialContext.rebind(jndiName, asyncDataSource)
        logger.info(s"datasource [$name] bound to JNDI as $jndiName")
      }

      asyncDataSource

    } catch {
      case ex: Exception => throw config.reportError(name, ex.getMessage, Some(ex))
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
    // Use getOptional here so the runtime warning is only triggered if it is not null
    config.getOptional[Boolean]("initializationFailFast").foreach(hikariConfig.setInitializationFailFast)
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
