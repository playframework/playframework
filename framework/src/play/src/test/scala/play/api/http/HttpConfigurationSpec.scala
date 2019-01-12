/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.http

import java.io.File

import com.typesafe.config.ConfigFactory
import org.specs2.mutable.Specification
import play.api.{ Configuration, Environment, Mode, PlayException }
import play.api.mvc.Cookie.SameSite
import play.core.cookie.encoding.{ ClientCookieDecoder, ClientCookieEncoder, ServerCookieDecoder, ServerCookieEncoder }

class HttpConfigurationSpec extends Specification {

  "HttpConfiguration" should {

    import scala.collection.JavaConverters._

    def properties = {
      Map(
        "play.http.context" -> "/",
        "play.http.parser.maxMemoryBuffer" -> "10k",
        "play.http.parser.maxDiskBuffer" -> "20k",
        "play.http.actionComposition.controllerAnnotationsFirst" -> "true",
        "play.http.actionComposition.executeActionCreatorActionFirst" -> "true",
        "play.http.cookies.strict" -> "true",
        "play.http.session.cookieName" -> "PLAY_SESSION",
        "play.http.session.secure" -> "true",
        "play.http.session.maxAge" -> "10s",
        "play.http.session.httpOnly" -> "true",
        "play.http.session.domain" -> "playframework.com",
        "play.http.session.path" -> "/session",
        "play.http.session.sameSite" -> "lax",
        "play.http.session.jwt.signatureAlgorithm" -> "HS256",
        "play.http.session.jwt.expiresAfter" -> null,
        "play.http.session.jwt.clockSkew" -> "30s",
        "play.http.session.jwt.dataClaim" -> "data",
        "play.http.flash.cookieName" -> "PLAY_FLASH",
        "play.http.flash.secure" -> "true",
        "play.http.flash.httpOnly" -> "true",
        "play.http.flash.domain" -> "playframework.com",
        "play.http.flash.path" -> "/flash",
        "play.http.flash.sameSite" -> "lax",
        "play.http.flash.jwt.signatureAlgorithm" -> "HS256",
        "play.http.flash.jwt.expiresAfter" -> null,
        "play.http.flash.jwt.clockSkew" -> "30s",
        "play.http.flash.jwt.dataClaim" -> "data",
        "play.http.fileMimeTypes" -> "foo=text/foo",
        "play.http.secret.key" -> "ad31779d4ee49d5ad5162bf1429c32e2e9933f3b",
        "play.http.secret.provider" -> null
      )
    }

    val configuration = new Configuration(ConfigFactory.parseMap(properties.asJava))

    val environment: Environment = Environment.simple(new File("."), Mode.Prod)

    "configure a context" in {
      val httpConfiguration = new HttpConfiguration.HttpConfigurationProvider(configuration, environment).get
      httpConfiguration.context must beEqualTo("/")
    }

    "throw an error when context does not starts with /" in {
      val config = properties + ("play.http.context" -> "something")
      val wrongConfiguration = Configuration(ConfigFactory.parseMap(config.asJava))
      new HttpConfiguration.HttpConfigurationProvider(wrongConfiguration, environment).get must throwA[PlayException]
    }

    "configure a session path" in {
      val httpConfiguration = new HttpConfiguration.HttpConfigurationProvider(configuration, environment).get
      httpConfiguration.session.path must beEqualTo("/session")
    }

    "throw an error when session path does not starts with /" in {
      val config = properties + ("play.http.session.path" -> "something")
      val wrongConfiguration = Configuration(ConfigFactory.parseMap(config.asJava))
      new HttpConfiguration.HttpConfigurationProvider(wrongConfiguration, environment).get must throwA[PlayException]
    }

    "configure a flash path" in {
      val httpConfiguration = new HttpConfiguration.HttpConfigurationProvider(configuration, environment).get
      httpConfiguration.flash.path must beEqualTo("/flash")
    }

    "throw an error when flash path does not starts with /" in {
      val config = properties + ("play.http.flash.path" -> "something")
      val wrongConfiguration = Configuration(ConfigFactory.parseMap(config.asJava))
      new HttpConfiguration.HttpConfigurationProvider(wrongConfiguration, environment).get must throwA[PlayException]
    }

    "throw an error when context includes a mimetype config setting" in {
      val config = properties + ("mimetype" -> "something")
      val wrongConfiguration = Configuration(ConfigFactory.parseMap(config.asJava))
      new HttpConfiguration.HttpConfigurationProvider(wrongConfiguration, environment).get must throwA[PlayException]
    }

    "configure max memory buffer" in {
      val httpConfiguration = new HttpConfiguration.HttpConfigurationProvider(configuration, environment).get
      httpConfiguration.parser.maxMemoryBuffer must beEqualTo(10 * 1024)
    }

    "configure max memory buffer to be more than Integer.MAX_VALUE" in {
      val testConfig = configuration ++ Configuration("play.http.parser.maxMemoryBuffer" -> s"${Int.MaxValue + 1L}")
      val httpConfiguration = new HttpConfiguration.HttpConfigurationProvider(testConfig, environment).get
      val expectedMaxMemoryBuffer: Long = Int.MaxValue + 1L
      httpConfiguration.parser.maxMemoryBuffer must beEqualTo(expectedMaxMemoryBuffer)
    }

    "configure max disk buffer" in {
      val httpConfiguration = new HttpConfiguration.HttpConfigurationProvider(configuration, environment).get
      httpConfiguration.parser.maxDiskBuffer must beEqualTo(20 * 1024)
    }

    "configure cookies encoder/decoder" in {
      val httpConfiguration = new HttpConfiguration.HttpConfigurationProvider(configuration, environment).get
      httpConfiguration.cookies.strict must beTrue
    }

    "configure session should set" in {

      "cookie name" in {
        val httpConfiguration = new HttpConfiguration.HttpConfigurationProvider(configuration, environment).get
        httpConfiguration.session.cookieName must beEqualTo("PLAY_SESSION")
      }

      "cookie security" in {
        val httpConfiguration = new HttpConfiguration.HttpConfigurationProvider(configuration, environment).get
        httpConfiguration.session.secure must beTrue
      }

      "cookie maxAge" in {
        val httpConfiguration = new HttpConfiguration.HttpConfigurationProvider(configuration, environment).get
        httpConfiguration.session.maxAge.map(_.toSeconds) must beEqualTo(Some(10))
      }

      "cookie httpOnly" in {
        val httpConfiguration = new HttpConfiguration.HttpConfigurationProvider(configuration, environment).get
        httpConfiguration.session.httpOnly must beTrue
      }

      "cookie domain" in {
        val httpConfiguration = new HttpConfiguration.HttpConfigurationProvider(configuration, environment).get
        httpConfiguration.session.domain must beEqualTo(Some("playframework.com"))
      }

      "cookie samesite" in {
        val httpConfiguration = new HttpConfiguration.HttpConfigurationProvider(configuration, environment).get
        httpConfiguration.session.sameSite must be some (SameSite.Lax)
      }
    }

    "configure flash should set" in {

      "cookie name" in {
        val httpConfiguration = new HttpConfiguration.HttpConfigurationProvider(configuration, environment).get
        httpConfiguration.flash.cookieName must beEqualTo("PLAY_FLASH")
      }

      "cookie security" in {
        val httpConfiguration = new HttpConfiguration.HttpConfigurationProvider(configuration, environment).get
        httpConfiguration.flash.secure must beTrue
      }

      "cookie httpOnly" in {
        val httpConfiguration = new HttpConfiguration.HttpConfigurationProvider(configuration, environment).get
        httpConfiguration.flash.httpOnly must beTrue
      }

      "cookie samesite" in {
        val httpConfiguration = new HttpConfiguration.HttpConfigurationProvider(configuration, environment).get
        httpConfiguration.flash.sameSite must be some (SameSite.Lax)
      }
    }

    "configure action composition" in {

      "controller annotations first" in {
        val httpConfiguration = new HttpConfiguration.HttpConfigurationProvider(configuration, environment).get
        httpConfiguration.actionComposition.controllerAnnotationsFirst must beTrue
      }

      "execute request handler action first" in {
        val httpConfiguration = new HttpConfiguration.HttpConfigurationProvider(configuration, environment).get
        httpConfiguration.actionComposition.executeActionCreatorActionFirst must beTrue
      }
    }

    "configure mime types" in {

      "for server encoder" in {
        val httpConfiguration = new HttpConfiguration.HttpConfigurationProvider(configuration, environment).get
        httpConfiguration.fileMimeTypes.mimeTypes must beEqualTo(Map("foo" -> "text/foo"))
      }
    }
  }

