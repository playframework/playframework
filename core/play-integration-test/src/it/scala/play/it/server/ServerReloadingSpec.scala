/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.server

import akka.stream.Materializer
import javax.inject.Inject
import javax.inject.Provider
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.concurrent.ActorSystemProvider
import play.api.libs.ws.WSClient
import play.api.mvc.DefaultActionBuilder
import play.api.mvc.Request
import play.api.mvc.Results
import play.api.routing.Router
import play.api.routing.sird._
import play.api.test.PlaySpecification
import play.api.test.WsTestClient
import play.api.Application
import play.api.Configuration
import play.core.ApplicationProvider
import play.core.server.common.ServerDebugInfo
import play.core.server.ServerConfig
import play.core.server.ServerProvider
import play.it.AkkaHttpIntegrationSpecification
import play.it.NettyIntegrationSpecification
import play.it.ServerIntegrationSpecification

import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try

class NettyServerReloadingSpec extends ServerReloadingSpec with NettyIntegrationSpecification
class AkkaServerReloadingSpec  extends ServerReloadingSpec with AkkaHttpIntegrationSpecification

trait ServerReloadingSpec extends PlaySpecification with WsTestClient with ServerIntegrationSpecification {
  class TestApplicationProvider extends ApplicationProvider {
    @volatile private var app: Option[Try[Application]] = None
    def provide(newApp: Try[Application]): Unit         = app = Some(newApp)
    override def get: Try[Application]                  = app.get
  }

  def withApplicationProvider[A](ap: ApplicationProvider)(block: Port => A): A = {
    val classLoader = Thread.currentThread.getContextClassLoader
    val configuration =
      Configuration.load(classLoader, System.getProperties, Map.empty, allowMissingApplicationConf = true)
    val actorSystem  = ActorSystemProvider.start(classLoader, configuration)
    val materializer = Materializer.matFromSystem(actorSystem)

    val server = integrationServerProvider.createServer(
      ServerProvider.Context(
        ServerConfig(port = Some(0)),
        ap,
        actorSystem,
        materializer,
        () => Future.successful(())
      )
    )
    val port: Port = server.httpPort.get

    try block(port)
    finally {
      server.stop()
    }
  }

