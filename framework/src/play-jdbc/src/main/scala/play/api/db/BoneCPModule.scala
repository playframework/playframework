/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.db

import java.sql.{ Connection, Statement }
import javax.sql.DataSource

import play.api.{ Configuration, Environment, Logger, Mode, Play }
import play.api.inject.Module
import play.api.libs.JNDI

import com.jolbox.bonecp._
import com.jolbox.bonecp.hooks._

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
  lazy val connectionPool: ConnectionPool = new BoneConnectionPool
}

/**
 * BoneCP implementation of connection pool interface.
 */
class BoneConnectionPool extends ConnectionPool {

  import BoneConnectionPool._

  /**
   * Create a data source with the given configuration.
   */
  def create(name: String, conf: Configuration, classLoader: ClassLoader): DataSource = {

    val datasource = new BoneCPDataSource

    val autocommit = conf.getBoolean("autocommit").getOrElse(true)
    val isolation = conf.getString("isolation").map {
      case "NONE" => Connection.TRANSACTION_NONE
      case "READ_COMMITTED" => Connection.TRANSACTION_READ_COMMITTED
      case "READ_UNCOMMITTED" => Connection.TRANSACTION_READ_UNCOMMITTED
      case "REPEATABLE_READ" => Connection.TRANSACTION_REPEATABLE_READ
      case "SERIALIZABLE" => Connection.TRANSACTION_SERIALIZABLE
      case unknown => throw conf.reportError("isolation",
        s"Unknown isolation level [$unknown]")
    }
    val catalog = conf.getString("defaultCatalog")
    val readOnly = conf.getBoolean("readOnly").getOrElse(false)

    datasource.setClassLoader(classLoader)

    // Re-apply per connection config @ checkout
    datasource.setConnectionHook(new AbstractConnectionHook {

      override def onCheckIn(connection: ConnectionHandle) {
        if (logger.isTraceEnabled) {
          logger.trace(s"Check in connection $connection [${datasource.getTotalLeased} leased]")
        }
      }

      override def onCheckOut(connection: ConnectionHandle) {
        connection.setAutoCommit(autocommit)
        isolation.map(connection.setTransactionIsolation)
        connection.setReadOnly(readOnly)
        catalog.map(connection.setCatalog)
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

    val PostgresFullUrl = "^postgres://([a-zA-Z0-9_]+):([^@]+)@([^/]+)/([^\\s]+)$".r
    val MysqlFullUrl = "^mysql://([a-zA-Z0-9_]+):([^@]+)@([^/]+)/([^\\s]+)$".r
    val MysqlCustomProperties = ".*\\?(.*)".r
    val H2DefaultUrl = "^jdbc:h2:mem:.+".r

    conf.getString("url") match {
      case Some(PostgresFullUrl(username, password, host, dbname)) =>
        datasource.setJdbcUrl(s"jdbc:postgresql://$host/$dbname")
        datasource.setUsername(username)
        datasource.setPassword(password)

      case Some(url @ MysqlFullUrl(username, password, host, dbname)) =>
        val defaultProperties = "?useUnicode=yes&characterEncoding=UTF-8&connectionCollation=utf8_general_ci"
        val addDefaultPropertiesIfNeeded = MysqlCustomProperties.findFirstMatchIn(url).map(_ => "").getOrElse(defaultProperties)
        datasource.setJdbcUrl(s"jdbc:mysql://$host/${dbname + addDefaultPropertiesIfNeeded}")
        datasource.setUsername(username)
        datasource.setPassword(password)

      // TODO: remove use of Play.maybeApplication
      case Some(url @ H2DefaultUrl()) if !url.contains("DB_CLOSE_DELAY") =>
        if (Play.maybeApplication.exists(_.mode == Mode.Dev)) {
          datasource.setJdbcUrl(s"$url;DB_CLOSE_DELAY=-1")
        } else datasource.setJdbcUrl(url)

      case Some(s: String) => datasource.setJdbcUrl(s)

      case _ => throw conf.globalError(s"Missing url configuration for database $name: $conf")
    }

    conf.getString("user").map(datasource.setUsername)
    conf.getString("pass").map(datasource.setPassword)
    conf.getString("password").map(datasource.setPassword)

    // Pool configuration
    datasource.setCloseOpenStatements(conf.getBoolean("closeOpenStatements").getOrElse(true))
    datasource.setPartitionCount(conf.getInt("partitionCount").getOrElse(1))
    datasource.setMaxConnectionsPerPartition(conf.getInt("maxConnectionsPerPartition").getOrElse(30))
    datasource.setMinConnectionsPerPartition(conf.getInt("minConnectionsPerPartition").getOrElse(5))
    datasource.setAcquireIncrement(conf.getInt("acquireIncrement").getOrElse(1))
    datasource.setAcquireRetryAttempts(conf.getInt("acquireRetryAttempts").getOrElse(10))
    datasource.setAcquireRetryDelayInMs(conf.getMilliseconds("acquireRetryDelay").getOrElse(1000))
    datasource.setConnectionTimeoutInMs(conf.getMilliseconds("connectionTimeout").getOrElse(1000))
    datasource.setIdleMaxAge(conf.getMilliseconds("idleMaxAge").getOrElse(1000 * 60 * 10), java.util.concurrent.TimeUnit.MILLISECONDS)
    datasource.setMaxConnectionAge(conf.getMilliseconds("maxConnectionAge").getOrElse(1000 * 60 * 60), java.util.concurrent.TimeUnit.MILLISECONDS)
    datasource.setDisableJMX(conf.getBoolean("disableJMX").getOrElse(true))
    datasource.setStatisticsEnabled(conf.getBoolean("statisticsEnabled").getOrElse(false))
    datasource.setIdleConnectionTestPeriod(conf.getMilliseconds("idleConnectionTestPeriod").getOrElse(1000 * 60), java.util.concurrent.TimeUnit.MILLISECONDS)
    datasource.setDisableConnectionTracking(conf.getBoolean("disableConnectionTracking").getOrElse(true))
    datasource.setQueryExecuteTimeLimitInMs(conf.getMilliseconds("queryExecuteTimeLimit").getOrElse(0))
    datasource.setResetConnectionOnClose(conf.getBoolean("resetConnectionOnClose").getOrElse(false))
    datasource.setDetectUnresolvedTransactions(conf.getBoolean("detectUnresolvedTransactions").getOrElse(false))

    conf.getString("initSQL").map(datasource.setInitSQL)
    conf.getBoolean("logStatements").map(datasource.setLogStatementsEnabled)
    conf.getString("connectionTestStatement").map(datasource.setConnectionTestStatement)

    // Bind in JNDI
    conf.getString("jndiName") map { name =>
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