  "Cookies configuration" should {

    "be configured as strict" in {

      val cookieConfiguration = CookiesConfiguration(strict = true)

      "for server encoder" in {
        cookieConfiguration.serverEncoder must beEqualTo(ServerCookieEncoder.STRICT)
      }

      "for server decoder" in {
        cookieConfiguration.serverDecoder must beEqualTo(ServerCookieDecoder.STRICT)
      }

      "for client encoder" in {
        cookieConfiguration.clientEncoder must beEqualTo(ClientCookieEncoder.STRICT)
      }

      "for client decoder" in {
        cookieConfiguration.clientDecoder must beEqualTo(ClientCookieDecoder.STRICT)
      }
    }

    "be configured as lax" in {

      val cookieConfiguration = CookiesConfiguration(strict = false)

      "for server encoder" in {
        cookieConfiguration.serverEncoder must beEqualTo(ServerCookieEncoder.LAX)
      }

      "for server decoder" in {
        cookieConfiguration.serverDecoder must beEqualTo(ServerCookieDecoder.LAX)
      }

      "for client encoder" in {
        cookieConfiguration.clientEncoder must beEqualTo(ClientCookieEncoder.LAX)
      }

      "for client decoder" in {
        cookieConfiguration.clientDecoder must beEqualTo(ClientCookieDecoder.LAX)
      }
    }
  }
}
