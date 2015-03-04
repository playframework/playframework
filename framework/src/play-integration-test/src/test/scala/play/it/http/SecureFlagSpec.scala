/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.http

import play.api.mvc._
import play.api.test._
import play.api.test.TestServer
import play.it._
import java.io.{ File, InputStream }
import javax.net.ssl.{ SSLContext, HttpsURLConnection, X509TrustManager }
import java.security.cert.X509Certificate
import scala.io.Source
import java.net.URL

object NettySecureFlagSpec extends SecureFlagSpec with NettyIntegrationSpecification
object AkkaHttpSecureFlagSpec extends SecureFlagSpec with AkkaHttpIntegrationSpecification

/**
 * Specs for the "secure" flag on requests
 */
trait SecureFlagSpec extends PlaySpecification with ServerIntegrationSpecification {

  sequential

  /** An action whose result is just "true" or "false" depending on the value of result.secure */
  val secureFlagAction = Action {
    request => Results.Ok(request.secure.toString)
  }

  // this step seems necessary to allow the generated keystore to be written
  new File("conf").mkdir()

  def withServer[T](action: EssentialAction, sslPort: Option[Int] = None)(block: Port => T) = {
    val port = testServerPort
    running(TestServer(port, sslPort = sslPort, application = FakeApplication(
      withRoutes = {
        case _ => action
      }
    ))) {
      block(port)
    }
  }

  "Play https server" should {

    val sslPort = 19943

    "show that requests are secure in the absence of X_FORWARDED_PROTO" in withServer(secureFlagAction, Some(sslPort)) { _ =>
      val conn = createConn(sslPort)
      Source.fromInputStream(conn.getContent.asInstanceOf[InputStream]).getLines().next must_== "true"
    }.pendingUntilAkkaHttpFixed // All these tests are waiting on Akka HTTP to support SSL
    "show that requests are secure in the absence of X_FORWARDED_PROTO" in withServer(secureFlagAction, Some(sslPort)) { _ =>
      val conn = createConn(sslPort)
      Source.fromInputStream(conn.getContent.asInstanceOf[InputStream]).getLines().next must_== "true"
    }.pendingUntilAkkaHttpFixed
    "show that requests are secure if X_FORWARDED_PROTO is https" in withServer(secureFlagAction, Some(sslPort)) { _ =>
      val conn = createConn(sslPort, Some("https"))
      Source.fromInputStream(conn.getContent.asInstanceOf[InputStream]).getLines().next must_== "true"
    }.pendingUntilAkkaHttpFixed
    "not show that requests are secure if X_FORWARDED_PROTO is http" in withServer(secureFlagAction, Some(sslPort)) { _ =>
      val conn = createConn(sslPort, Some("http"))
      Source.fromInputStream(conn.getContent.asInstanceOf[InputStream]).getLines().next must_== "false"
    }.pendingUntilAkkaHttpFixed
  }

  "Play http server" should {
    "not show that requests are secure in the absence of X_FORWARDED_PROTO" in withServer(secureFlagAction) { port =>
      val responses = BasicHttpClient.makeRequests(port)(
        BasicRequest("GET", "/", "HTTP/1.1", Map(), "foo")
      )
      responses.length must_== 1
      responses(0).body must_== Left("false")
    }
    "show that requests are secure if X_FORWARDED_PROTO is https" in withServer(secureFlagAction) { port =>
      val responses = BasicHttpClient.makeRequests(port)(
        BasicRequest("GET", "/", "HTTP/1.1", Map((X_FORWARDED_PROTO, "https")), "foo")
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
    forwardedProto.foreach(proto => conn.setRequestProperty(X_FORWARDED_PROTO, proto))
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

    def checkClientTrusted(x509Certificates: Array[X509Certificate], s: String) {}

    def checkServerTrusted(x509Certificates: Array[X509Certificate], s: String) {}

    def getAcceptedIssuers = nullArray
  }

}
