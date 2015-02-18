/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.utils

import java.sql._
import java.util.logging.Logger

class ProxyDriver(proxied: Driver) extends Driver {

  def acceptsURL(url: String) = proxied.acceptsURL(url)
  def connect(user: String, properties: java.util.Properties) = proxied.connect(user, properties)
  def getMajorVersion() = proxied.getMajorVersion
  def getMinorVersion() = proxied.getMinorVersion
  def getPropertyInfo(user: String, properties: java.util.Properties) = proxied.getPropertyInfo(user, properties)
  def jdbcCompliant() = proxied.jdbcCompliant
  def getParentLogger(): Logger = null

}
