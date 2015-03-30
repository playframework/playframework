/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.db

import java.sql.{ Connection, Statement }
import javax.inject.{ Inject, Singleton }
import javax.sql.DataSource

import com.typesafe.config.Config
import play.api._
import play.api.inject.Module
import play.api.libs.JNDI

import com.jolbox.bonecp._
import com.jolbox.bonecp.hooks._

import scala.concurrent.duration.{ FiniteDuration, Duration }

/**
 * BoneCP runtime inject module.
 */
class BoneCPModule extends Module {
  def bindings(environment: Environment, configuration: Configuration) = {
    Seq(
      bind[ConnectionPool].to[BoneConnectionPool]
    )
  }
}

/**
 * BoneCP components (for compile-time injection).
 */
trait BoneCPComponents {
  def environment: Environment

  lazy val connectionPool: ConnectionPool = new BoneConnectionPool(environment)
}

/**
 * BoneCP implementation of connection pool interface.
 */
@Singleton
class BoneConnectionPool @Inject() (environment: Environment) extends ConnectionPool {

  import BoneConnectionPool._

  /**
   * Create a data source with the given configuration.
   */
  def create(name: String, dbConfig: DatabaseConfig, conf: Config): DataSource = {

    val config = PlayConfig(conf)

    val datasource = new BoneCPDataSource

    val autocommit = config.getDeprecated[Boolean]("bonecp.autoCommit", "autocommit")
    val isolation = config.getOptionalDeprecated[String]("bonecp.isolation", "isolation").map {
      case "NONE" => Connection.TRANSACTION_NONE
      case "READ_COMMITTED" => Connection.TRANSACTION_READ_COMMITTED
      case "READ_UNCOMMITTED" => Connection.TRANSACTION_READ_UNCOMMITTED
      case "REPEATABLE_READ" => Connection.TRANSACTION_REPEATABLE_READ
      case "SERIALIZABLE" => Connection.TRANSACTION_SERIALIZABLE
      case unknown => throw config.reportError("bonecp.isolation",
        s"Unknown isolation level [$unknown]")
    }
    val catalog = config.getOptionalDeprecated[String]("defaultCatalog", "bonecp.defaultCatalog")
    val readOnly = config.getDeprecated[Boolean]("bonecp.readOnly", "readOnly")

    datasource.setClassLoader(environment.classLoader)

    // Re-apply per connection config @ checkout
    datasource.setConnectionHook(new AbstractConnectionHook {

      override def onCheckIn(connection: ConnectionHandle) {
        if (logger.isTraceEnabled) {
          logger.trace(s"Check in connection $connection [${datasource.getTotalLeased} leased]")
        }
      }

      override def onCheckOut(connection: ConnectionHandle) {
        connection.setAutoCommit(autocommit)
        isolation.foreach(connection.setTransactionIsolation)
        connection.setReadOnly(readOnly)
        catalog.foreach(connection.setCatalog)
        if (logger.isTraceEnabled) {
          logger.trace(s"Check out connection $connection [${datasource.getTotalLeased} leased]")
        }
      }

      override def onQueryExecuteTimeLimitExceeded(handle: ConnectionHandle, statement: Statement, sql: String, logParams: java.util.Map[AnyRef, AnyRef], timeElapsedInNs: Long) {
        val timeMs = timeElapsedInNs / 1000
        val query = PoolUtil.fillLogParams(sql, logParams)
        logger.warn(s"Query execute time limit exceeded (${timeMs}ms) - query: ${query}")
      }

    })

    dbConfig.url.foreach(datasource.setJdbcUrl)
    dbConfig.username.foreach(datasource.setUsername)
    dbConfig.password.foreach(datasource.setPassword)

    // Pool configuration
    datasource.setCloseOpenStatements(config.getDeprecated[Boolean]("bonecp.closeOpenStatements", "closeOpenStatements"))
    datasource.setPartitionCount(config.getDeprecated[Int]("bonecp.partitionCount", "partitionCount"))
    datasource.setMaxConnectionsPerPartition(config.getDeprecated[Int]("bonecp.maxConnectionsPerPartition", "maxConnectionsPerPartition"))
    datasource.setMinConnectionsPerPartition(config.getDeprecated[Int]("bonecp.minConnectionsPerPartition", "minConnectionsPerPartition"))
    datasource.setAcquireIncrement(config.getDeprecated[Int]("bonecp.acquireIncrement", "acquireIncrement"))
    datasource.setAcquireRetryAttempts(config.getDeprecated[Int]("bonecp.acquireRetryAttempts", "acquireRetryAttempts"))
    datasource.setAcquireRetryDelayInMs(config.getDeprecated[FiniteDuration]("bonecp.acquireRetryDelay", "acquireRetryDelay").toMillis)
    datasource.setConnectionTimeoutInMs(config.getDeprecated[FiniteDuration]("bonecp.connectionTimeout", "connectionTimeout").toMillis)
    datasource.setIdleMaxAgeInSeconds(config.getDeprecated[FiniteDuration]("bonecp.idleMaxAge", "idleMaxAge").toSeconds)
    datasource.setMaxConnectionAgeInSeconds(config.getDeprecated[FiniteDuration]("bonecp.maxConnectionAge", "maxConnectionAge").toSeconds)
    datasource.setDisableJMX(config.getDeprecated[Boolean]("bonecp.disableJMX", "disableJMX"))
    datasource.setStatisticsEnabled(config.getDeprecated[Boolean]("bonecp.statisticsEnabled", "statisticsEnabled"))
    datasource.setIdleConnectionTestPeriodInSeconds(config.getDeprecated[FiniteDuration]("bonecp.idleConnectionTestPeriod", "idleConnectionTestPeriod").toSeconds)
    datasource.setDisableConnectionTracking(config.getDeprecated[Boolean]("bonecp.disableConnectionTracking", "disableConnectionTracking"))
    datasource.setQueryExecuteTimeLimitInMs(config.getDeprecated[FiniteDuration]("bonecp.queryExecuteTimeLimit", "queryExecuteTimeLimit").toMillis)
    datasource.setResetConnectionOnClose(config.getDeprecated[Boolean]("bonecp.resetConnectionOnClose", "resetConnectionOnClose"))
    datasource.setDetectUnresolvedTransactions(config.getDeprecated[Boolean]("bonecp.detectUnresolvedTransactions", "detectUnresolvedTransactions"))
    datasource.setLogStatementsEnabled(config.getDeprecated[Boolean]("bonecp.logStatements", "logStatements"))

    config.getOptionalDeprecated[String]("bonecp.initSQL", "initSQL").foreach(datasource.setInitSQL)
    config.getOptionalDeprecated[String]("bonecp.connectionTestStatement", "connectionTestStatement").foreach(datasource.setConnectionTestStatement)

    // Bind in JNDI
    dbConfig.jndiName foreach { name =>
      JNDI.initialContext.rebind(name, datasource)
      val visibleUrl = datasource.getJdbcUrl
      logger.info(s"""datasource [$visibleUrl] bound to JNDI as $name""")
    }

    datasource
  }

  /**
   * Close the given data source.
   */
  def close(ds: DataSource): Unit = ds match {
    case bcp: BoneCPDataSource => bcp.close()
    case _ => sys.error("Unable to close data source: not a BoneCPDataSource")
  }

}

object BoneConnectionPool {
  private val logger = Logger(classOf[BoneConnectionPool])
}
