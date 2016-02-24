/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.db

import play.api.{ Environment, PlayConfig }

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

  def fromConfig(config: PlayConfig, environment: Environment) = {

    val driver = config.get[Option[String]]("driver")
    val (url, userPass) = ConnectionPool.extractUrl(config.get[Option[String]]("url"), environment.mode)
    val username = config.getDeprecated[Option[String]]("username", "user").orElse(userPass.map(_._1))
    val password = config.getDeprecated[Option[String]]("password", "pass").orElse(userPass.map(_._2))
    val jndiName = config.get[Option[String]]("jndiName")

    DatabaseConfig(driver, url, username, password, jndiName)
  }
}
