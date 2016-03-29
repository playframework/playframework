/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.http

import java.net.SocketException

import akka.stream.scaladsl.Sink
import play.api.Configuration
import play.api.mvc.{ EssentialAction, Results }
import play.api.test.{ FakeApplication, _ }
import play.it.{ NettyIntegrationSpecification, ServerIntegrationSpecification }
import play.it.AkkaHttpIntegrationSpecification
import play.api.libs.iteratee._
import play.api.libs.streams.Accumulator
import play.core.server.ServerProvider
import play.core.server.NettyServerProvider
import play.core.server.NettyServer
import play.core.server.Server
import play.core.server.ServerProvider.Context
import play.core.server.akkahttp.AkkaHttpServerProvider
import play.core.server.akkahttp.AkkaHttpServer

import scala.concurrent.ExecutionContext.Implicits._
import scala.util.Random

object NettyIdleTimeoutSpec extends IdleTimeoutSpec with NettyIntegrationSpecification {
  override def integrationServerProvider: ServerProvider = new NettyServerProvider() with ServerConfigOverride {
    override def createServer(context: Context): NettyServer = super.createServer(setupContext(context))
  }
}

object AkkaIdleTimeoutSpec extends IdleTimeoutSpec with AkkaHttpIntegrationSpecification {
  override def integrationServerProvider: ServerProvider = new AkkaHttpServerProvider() with ServerConfigOverride {
    override def createServer(context: Context): AkkaHttpServer = super.createServer(setupContext(context))
  }
}

// need to jump through some hoops here to keep binary compatibility for 2.5.x and support different timeouts per test
// Lightbend Config caches system properties. Ideally play.api.test.TestServer would be changed to allow passing
// config values when creating the server config
trait ServerConfigOverride {
  def setupContext(context: Context) = {
    val idleConfig = Configuration("play.server.http.idleTimeout" -> System.getProperty("play.server.http.idleTimeout")) ++
      Option(System.getProperty("play.server.https.idleTimeout"))
      .map(t => Configuration("play.server.https.idleTimeout" -> t)).getOrElse(Configuration.empty)
    context.copy(config = context.config.copy(configuration = context.config.configuration ++ idleConfig))
  }
}

trait IdleTimeoutSpec extends PlaySpecification with ServerIntegrationSpecification {
  val httpsPort = 9443

  "Play's idle timeout support" should {
    def withServer[T](httpTimeout: Int, httpsPort: Option[Int] = None, httpsTimeout: Option[Int] = None)(action: EssentialAction)(block: Port => T) = {
      val port = testServerPort
      System.setProperty("play.server.http.idleTimeout", s"${httpTimeout}ms")
      httpsTimeout.foreach(t => System.setProperty("play.server.https.idleTimeout", s"${t}ms"))
      running(TestServer(port, sslPort = httpsPort, application = FakeApplication(
        withRoutes = {
          case _ => action
        }
      ))) {
        block(port)
      }
    }

    def doRequests(port: Int, trickle: Long, secure: Boolean = false) = {
      val body = new String(Random.alphanumeric.take(50 * 1024).toArray)
      val responses = BasicHttpClient.makeRequests(port, secure = secure, trickleFeed = Some(trickle))(
        BasicRequest("POST", "/", "HTTP/1.1", Map("Content-Length" -> body.length.toString), body),
        // Second request ensures that Play switches back to its normal handler
        BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
      )
      responses
    }

    "support sub-second timeouts" in withServer(300)(EssentialAction { req =>
      Accumulator(Sink.ignore).map(_ => Results.Ok)
    }) { port =>
      doRequests(port, trickle = 400L) must throwA[SocketException]("Broken pipe|Connection reset")
    }

    "support a separate timeout for https" in withServer(1000, httpsPort = Some(httpsPort), httpsTimeout = Some(400))(EssentialAction { req =>
      Accumulator(Sink.ignore).map(_ => Results.Ok)
    }) { port =>
      val responses = doRequests(port, trickle = 200L)
      responses.length must_== 2
      responses(0).status must_== 200
      responses(1).status must_== 200

      doRequests(httpsPort, trickle = 600L, secure = true) must throwA[SocketException]("Broken pipe|Connection reset")
    }

    "support multi-second timeouts" in withServer(1500)(EssentialAction { req =>
      Accumulator(Sink.ignore).map(_ => Results.Ok)
    }) { port =>
      doRequests(port, trickle = 1600L) must throwA[SocketException]("Broken pipe|Connection reset")
    }

    "not timeout for slow requests with a sub-second timeout" in withServer(700)(EssentialAction { req =>
      Accumulator(Sink.ignore).map(_ => Results.Ok)
    }) { port =>
      val responses = doRequests(port, trickle = 400L)
      responses.length must_== 2
      responses(0).status must_== 200
      responses(1).status must_== 200
    }

    "not timeout for slow requests with a multi-second timeout" in withServer(1500)(EssentialAction { req =>
      Accumulator(Sink.ignore).map(_ => Results.Ok)
    }) { port =>
      val responses = doRequests(port, trickle = 1000L)
      responses.length must_== 2
      responses(0).status must_== 200
      responses(1).status must_== 200
    }
  }

}