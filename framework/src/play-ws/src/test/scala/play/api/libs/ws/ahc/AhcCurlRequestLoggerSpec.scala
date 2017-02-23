/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.ws.ahc

import org.slf4j.Logger
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import play.api.libs.ws._
import play.api.test.{ WithServer, WsTestClient, PlaySpecification, WithApplication }

import scala.concurrent.Future

class AhcCurlRequestLoggerSpec extends PlaySpecification
    with WsTestClient
    with Mockito
    with org.specs2.specification.mutable.ExecutionEnvironment {

  def is(implicit ee: ExecutionEnv) = {

    "AhcCurlRequestLogger" should {

      "log a request with custom headers" in new WithServer {
        val client = wsUrl("/")
        val logger = mock[Logger]

        val headers = Seq(
          "accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
          "user-agent" -> "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.94 Safari/537.36"
        )

        val responseFuture = client.withRequestFilter(AhcCurlRequestLogger(logger))
          .withHeaders(headers: _*)
          .get()
        responseFuture must beAnInstanceOf[AhcWSResponse].await

        val curlStatement = s"""curl \\
                              |  --verbose \\
                              |  --request GET \\
                              |  --header 'accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8' \\
                              |  --header 'user-agent: Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.94 Safari/537.36' \\
                              |  'http://localhost:$testServerPort/'""".stripMargin
        there was one(logger).info(curlStatement)
      }

      "log a request with POST" in new WithServer() {
        val client = wsUrl("/")
        val logger = mock[Logger]

        val responseFuture = client.withRequestFilter(AhcCurlRequestLogger(logger))
          .post(Map("key" -> Seq("value")))
        responseFuture must beAnInstanceOf[AhcWSResponse].await

        val curlStatement = s"""curl \\
                              |  --verbose \\
                              |  --request POST \\
                              |  --header 'Content-Type: application/x-www-form-urlencoded' \\
                              |  --data 'key=value' \\
                              |  'http://localhost:$testServerPort/'""".stripMargin
        there was one(logger).info(curlStatement)
      }

      "log a request with POST with an explicit content type" in new WithServer() {
        val client = wsUrl("/")
        val logger = mock[Logger]
        val headers = Seq("Content-Type" -> "text/plain; charset=utf-8")

        val responseFuture = client.withRequestFilter(AhcCurlRequestLogger(logger))
          .withHeaders(headers: _*)
          .post("this is plain text")
        responseFuture must beAnInstanceOf[AhcWSResponse].await

        val curlStatement = s"""curl \\
                              |  --verbose \\
                              |  --request POST \\
                              |  --header 'Content-Type: text/plain; charset=utf-8' \\
                              |  --data 'this is plain text' \\
                              |  'http://localhost:$testServerPort/'""".stripMargin
        there was one(logger).info(curlStatement)
      }

      "log a query string" in new WithServer() {
        val client = wsUrl("/")
        val logger = mock[Logger]
        val requestLogger = AhcCurlRequestLogger(logger)

        val responseFuture = client.withRequestFilter(AhcCurlRequestLogger(logger))
          .withQueryString("search" -> "&?$HOME'")
          .get()
        responseFuture must beAnInstanceOf[AhcWSResponse].await

        val curlStatement = s"""curl \\
                              |  --verbose \\
                              |  --request GET \\
                              |  'http://localhost:$testServerPort/?search=%26%3F%24HOME%27'""".stripMargin
        there was one(logger).info(curlStatement)
      }

      "log a request with POST with a hanging quote" in new WithServer() {
        val client = wsUrl("/")
        val logger = mock[Logger]
        val requestLogger = AhcCurlRequestLogger(logger)
        val headers = Seq(
          "Content-Type" -> "text/plain; charset=utf-8"
        )

        val responseFuture = client.withRequestFilter(AhcCurlRequestLogger(logger))
          .withHeaders(headers: _*)
          .post("this is ' text with a hanging quote")
        responseFuture must beAnInstanceOf[AhcWSResponse].await

        val curlStatement = s"""curl \\
                              |  --verbose \\
                              |  --request POST \\
                              |  --header 'Content-Type: text/plain; charset=utf-8' \\
                              |  --data 'this is '\\'' text with a hanging quote' \\
                              |  'http://localhost:$testServerPort/'""".stripMargin

        there was one(logger).info(curlStatement)
      }

      "log a request with PUT" in new WithServer {
        val client = wsUrl("/")
        val logger = mock[Logger]

        val responseFuture = client.withRequestFilter(AhcCurlRequestLogger(logger))
          .withBody(Map("param1" -> Seq("value1")))
          .put(Map("key" -> Seq("value")))
        responseFuture must beAnInstanceOf[AhcWSResponse].await

        val curlStatement = s"""curl \\
                              |  --verbose \\
                              |  --request PUT \\
                              |  --header 'Content-Type: application/x-www-form-urlencoded' \\
                              |  --data 'key=value' \\
                              |  'http://localhost:$testServerPort/'""".stripMargin

        there was one(logger).info(curlStatement)
      }
      //
      //      "log a request with a proxy" in new WithServer {
      //        val client = wsUrl("/")
      //        val proxy = DefaultWSProxyServer(host = "localhost", port = 8080)
      //        val logger = mock[Logger]
      //        val requestLogger = AhcCurlRequestLogger(logger)
      //
      //        val responseFuture = client.withRequestFilter(AhcCurlRequestLogger(logger))
      //          .withProxyServer(proxy)
      //          .get()
      //        responseFuture must beAnInstanceOf[AhcWSResponse].await
      //
      //        val curlStatement = s"""curl \\
      //                              |  --verbose \\
      //                              |  --request GET \\
      //                              |  --proxy localhost:8080 \\
      //                              |  'http://localhost:$testServerPort/'""".stripMargin
      //
      //        there was one(logger).info(curlStatement)
      //      }
    }
  }

}
