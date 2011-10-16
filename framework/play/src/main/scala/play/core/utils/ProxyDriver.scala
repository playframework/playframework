package play.utils

import java.sql._
import java.util._

class ProxyDriver(proxied: Driver) extends Driver {

  def acceptsURL(url: String) = proxied.acceptsURL(url)
  def connect(user: String, properties: Properties) = proxied.connect(user, properties)
  def getMajorVersion() = proxied.getMajorVersion
  def getMinorVersion() = proxied.getMinorVersion
  def getPropertyInfo(user: String, properties: Properties) = proxied.getPropertyInfo(user, properties)
  def jdbcCompliant() = proxied.jdbcCompliant

}