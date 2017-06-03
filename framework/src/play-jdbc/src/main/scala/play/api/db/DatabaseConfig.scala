/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.db

import play.api.{ Configuration, Environment, Mode }

/**
 * The generic database configuration.
 *
 * @param driver The driver
 * @param url The jdbc URL
 * @param username The username
 * @param password The password
 * @param jndiName The JNDI name
 */
case class DatabaseConfig(driver: Option[String], url: Option[String], username: Option[String], password: Option[String], jndiName: Option[String])

object DatabaseConfig {

  def fromConfig(config: Configuration, environment: Environment) = {

    val driver = config.get[Option[String]]("driver")
    val (url, userPass) = extractUrl(config.get[Option[String]]("url"), environment.mode)
    val username = config.getDeprecated[Option[String]]("username", "user").orElse(userPass.map(_._1))
    val password = config.getDeprecated[Option[String]]("password", "pass").orElse(userPass.map(_._2))
    val jndiName = config.get[Option[String]]("jndiName")

    DatabaseConfig(driver, url, username, password, jndiName)
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
  private def extractUrl(maybeUrl: Option[String], mode: Mode): (Option[String], Option[(String, String)]) = {

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
