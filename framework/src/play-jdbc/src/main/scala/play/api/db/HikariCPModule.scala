/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.db

import javax.sql.DataSource

import play.api.libs.JNDI
import play.api.inject.Module
import play.api._

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
  lazy val connectionPool: ConnectionPool = new HikariCPConnectionPool
}

class HikariCPConnectionPool extends ConnectionPool {

  import HikariCPConnectionPool._

  /**
   * Create a data source with the given configuration.
   *
   * @param name the database name
   * @param configuration the data source configuration
   * @param classLoader the database class loader
   * @return a data source backed by a connection pool
   */
  override def create(name: String, configuration: Configuration, classLoader: ClassLoader): DataSource = {
    Try {
      Logger.info(s"Creating Pool for datasource '$name'")

      val hikariConfig = new HikariCPConfig(configuration).toHikariConfig
      val datasource = new HikariDataSource(hikariConfig)

      // Bind in JNDI
      configuration.getString("jndiName").map { name =>
        JNDI.initialContext.rebind(name, datasource)
        val visibleUrl = datasource.getJdbcUrl
        logger.info(s"""datasource [$visibleUrl] bound to JNDI as $name""")
      }

      datasource
    } match {
      case Success(datasource) => datasource
      case Failure(ex) => throw configuration.reportError(name, ex.getMessage, Some(ex))
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
class HikariCPConfig(config: Configuration) {

  def toHikariConfig: HikariConfig = {
    val hikariConfig = new HikariConfig()

    // Essentials configurations
    config.getString("dataSourceClassName") match {
      case Some(className) => hikariConfig.setDataSourceClassName(className)
      case None => Logger.debug("`dataSourceClassName` not present. Will use `jdbcUrl` or `url` instead.")
    }

    // "driver" and "url" are used in a lot of places, so I decide
    // to maintain both in order to simplify the migration to HikariCP.
    (config.getString("jdbcUrl") ++ config.getString("url")).foreach(url => configureUrl(Some(url), hikariConfig))
    (config.getString("driverClassName") ++ config.getString("driver")).foreach(hikariConfig.setDriverClassName)

    config.getString("username").foreach(hikariConfig.setUsername)
    config.getString("password").foreach(hikariConfig.setPassword)

    config.getConfig("dataSource").foreach { dataSourceConfig =>
      dataSourceConfig.keys.foreach { key =>
        hikariConfig.addDataSourceProperty(key, dataSourceConfig.getString(key).get)
      }
    }

    // Frequently used
    config.getBoolean("autoCommit").foreach(hikariConfig.setAutoCommit)
    config.getMilliseconds("connectionTimeout").foreach(hikariConfig.setConnectionTimeout)
    config.getMilliseconds("idleTimeout").foreach(hikariConfig.setIdleTimeout)
    config.getMilliseconds("maxLifetime").foreach(hikariConfig.setMaxLifetime)
    config.getString("connectionTestQuery").foreach(hikariConfig.setConnectionTestQuery)
    config.getInt("minimumIdle").foreach(hikariConfig.setMinimumIdle)
    config.getInt("maximumPoolSize").foreach(hikariConfig.setMaximumPoolSize)
    config.getString("poolName").foreach(hikariConfig.setPoolName)

    // Infrequently used
    config.getBoolean("initializationFailFast").foreach(hikariConfig.setInitializationFailFast)
    config.getBoolean("isolateInternalQueries").foreach(hikariConfig.setIsolateInternalQueries)
    config.getBoolean("allowPoolSuspension").foreach(hikariConfig.setAllowPoolSuspension)
    config.getBoolean("readOnly").foreach(hikariConfig.setReadOnly)
    config.getBoolean("registerMbeans").foreach(hikariConfig.setRegisterMbeans)
    config.getString("catalog").foreach(hikariConfig.setCatalog)
    config.getString("connectionInitSql").foreach(hikariConfig.setConnectionInitSql)
    config.getString("transactionIsolation").foreach(hikariConfig.setTransactionIsolation)
    config.getMilliseconds("validationTimeout").foreach(hikariConfig.setValidationTimeout)
    config.getMilliseconds("leakDetectionThreshold").foreach(hikariConfig.setLeakDetectionThreshold)

    hikariConfig.validate()
    hikariConfig
  }

  private def configureUrl(databaseUrl: Option[String], hikariConfig: HikariConfig) = {
    val PostgresFullUrl = "^postgres://([a-zA-Z0-9_]+):([^@]+)@([^/]+)/([^\\s]+)$".r
    val MysqlFullUrl = "^mysql://([a-zA-Z0-9_]+):([^@]+)@([^/]+)/([^\\s]+)$".r
    val MysqlCustomProperties = ".*\\?(.*)".r
    val H2DefaultUrl = "^jdbc:h2:mem:.+".r

    databaseUrl match {
      case Some(PostgresFullUrl(username, password, host, dbname)) =>
        hikariConfig.setJdbcUrl(s"jdbc:postgresql://$host/$dbname")
        hikariConfig.setUsername(username)
        hikariConfig.setPassword(password)

      case Some(url @ MysqlFullUrl(username, password, host, dbname)) =>
        val defaultProperties = "?useUnicode=yes&characterEncoding=UTF-8&connectionCollation=utf8_general_ci"
        val addDefaultPropertiesIfNeeded = MysqlCustomProperties.findFirstMatchIn(url).map(_ => "").getOrElse(defaultProperties)
        hikariConfig.setJdbcUrl(s"jdbc:mysql://$host/${dbname + addDefaultPropertiesIfNeeded}")
        hikariConfig.setUsername(username)
        hikariConfig.setPassword(password)

      // TODO: remove use of Play.maybeApplication
      case Some(url @ H2DefaultUrl()) if !url.contains("DB_CLOSE_DELAY") =>
        if (Play.maybeApplication.exists(_.mode == Mode.Dev)) {
          hikariConfig.setJdbcUrl(s"$url;DB_CLOSE_DELAY=-1")
        } else hikariConfig.setJdbcUrl(url)

      case Some(s: String) => hikariConfig.setJdbcUrl(s)

      case _ =>
    }
  }
}

object HikariCPConnectionPool {
  private val logger = Logger(classOf[HikariCPConnectionPool])
}
