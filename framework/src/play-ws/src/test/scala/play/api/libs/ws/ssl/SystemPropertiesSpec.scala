/*
 *
 *  * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import org.specs2.mutable.Specification
import org.specs2.specification.After
import play.api.libs.ws.DefaultWSClientConfig
import java.security.Security

object SystemPropertiesSpec extends Specification with After {

  sequential

  def after = sp.clearProperties()

  val sp = new SystemConfiguration()

  "SystemProperties" should {

    "disableCheckRevocation should not be set normally" in {
      val config = DefaultWSClientConfig(ssl = Some(DefaultSSLConfig(checkRevocation = None)))

      sp.configure(config)

      // http://stackoverflow.com/a/8507905/5266
      Security.getProperty("ocsp.enable") must beNull
      System.getProperty("com.sun.security.enableCRLDP") must beNull
      System.getProperty("com.sun.net.ssl.checkRevocation") must beNull
    }

    "disableCheckRevocation is set explicitly" in {
      val config = DefaultWSClientConfig(ssl = Some(DefaultSSLConfig(checkRevocation = Some(true))))

      sp.configure(config)

      // http://stackoverflow.com/a/8507905/5266
      Security.getProperty("ocsp.enable") must be("true")
      System.getProperty("com.sun.security.enableCRLDP") must be("true")
      System.getProperty("com.sun.net.ssl.checkRevocation") must be("true")
    }

    // @see http://www.oracle.com/technetwork/java/javase/documentation/tlsreadme2-176330.html
    "allowLegacyHelloMessages is not set" in {
      val config = DefaultWSClientConfig(ssl = Some(DefaultSSLConfig(loose = Some(DefaultSSLLooseConfig(allowLegacyHelloMessages = None)))))

      sp.configure(config)

      System.getProperty("sun.security.ssl.allowLegacyHelloMessages") must beNull
    }

    // @see http://www.oracle.com/technetwork/java/javase/documentation/tlsreadme2-176330.html
    "allowLegacyHelloMessages is set" in {
      val config = DefaultWSClientConfig(ssl = Some(DefaultSSLConfig(loose = Some(DefaultSSLLooseConfig(allowLegacyHelloMessages = Some(true))))))

      sp.configure(config)

      System.getProperty("sun.security.ssl.allowLegacyHelloMessages") must be("true")
    }

    // @see http://www.oracle.com/technetwork/java/javase/documentation/tlsreadme2-176330.html
    "allowUnsafeRenegotiation not set" in {
      val config = DefaultWSClientConfig(ssl = Some(DefaultSSLConfig(loose = Some(DefaultSSLLooseConfig(allowUnsafeRenegotiation = None)))))

      sp.configure(config)

      System.getProperty("sun.security.ssl.allowUnsafeRenegotiation") must beNull
    }

    // @see http://www.oracle.com/technetwork/java/javase/documentation/tlsreadme2-176330.html
    "allowUnsafeRenegotiation is set" in {
      val config = DefaultWSClientConfig(ssl = Some(DefaultSSLConfig(loose = Some(DefaultSSLLooseConfig(allowUnsafeRenegotiation = Some(true))))))

      sp.configure(config)

      System.getProperty("sun.security.ssl.allowUnsafeRenegotiation") must be("true")
    }

  }

}
