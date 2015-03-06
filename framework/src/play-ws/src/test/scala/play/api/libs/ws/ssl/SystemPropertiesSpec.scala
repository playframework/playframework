/*
 *
 *  * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import org.specs2.mutable._
import java.security.Security

import play.api.libs.ws.WSClientConfig

object SystemPropertiesSpec extends Specification with After {

  sequential

  def after = sp.clearProperties()

  val sp = new SystemConfiguration()

  "SystemProperties" should {

    "disableCheckRevocation should not be set normally" in {
      val config = WSClientConfig(ssl = SSLConfig(checkRevocation = None))

      val originalOscp = Security.getProperty("ocsp.enable")

      sp.configure(config)

      // http://stackoverflow.com/a/8507905/5266
      Security.getProperty("ocsp.enable") must_== originalOscp
      System.getProperty("com.sun.security.enableCRLDP") must beNull
      System.getProperty("com.sun.net.ssl.checkRevocation") must beNull
    }

    "disableCheckRevocation is set explicitly" in {
      val config = WSClientConfig(ssl = SSLConfig(checkRevocation = Some(true)))

      sp.configure(config)

      // http://stackoverflow.com/a/8507905/5266
      Security.getProperty("ocsp.enable") must be("true")
      System.getProperty("com.sun.security.enableCRLDP") must be("true")
      System.getProperty("com.sun.net.ssl.checkRevocation") must be("true")
    }

    // @see http://www.oracle.com/technetwork/java/javase/documentation/tlsreadme2-176330.html
    "allowLegacyHelloMessages is not set" in {
      val config = WSClientConfig(ssl = SSLConfig(loose = SSLLooseConfig(allowLegacyHelloMessages = None)))

      sp.configure(config)

      System.getProperty("sun.security.ssl.allowLegacyHelloMessages") must beNull
    }

    // @see http://www.oracle.com/technetwork/java/javase/documentation/tlsreadme2-176330.html
    "allowLegacyHelloMessages is set" in {
      val config = WSClientConfig(ssl = SSLConfig(loose = SSLLooseConfig(allowLegacyHelloMessages = Some(true))))

      sp.configure(config)

      System.getProperty("sun.security.ssl.allowLegacyHelloMessages") must be("true")
    }

    // @see http://www.oracle.com/technetwork/java/javase/documentation/tlsreadme2-176330.html
    "allowUnsafeRenegotiation not set" in {
      val config = WSClientConfig(ssl = SSLConfig(loose = SSLLooseConfig(allowUnsafeRenegotiation = None)))

      sp.configure(config)

      System.getProperty("sun.security.ssl.allowUnsafeRenegotiation") must beNull
    }

    // @see http://www.oracle.com/technetwork/java/javase/documentation/tlsreadme2-176330.html
    "allowUnsafeRenegotiation is set" in {
      val config = WSClientConfig(ssl = SSLConfig(loose = SSLLooseConfig(allowUnsafeRenegotiation = Some(true))))

      sp.configure(config)

      System.getProperty("sun.security.ssl.allowUnsafeRenegotiation") must be("true")
    }

  }

}
