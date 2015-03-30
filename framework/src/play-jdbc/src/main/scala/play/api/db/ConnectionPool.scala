/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.db

import javax.sql.DataSource

import com.typesafe.config.Config
import play.api.{ Environment, Mode }
import play.api.inject.Injector
import play.utils.Reflect

/**
 * Connection pool API for managing data sources.
 */
trait ConnectionPool {

  /**
   * Create a data source with the given configuration.
   *
   * @param name the database name
   * @param configuration the data source configuration
   * @return a data source backed by a connection pool
   */
  def create(name: String, dbConfig: DatabaseConfig, configuration: Config): DataSource

  /**
   * Close the given data source.
   *
   * @param dataSource the data source to close
   */
  def close(dataSource: DataSource): Unit

}

object ConnectionPool {

  /**
   * Load a connection pool from a configured connection pool
   */
  def fromConfig(config: String, injector: Injector, environment: Environment, default: ConnectionPool): ConnectionPool = {
    config match {
      case "default" => default
      case "bonecp" => new BoneConnectionPool(environment)
      case "hikaricp" => new HikariCPConnectionPool(environment)
      case fqcn => injector.instanceOf(Reflect.getClass[ConnectionPool](fqcn, environment.classLoader))
    }
  }

  private val PostgresFullUrl = "^postgres://([a-zA-Z0-9_]+):([^@]+)@([^/]+)/([^\\s]+)$".r
  private val MysqlFullUrl = "^mysql://([a-zA-Z0-9_]+):([^@]+)@([^/]+)/([^\\s]+)$".r
  private val MysqlCustomProperties = ".*\\?(.*)".r
  private val H2DefaultUrl = "^jdbc:h2:mem:.+".r

  /**
   * Extract the given URL.
   *
   * Supports shortcut URLs for postgres and mysql, and also adds various default parameters as appropriate.
   */
  def extractUrl(maybeUrl: Option[String], mode: Mode.Mode): (Option[String], Option[(String, String)]) = {

    maybeUrl match {
      case Some(PostgresFullUrl(username, password, host, dbname)) =>
        Some(s"jdbc:postgresql://$host/$dbname") -> Some(username -> password)

      case Some(url @ MysqlFullUrl(username, password, host, dbname)) =>
        val defaultProperties = "?useUnicode=yes&characterEncoding=UTF-8&connectionCollation=utf8_general_ci"
        val addDefaultPropertiesIfNeeded = MysqlCustomProperties.findFirstMatchIn(url).map(_ => "").getOrElse(defaultProperties)
        Some(s"jdbc:mysql://$host/${dbname + addDefaultPropertiesIfNeeded}") -> Some(username -> password)

      case Some(url @ H2DefaultUrl()) if !url.contains("DB_CLOSE_DELAY") && mode == Mode.Dev =>
        Some(s"$url;DB_CLOSE_DELAY=-1") -> None

      case Some(url) =>
        Some(url) -> None
      case None =>
        None -> None
    }

  }
}