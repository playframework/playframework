/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.http

import play.api.mvc._
import play.api.test._
import play.api.test.TestServer
import scala.util.Random
import scala.io.Source
import java.io.InputStream

object BadClientHandlingSpec extends PlaySpecification {

  "Play" should {

    def withServer[T](action: EssentialAction)(block: Port => T) = {
      val port = testServerPort
      running(TestServer(port, FakeApplication(
        withRoutes = {
          case _ => action
        }
      ))) {
        block(port)
      }
    }

    "gracefully handle long urls and return 414" in withServer(Action(Results.Ok)) { port =>
      val url = new String(Random.alphanumeric.take(5 * 1024).toArray)

      val conn = new java.net.URL("http://localhost:" + port + "/" + url).openConnection()
      try {
        Source.fromInputStream(conn.getContent.asInstanceOf[InputStream]).getLines()
        failure // must not reach this point
      } catch {
        case e: Exception =>
          e.getMessage must contain("Server returned HTTP response code: 414")
          success
      }
    }

  }
}
