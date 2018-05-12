/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http

import java.io.{ File, InputStream }
import java.net.URL
import java.security.cert.X509Certificate
import javax.net.ssl.{ HttpsURLConnection, SSLContext, X509TrustManager }

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test._
import play.it._

import scala.io.Source

class NettySecureFlagSpec extends SecureFlagSpec with NettyIntegrationSpecification
class AkkaHttpSecureFlagSpec extends SecureFlagSpec with AkkaHttpIntegrationSpecification

/**
 * Specs for the "secure" flag on requests
 */
trait SecureFlagSpec extends PlaySpecification with ServerIntegrationSpecification {

  sequential

  /** An action whose result is just "true" or "false" depending on the value of result.secure */
  val secureFlagAction = ActionBuilder.ignoringBody { request: Request[_] =>
    Results.Ok(request.secure.toString)
  }

  // this step seems necessary to allow the generated keystore to be written
  new File("conf").mkdir()

  def withServer[T](action: EssentialAction, sslPort: Option[Int] = None)(block: Port => T) = {
    val port = testServerPort
    running(TestServer(port, sslPort = sslPort, application = GuiceApplicationBuilder()
      .routes { case _ => action }
      .build()
    )) {
      block(port)
    }
  }

  "Play https server" should {

    val sslPort = 19943

    def test(connection: HttpsURLConnection, expect: Boolean) = {
      Source.fromInputStream(connection.getContent.asInstanceOf[InputStream]).mkString must_== expect.toString
    }

    "show that requests are secure in the absence of X_FORWARDED_PROTO" in withServer(secureFlagAction, Some(sslPort)) { _ =>
      test(createConn(sslPort), true)
    }
    "show that requests are secure if X_FORWARDED_PROTO is https" in withServer(secureFlagAction, Some(sslPort)) { _ =>
      test(createConn(sslPort, Some("https")), true)
    }
    "not show that requests are not secure if X_FORWARDED_PROTO is http" in withServer(secureFlagAction, Some(sslPort)) { _ =>
      test(createConn(sslPort, Some("http")), false)
    }
  }

  "Play http server" should {
    "not show that requests are not secure in the absence of X_FORWARDED_PROTO" in withServer(secureFlagAction) { port =>
      val responses = BasicHttpClient.makeRequests(port)(
        BasicRequest("GET", "/", "HTTP/1.1", Map(), "foo")
      )
      responses.length must_== 1
      responses(0).body must_== Left("false")
    }
    "show that requests are secure if X_FORWARDED_PROTO is https" in withServer(secureFlagAction) { port =>
      val responses = BasicHttpClient.makeRequests(port)(
        BasicRequest("GET", "/", "HTTP/1.1", Map(X_FORWARDED_FOR -> "127.0.0.1", X_FORWARDED_PROTO -> "https"), "foo")
      )
      responses.length must_== 1
      responses(0).body must_== Left("true")
    }
    "not show that requests are secure if X_FORWARDED_PROTO is http" in withServer(secureFlagAction) { port =>
      val responses = BasicHttpClient.makeRequests(port)(
        BasicRequest("GET", "/", "HTTP/1.1", Map((X_FORWARDED_PROTO, "http")), "foo")
      )
      responses.length must_== 1
      responses(0).body must_== Left("false")
    }
  }

  // the following are adapted from SslSpec

  def createConn(sslPort: Int, forwardedProto: Option[String] = None) = {
    val conn = new URL("https://localhost:" + sslPort + "/").openConnection().asInstanceOf[HttpsURLConnection]
    forwardedProto.foreach { proto =>
      conn.setRequestProperty(X_FORWARDED_FOR, "127.0.0.1")
      conn.setRequestProperty(X_FORWARDED_PROTO, proto)
    }
    conn.setSSLSocketFactory(sslFactory)
    conn
  }

  def sslFactory = {
    val ctx = SSLContext.getInstance("TLS")
    ctx.init(null, Array(MockTrustManager()), null)
    ctx.getSocketFactory
  }

  case class MockTrustManager() extends X509TrustManager {
    val nullArray = Array[X509Certificate]()

    def checkClientTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = {}

    def checkServerTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = {}

    def getAcceptedIssuers = nullArray
  }

}
