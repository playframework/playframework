/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.ws

import org.specs2.mutable._
import com.typesafe.config.ConfigFactory
import play.api.Environment
import play.api.test.WithApplication

object DefaultWSConfigParserSpec extends Specification {

  "DefaultWSConfigParser" should {

    def parseThis(input: String)(implicit app: play.api.Application) = {
      val config = play.api.Configuration(ConfigFactory.parseString(input))
      val parser = new DefaultWSConfigParser(config, app.injector.instanceOf[Environment])
      parser.parse()
    }

    "parse ws base section" in new WithApplication {
      val actual = parseThis("""
                                |ws.timeout.connection = 9999
                                |ws.timeout.idle = 666
                                |ws.timeout.request = 1234
                                |ws.followRedirects = false
                                |ws.useProxyProperties = false
                                |ws.useragent = "FakeUserAgent"
                                |ws.acceptAnyCertificate = true
                              """.stripMargin)

      actual.connectionTimeout must beSome.which(_ must_== 9999)
      actual.idleTimeout must beSome.which(_ must_== 666)
      actual.requestTimeout must beSome.which(_ must_== 1234)

      // default: true
      actual.followRedirects must beSome.which(_ must_== false)

      // default: true
      actual.useProxyProperties must beSome.which(_ must_== false)

      actual.userAgent must beSome.which(_ must_== "FakeUserAgent")

      actual.acceptAnyCertificate must beSome.which(_ must_== true)

    }
  }
}
