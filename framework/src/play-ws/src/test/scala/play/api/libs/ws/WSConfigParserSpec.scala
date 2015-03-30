/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.ws

import org.specs2.mutable._
import com.typesafe.config.ConfigFactory
import org.specs2.time.NoTimeConversions
import play.api.Environment
import play.api.test.WithApplication

import scala.concurrent.duration._

object WSConfigParserSpec extends Specification with NoTimeConversions {

  "WSConfigParser" should {

    def parseThis(input: String)(implicit app: play.api.Application) = {
      val config = play.api.Configuration(ConfigFactory.parseString(input).withFallback(ConfigFactory.defaultReference()))
      val parser = new WSConfigParser(config, app.injector.instanceOf[Environment])
      parser.parse()
    }

    "parse ws base section" in new WithApplication {
      val actual = parseThis("""
                                |play.ws.timeout.connection = 9999 ms
                                |play.ws.timeout.idle = 666 ms
                                |play.ws.timeout.request = 1234 ms
                                |play.ws.followRedirects = false
                                |play.ws.useProxyProperties = false
                                |play.ws.useragent = "FakeUserAgent"
                              """.stripMargin)

      actual.connectionTimeout must_== 9999.millis
      actual.idleTimeout must_== 666.millis
      actual.requestTimeout must_== 1234.millis

      // default: true
      actual.followRedirects must beFalse

      // default: true
      actual.useProxyProperties must beFalse

      actual.userAgent must beSome.which(_ must_== "FakeUserAgent")
    }
  }
}
