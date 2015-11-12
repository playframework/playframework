/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.db

import javax.inject.{ Inject, Provider }

import play.api.{ Configuration, Environment, PlayConfig }

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

class DatabaseConfigProvider @Inject() (configuration: Configuration, environment: Environment)
    extends Provider[DatabaseConfig] {

  val config = PlayConfig(configuration)

  def get(): DatabaseConfig = {
    val driver = config.get[Option[String]]("driver")
    val (url, userPass) = ConnectionPool.extractUrl(config.get[Option[String]]("url"), environment.mode)
    val username = config.getDeprecated[Option[String]]("username", "user").orElse(userPass.map(_._1))
    val password = config.getDeprecated[Option[String]]("password", "pass").orElse(userPass.map(_._2))
    val jndiName = config.get[Option[String]]("jndiName")

    DatabaseConfig(driver, url, username, password, jndiName)
  }

}

object DatabaseConfig {

  @Deprecated("please use the DatabaseConfigProvider class")
  def fromConfig(config: PlayConfig, environment: Environment) = {

    val driver = config.get[Option[String]]("driver")
    val (url, userPass) = ConnectionPool.extractUrl(config.get[Option[String]]("url"), environment.mode)
    val username = config.getDeprecated[Option[String]]("username", "user").orElse(userPass.map(_._1))
    val password = config.getDeprecated[Option[String]]("password", "pass").orElse(userPass.map(_._2))
    val jndiName = config.get[Option[String]]("jndiName")

    DatabaseConfig(driver, url, username, password, jndiName)
  }
}