  "Server reloading" should {
    "update its flash cookie secret on reloading" in {
      // Test for https://github.com/playframework/playframework/issues/7533

      val testAppProvider = new TestApplicationProvider
      withApplicationProvider(testAppProvider) { implicit port: Port => // First we make a request to the server. This tries to load the application
        // but fails because we set our TestApplicationProvider to contain a Failure
        // instead of an Application. The server can't load the Application configuration
        // yet, so it loads some default flash configuration.

        testAppProvider.provide(Failure(new Exception))
        val res1 = await(wsUrl("/").get())
        res1.status must_== 500

        // Now we update the TestApplicationProvider with a working Application.
        // Then we make a request to the application to check that the Server has
        // reloaded the flash configuration properly. The FlashTestRouterProvider
        // has the logic for setting and reading the flash value.

        val application = GuiceApplicationBuilder()
          .configure("play.ws.ahc.useCookieStore" -> "true") // to preserve cookies between requests
          .overrides(bind[Router].toProvider[ServerReloadingSpec.TestRouterProvider])
          .build()

        // This client producer is created based on the application above, which configures
        // the use of cookie store to "true".
        val persistentCookiesClientProducer: (Port, String) => WSClient = { (port, scheme) =>
          application.injector.instanceOf[WSClient]
        }

        testAppProvider.provide(Success(application))

        val res2 = await(
          wsUrl("/setflash")(client = persistentCookiesClientProducer, port = port).withFollowRedirects(true).get()
        )
        res2.status must_== 200
        res2.body must_== "Some(bar)"
      }
    }

    "update its forwarding configuration on reloading" in {
      val testAppProvider = new TestApplicationProvider
      withApplicationProvider(testAppProvider) { implicit port: Port => // First we make a request to the server when the application
      // cannot be loaded. This may cause the server to load the configuration.

      {
        testAppProvider.provide(Failure(new Exception))
        val response = await(wsUrl("/getremoteaddress").get())
        response.status must_== 500
      }

      // Now we update the TestApplicationProvider with a working Application.
      // We check that the server uses the default forwarding configuration.

      {
        testAppProvider.provide(
          Success(
            GuiceApplicationBuilder()
              .overrides(bind[Router].toProvider[ServerReloadingSpec.TestRouterProvider])
              .build()
          )
        )

        val noHeaderResponse = await {
          wsUrl("/getremoteaddress").get()
        }
        noHeaderResponse.status must_== 200
        noHeaderResponse.body must_== "127.0.0.1"

        val xForwardedHeaderResponse = await {
          wsUrl("/getremoteaddress")
            .withHttpHeaders("X-Forwarded-For" -> "192.0.2.43, ::1, 127.0.0.1, [::1]")
            .get()
        }
        xForwardedHeaderResponse.status must_== 200
        xForwardedHeaderResponse.body must_== "192.0.2.43"

        val forwardedHeaderResponse = await {
          wsUrl("/getremoteaddress")
            .withHttpHeaders("Forwarded" -> "for=192.0.2.43;proto=https, for=\"[::1]\"")
            .get()
        }
        forwardedHeaderResponse.status must_== 200
        forwardedHeaderResponse.body must_== "127.0.0.1"
      }

      // Now we update the TestApplicationProvider with a second working Application,
      // this time with different forwarding configuration.

      {
        testAppProvider.provide(
          Success(
            GuiceApplicationBuilder()
              .configure("play.http.forwarded.version" -> "rfc7239")
              .overrides(bind[Router].toProvider[ServerReloadingSpec.TestRouterProvider])
              .build()
          )
        )

        val noHeaderResponse = await {
          wsUrl("/getremoteaddress").get()
        }
        noHeaderResponse.status must_== 200
        noHeaderResponse.body must_== "127.0.0.1"

        val xForwardedHeaderResponse = await {
          wsUrl("/getremoteaddress")
            .withHttpHeaders("X-Forwarded-For" -> "192.0.2.43, ::1, 127.0.0.1, [::1]")
            .get()
        }
        xForwardedHeaderResponse.status must_== 200
        xForwardedHeaderResponse.body must_== "127.0.0.1"

        val forwardedHeaderResponse = await {
          wsUrl("/getremoteaddress")
            .withHttpHeaders("Forwarded" -> "for=192.0.2.43;proto=https, for=\"[::1]\"")
            .get()
        }
        forwardedHeaderResponse.status must_== 200
        forwardedHeaderResponse.body must_== "192.0.2.43"
      }
      }
    }

    "only reload its configuration when the application changes" in {
      val testAppProvider = new TestApplicationProvider
      withApplicationProvider(testAppProvider) { implicit port: Port =>
        def appWithConfig(conf: (String, Any)*): Success[Application] = {
          Success(
            GuiceApplicationBuilder()
              .configure(conf: _*)
              .overrides(bind[Router].toProvider[ServerReloadingSpec.TestRouterProvider])
              .build()
          )
        }

        val app1 = appWithConfig("play.server.debug.addDebugInfoToRequests" -> true)
        testAppProvider.provide(app1)
        await(wsUrl("/getserverconfigcachereloads").get()).body must_== "Some(1)"
        await(wsUrl("/getserverconfigcachereloads").get()).body must_== "Some(1)"

        val app2 = Failure(new Exception())
        testAppProvider.provide(app2)
        await(wsUrl("/getserverconfigcachereloads").get()).status must_== 500
        await(wsUrl("/getserverconfigcachereloads").get()).status must_== 500

        val app3 = appWithConfig("play.server.debug.addDebugInfoToRequests" -> true)
        testAppProvider.provide(app3)
        await(wsUrl("/getserverconfigcachereloads").get()).body must_== "Some(3)"
        await(wsUrl("/getserverconfigcachereloads").get()).body must_== "Some(3)"

        val app4 = appWithConfig()
        testAppProvider.provide(app4)
        await(wsUrl("/getserverconfigcachereloads").get()).body must_== "None"
        await(wsUrl("/getserverconfigcachereloads").get()).body must_== "None"

        val app5 = appWithConfig("play.server.debug.addDebugInfoToRequests" -> true)
        testAppProvider.provide(app5)
        await(wsUrl("/getserverconfigcachereloads").get()).body must_== "Some(5)"
        await(wsUrl("/getserverconfigcachereloads").get()).body must_== "Some(5)"

        val app6 = Failure(new Exception())
        testAppProvider.provide(app6)
        await(wsUrl("/getserverconfigcachereloads").get()).status must_== 500
        await(wsUrl("/getserverconfigcachereloads").get()).status must_== 500

        val app7 = appWithConfig("play.server.debug.addDebugInfoToRequests" -> true)
        testAppProvider.provide(app7)
        await(wsUrl("/getserverconfigcachereloads").get()).body must_== "Some(7)"
        await(wsUrl("/getserverconfigcachereloads").get()).body must_== "Some(7)"
      }
    }
  }
}

private[server] object ServerReloadingSpec {

  /**
   * The router for an application to help test server reloading.
   */
  class TestRouterProvider @Inject() (action: DefaultActionBuilder) extends Provider[Router] {
    override lazy val get: Router = Router.from {
      case GET(p"/setflash") =>
        action {
          Results.Redirect("/getflash").flashing("foo" -> "bar")
        }
      case GET(p"/getflash") =>
        action { request: Request[_] =>
          Results.Ok(request.flash.data.get("foo").toString)
        }
      case GET(p"/getremoteaddress") =>
        action { request: Request[_] =>
          Results.Ok(request.remoteAddress)
        }
      case GET(p"/getserverconfigcachereloads") =>
        action { request: Request[_] =>
          val reloadCount: Option[Int] = request.attrs.get(ServerDebugInfo.Attr).map(_.serverConfigCacheReloads)
          Results.Ok(reloadCount.toString)
        }
    }
  }
}
