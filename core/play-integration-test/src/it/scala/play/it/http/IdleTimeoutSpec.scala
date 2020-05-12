/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http

import java.io.IOException
import java.net.SocketException

import akka.stream.scaladsl.Sink
import play.api.BuiltInComponents
import play.api.mvc.EssentialAction
import play.api.mvc.Results
import play.api.test._
import play.api.libs.streams.Accumulator
import play.api.routing.Router
import play.core.server._
import play.it.test.AkkaHttpServerEndpointRecipes
import play.it.test.EndpointIntegrationSpecification
import play.it.test.NettyServerEndpointRecipes

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits._
import scala.util.Random

class IdleTimeoutSpec extends PlaySpecification with EndpointIntegrationSpecification with ApplicationFactories {

  def timeouts(httpTimeout: Duration, httpsTimeout: Duration): Map[String, String] = {
    def getTimeout(d: Duration) = d match {
      case Duration.Inf   => "null"
      case Duration(t, u) => s"${u.toMillis(t)}ms"
    }

    Map(
      "play.server.http.idleTimeout"  -> getTimeout(httpTimeout),
      "play.server.https.idleTimeout" -> getTimeout(httpsTimeout)
    )
  }

  "Play's idle timeout support" should {

    def withServerAndConfig(extraConfig: Map[String, Any] = Map.empty): ApplicationFactory = {
      withConfigAndRouter(extraConfig) { components: BuiltInComponents =>
        Router.from {
          case _ =>
            EssentialAction { rh =>
              Accumulator(Sink.ignore).map(_ => Results.Ok)
            }
        }
      }
    }

    def endpoints(extraConfig: Map[String, Any]): Seq[ServerEndpointRecipe] =
      Seq(
        AkkaHttpServerEndpointRecipes.AkkaHttp11Plaintext,
        AkkaHttpServerEndpointRecipes.AkkaHttp11Encrypted,
        NettyServerEndpointRecipes.Netty11Plaintext,
        NettyServerEndpointRecipes.Netty11Encrypted,
      ).map(_.withExtraServerConfiguration(extraConfig))

    def akkaHttp2endpoints(extraConfig: Map[String, Any]): Seq[ServerEndpointRecipe] =
      Seq(
        AkkaHttpServerEndpointRecipes.AkkaHttp20Plaintext,
        AkkaHttpServerEndpointRecipes.AkkaHttp20Encrypted,
      ).map(_.withExtraServerConfiguration(extraConfig))

    def doRequests(port: Int, trickle: Long, secure: Boolean = false) = {
      val body = new String(Random.alphanumeric.take(50 * 1024).toArray)
      val responses = BasicHttpClient.makeRequests(port, secure = secure, trickleFeed = Some(trickle))(
        BasicRequest("POST", "/", "HTTP/1.1", Map("Content-Length" -> body.length.toString), body),
        // Second request ensures that Play switches back to its normal handler
        BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
      )
      responses
    }

    "support null as an infinite timeout" in {
      val extraConfig = Map(
        "play.server.http.idleTimeout"  -> null,
        "play.server.https.idleTimeout" -> null
      )
      withServerAndConfig(extraConfig).withEndpoints(endpoints(extraConfig)) { endpoint: ServerEndpoint =>
        // We are interested to know that the server started correctly with "null"
        // configurations. So there is no need to wait for a longer time.
        val responses = doRequests(endpoint.port, trickle = 200L, secure = "https" == endpoint.scheme)
        responses.length must_== 2
        responses(0).status must_== 200
        responses(1).status must_== 200
      }
    }

    "support 'infinite' as an infinite timeout" in {
      val extraConfig = Map(
        "play.server.http.idleTimeout"  -> "infinite",
        "play.server.https.idleTimeout" -> "infinite"
      )
      withServerAndConfig(extraConfig).withEndpoints(endpoints(extraConfig)) { endpoint: ServerEndpoint =>
        // We are interested to know that the server started correctly with "infinite"
        // configurations. So there is no need to wait for a longer time.
        val responses = doRequests(endpoint.port, trickle = 200L, secure = "https" == endpoint.scheme)
        responses.length must_== 2
        responses(0).status must_== 200
        responses(1).status must_== 200
      }
    }

    "support sub-second timeouts" in {
      val extraConfig = timeouts(httpTimeout = 300.millis, httpsTimeout = 300.millis)
      withServerAndConfig(extraConfig).withEndpoints(endpoints(extraConfig)) { endpoint: ServerEndpoint =>
        doRequests(endpoint.port, trickle = 400L, secure = "https" == endpoint.scheme) must throwA[IOException].like {
          case e => (e must beAnInstanceOf[SocketException]).or(e.getCause must beAnInstanceOf[SocketException])
        }
      }
    }

    "support a separate timeout for https" in {
      val extraConfig = timeouts(1.second, httpsTimeout = 400.millis)
      withServerAndConfig(extraConfig).withEndpoints(endpoints(extraConfig)) { endpoint: ServerEndpoint =>
        if (endpoint.scheme == "http") {
          val responses = doRequests(endpoint.port, trickle = 200L)
          responses.length must_== 2
          responses(0).status must_== 200
          responses(1).status must_== 200
        } else {
          doRequests(endpoint.port, trickle = 600L, secure = true) must throwA[IOException].like {
            case e => (e must beAnInstanceOf[SocketException]).or(e.getCause must beAnInstanceOf[SocketException])
          }
        }
      }
    }

    "support multi-second timeouts" in {
      val extraConfig = timeouts(httpTimeout = 1500.millis, httpsTimeout = 1500.millis)
      withServerAndConfig(extraConfig).withEndpoints(endpoints(extraConfig)) { endpoint: ServerEndpoint =>
        doRequests(endpoint.port, trickle = 2600L, secure = "https" == endpoint.scheme) must throwA[IOException].like {
          case e => (e must beAnInstanceOf[SocketException]).or(e.getCause must beAnInstanceOf[SocketException])
        }
      }
    }

    "not timeout for slow requests with a sub-second timeout" in {
      val extraConfig = timeouts(httpTimeout = 700.millis, httpsTimeout = 700.millis)
      withServerAndConfig(extraConfig).withEndpoints(endpoints(extraConfig)) { endpoint: ServerEndpoint =>
        val responses = doRequests(endpoint.port, trickle = 400L, secure = "https" == endpoint.scheme)
        responses.length must_== 2
        responses(0).status must_== 200
        responses(1).status must_== 200
      }
    }

    "not timeout for slow requests with a multi-second timeout" in {
      val extraConfig = timeouts(httpTimeout = 1500.millis, httpsTimeout = 1500.millis)
      withServerAndConfig(extraConfig).withEndpoints(endpoints(extraConfig)) { endpoint: ServerEndpoint =>
        val responses = doRequests(endpoint.port, trickle = 1000L, secure = "https" == endpoint.scheme)
        responses.length must_== 2
        responses(0).status must_== 200
        responses(1).status must_== 200
      }
    }

    "always be infinite when using akka-http HTTP/2" in {
      // See https://github.com/akka/akka-http/pull/2776
      val extraConfig = timeouts(httpTimeout = 100.millis, httpsTimeout = 100.millis) // will be ignored
      withServerAndConfig(extraConfig).withEndpoints(akkaHttp2endpoints(extraConfig)) { endpoint: ServerEndpoint =>
        val responses = doRequests(endpoint.port, trickle = 1500L, secure = "https" == endpoint.scheme)
        responses.length must_== 2
        responses(0).status must_== 200
        responses(1).status must_== 200
      }
    }
  }
}
