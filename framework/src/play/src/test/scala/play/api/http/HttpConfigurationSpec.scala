/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.http

import com.typesafe.config.ConfigFactory
import org.specs2.mutable.Specification
import play.api.{ PlayException, Configuration }
import play.core.netty.utils.{ ClientCookieEncoder, ClientCookieDecoder, ServerCookieDecoder, ServerCookieEncoder }

object HttpConfigurationSpec extends Specification {

  "HttpConfiguration" should {

    import scala.collection.JavaConversions._

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
        "play.http.flash.cookieName" -> "PLAY_FLASH",
        "play.http.flash.secure" -> "true",
        "play.http.flash.httpOnly" -> "true"
      )
    }

    val configuration = new Configuration(ConfigFactory.parseMap(properties))

    "configure a context" in {
      val httpConfiguration = new HttpConfiguration.HttpConfigurationProvider(configuration).get
      httpConfiguration.context must beEqualTo("/")
    }

    "throw an error when context does not starts with /" in {
      val config = properties + ("play.http.context" -> "something")
      val wrongConfiguration = Configuration(ConfigFactory.parseMap(config))
      new HttpConfiguration.HttpConfigurationProvider(wrongConfiguration).get must throwA[PlayException]
    }

    "configure max memory buffer" in {
      val httpConfiguration = new HttpConfiguration.HttpConfigurationProvider(configuration).get
      httpConfiguration.parser.maxMemoryBuffer must beEqualTo(10 * 1024)
    }

    "configure max disk buffer" in {
      val httpConfiguration = new HttpConfiguration.HttpConfigurationProvider(configuration).get
      httpConfiguration.parser.maxDiskBuffer must beEqualTo(20 * 1024)
    }

    "configure cookies encoder/decoder" in {
      val httpConfiguration = new HttpConfiguration.HttpConfigurationProvider(configuration).get
      httpConfiguration.cookies.strict must beTrue
    }

    "configure session should set" in {

      "cookie name" in {
        val httpConfiguration = new HttpConfiguration.HttpConfigurationProvider(configuration).get
        httpConfiguration.session.cookieName must beEqualTo("PLAY_SESSION")
      }

      "cookie security" in {
        val httpConfiguration = new HttpConfiguration.HttpConfigurationProvider(configuration).get
        httpConfiguration.session.secure must beTrue
      }

      "cookie maxAge" in {
        val httpConfiguration = new HttpConfiguration.HttpConfigurationProvider(configuration).get
        httpConfiguration.session.maxAge.map(_.toSeconds) must beEqualTo(Some(10))
      }

      "cookie httpOnly" in {
        val httpConfiguration = new HttpConfiguration.HttpConfigurationProvider(configuration).get
        httpConfiguration.session.httpOnly must beTrue
      }

      "cookie domain" in {
        val httpConfiguration = new HttpConfiguration.HttpConfigurationProvider(configuration).get
        httpConfiguration.session.domain must beEqualTo(Some("playframework.com"))
      }
    }

    "configure flash should set" in {

      "cookie name" in {
        val httpConfiguration = new HttpConfiguration.HttpConfigurationProvider(configuration).get
        httpConfiguration.flash.cookieName must beEqualTo("PLAY_FLASH")
      }

      "cookie security" in {
        val httpConfiguration = new HttpConfiguration.HttpConfigurationProvider(configuration).get
        httpConfiguration.flash.secure must beTrue
      }

      "cookie httpOnly" in {
        val httpConfiguration = new HttpConfiguration.HttpConfigurationProvider(configuration).get
        httpConfiguration.flash.httpOnly must beTrue
      }
    }

    "configure action composition" in {

      "controller annotations first" in {
        val httpConfiguration = new HttpConfiguration.HttpConfigurationProvider(configuration).get
        httpConfiguration.actionComposition.controllerAnnotationsFirst must beTrue
      }

      "execute request handler action first" in {
        val httpConfiguration = new HttpConfiguration.HttpConfigurationProvider(configuration).get
        httpConfiguration.actionComposition.executeActionCreatorActionFirst must beTrue
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
