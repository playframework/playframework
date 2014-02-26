/*
 *
 *  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import org.specs2.mutable._
import play.api.libs.ws.DefaultWSClientConfig

object SystemPropertiesSpec extends Specification {

  sequential

  val sp = new SystemConfiguration()

  "SystemProperties" should {

    "disableCheckRevocation" in {
      val config = DefaultWSClientConfig(ssl = Some(DefaultSSLConfig(loose = Some(DefaultSSLLooseConfig(disableCheckRevocation = Some(true))))))

      sp.configureSystemProperties(config)

      // http://stackoverflow.com/a/8507905/5266
      System.getProperty("ocsp.enable") must be("false")
      System.getProperty("com.sun.security.enableCRLDP") must be("false")
      System.getProperty("com.sun.net.ssl.checkRevocation") must be("false")
    }.after {
      sp.clearProperties()
    }

    // @see http://www.oracle.com/technetwork/java/javase/documentation/tlsreadme2-176330.html
    "allowLegacyHelloMessages" in {
      val config = DefaultWSClientConfig(ssl = Some(DefaultSSLConfig(loose = Some(DefaultSSLLooseConfig(allowLegacyHelloMessages = Some(true))))))

      sp.configureSystemProperties(config)

      System.getProperty("sun.security.ssl.allowLegacyHelloMessages") must be("true")
    }.after {
      sp.clearProperties()
    }

    // @see http://www.oracle.com/technetwork/java/javase/documentation/tlsreadme2-176330.html
    "allowUnsafeRenegotiation" in {
      val config = DefaultWSClientConfig(ssl = Some(DefaultSSLConfig(loose = Some(DefaultSSLLooseConfig(allowUnsafeRenegotiation = Some(true))))))

      sp.configureSystemProperties(config)

      System.getProperty("sun.security.ssl.allowUnsafeRenegotiation") must be("true")
    }.after {
      sp.clearProperties()
    }

  }

}
