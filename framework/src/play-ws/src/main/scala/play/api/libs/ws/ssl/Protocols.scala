/*
 *
 *  * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

object Protocols {

  /**
   * Protocols which are known to be insecure.
   */
  val deprecatedProtocols = Set("SSL", "SSLv2Hello", "SSLv3")

  val recommendedProtocols = Array("TLSv1.2", "TLSv1.1", "TLSv1")

  // Use 1.2 as a default in 1.7, use 1.0 in 1.6
  // https://docs.fedoraproject.org/en-US/Fedora_Security_Team//html/Defensive_Coding/sect-Defensive_Coding-TLS-Client-OpenJDK.html
  def recommendedProtocol = foldVersion(run16 = "TLSv1", runHigher = "TLSv1.2")

}
