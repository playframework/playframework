/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.test

import play.api.Application
import play.api.libs.ws._
import play.api.mvc.Call

import WSClientResolver._

/**
 * wsCall and wsUrl methods that require an implicit port parameter.
 */
trait WSTestMethods {
  def wsClient: WSClient

  /**
   * Provides a WSRequest against client.url(s"http://localhost:" + port + call.url)
   *
   * Note that this method does not use the call.method.  If you need it, call:
   *
   * {{{
   * wsCall(call).withMethod(call.method)
   * }}}
   *
   * @param call the call relative to the server.
   * @param port the port relative to the server.
   * @return a WSRequest from WSClient
   */
  def wsCall(call: Call)(implicit port: Port): WSRequest = {
    wsUrl(call.url)
  }

  /**
   * Provides a WSRequest against client.url(s"http://localhost:" + port + path)
   *
   * @param path the relative path to call
   * @param port the port to use
   * @return a WSRequest from WSClient
   */
  def wsUrl(path: String)(implicit port: Port): WSRequest = {
    wsClient.url(s"http://localhost:" + port + path)
  }
}

/**
 * wsCall and wsUrl methods that do not require an implicit port parameter.
 */
trait WSTestMethodsWithPort {
  def wsClient: WSClient
  def wsClientPort: play.api.test.Port

  /**
   * Provides a WSRequest against client.url(s"http://localhost:" + port + call.url)
   *
   * Note that this method does not use the call.method.  If you need it, call:
   *
   * {{{
   * wsCall(call).withMethod(call.method)
   * }}}
   *
   * @param call the call relative to the server.
   * @return a WSRequest from WSClient
   */
  def wsCall(call: Call): WSRequest = {
    wsUrl(call.url)
  }

  /**
   * Provides a WSRequest against client.url(s"http://localhost:" + port + path)
   *
   * @param path the relative path to call
   * @return a WSRequest from WSClient
   */
  def wsUrl(path: String): WSRequest = {
    val methods = new WSTestMethods {
      override def wsClient: WSClient = WSTestMethodsWithPort.this.wsClient
    }
    methods.wsUrl(path)(wsClientPort)
  }
}

/**
 * Used to extend types like `new WithApplication`
 *
 * {{{
 *   new WithApplication with AppWSTestMethods {
 *
 *   }
 * }}}
 */
trait AppWSTestMethods extends WSTestMethods {
  self: HasApp =>
  import scala.language.reflectiveCalls

  def wsClient: WSClient = currentClient(self.app)
}

trait ServerWSTestMethods extends WSTestMethodsWithPort {
  self: HasAppAndPort =>
  import scala.language.reflectiveCalls

  def wsClient: WSClient = currentClient(self.app)
  def wsClientPort: play.api.test.Port = self.port
}

/**
 * Converts anything with Application and Port to an instance of WSTestMethods.
 */
trait WSTestMethodsImplicits {
  import scala.language.implicitConversions
  import scala.language.reflectiveCalls

  implicit def fromHasAppAndPort(hasAppAndPort: HasAppAndPort): WSTestMethodsWithPort = new WSTestMethodsWithPort {
    override def wsClient: WSClient = currentClient(hasAppAndPort.app)
    override def wsClientPort: Port = hasAppAndPort.port
  }

  implicit def fromHasApp(hasApp: HasApp): WSTestMethods = new WSTestMethods {
    override def wsClient: WSClient = currentClient(hasApp.app)
  }

  implicit def fromClient(client: WSClient): WSTestMethods = new WSTestMethods {
    override def wsClient: WSClient = client
  }

  implicit def fromApplication(app: Application): WSTestMethods = new WSTestMethods {
    override def wsClient: WSClient = currentClient(app)
  }

}

/**
 * A standalone test client that is useful for running functional tests.
 */
trait WsTestClient extends WSTestMethodsImplicits {

  // Use the current app as a fallback
  private def currentClient(implicit app: Application): WSClient = {
    Application.instanceCache[WSClient].apply(app)
  }

  type Port = play.api.test.Port

  /**
   * Constructs a WS request for the given reverse route.  Optionally takes a WSClient.  Note that the WS client used
   * by default requires a running Play application (use WithApplication for tests).
   *
   * For example:
   * {{{
   * "work" in new WithApplication() { implicit app =>
   *   wsCall(controllers.routes.Application.index()).get()
   * }
   * }}}
   */
  @deprecated("Please see https://playframework.com/documentation/2.6.x/WSMigration26", "2.6.0")
  def wsCall(call: Call)(implicit port: Port, client: WSClient = currentClient(play.api.Play.privateMaybeApplication.get)): WSRequest = {
    client.url(s"http://localhost:" + port + call.url)
  }

  /**
   * Constructs a WS request for the given relative URL.  Optionally takes a port and WSClient.  Note that the WS client used
   * by default requires a running Play application (use WithApplication for tests).
   */
  @deprecated("Please see https://playframework.com/documentation/2.6.x/WSMigration26", "2.6.0")
  def wsUrl(url: String)(implicit port: Port, client: WSClient = currentClient(play.api.Play.privateMaybeApplication.get)): WSRequest = {
    client.url(s"http://localhost:" + port + url)
  }

  /**
   * Run the given block of code with a client.
   *
   * The client passed to the block of code supports absolute path relative URLs passed to the url method.  If an
   * absolute path relative URL is used, the protocol is assumed to be http, the host localhost, and the port is the
   * implicit port parameter passed to this method.  This is designed to work smoothly with the Server.with* methods,
   * for example:
   *
   * {{{
   * Server.withRouter() {
   *   case GET(p"/hello/\$who") => Action(Ok("Hello " + who))
   * } { implicit port =>
   *   withClient { ws =>
   *     await(ws.url("/hello/world").get()).body must_== "Hello world"
   *   }
   * }
   * }}}
   *
   * @param block  The block of code to run
   * @param port   The port
   * @param scheme The scheme
   * @return       The result of the block of code
   */
  def withClient[T](block: WSClient => T)(implicit port: play.api.http.Port = new play.api.http.Port(-1), scheme: String = "http"): T = {
    require(scheme != null)
    val client = new WsTestClient.InternalWSClient(scheme, port)
    try {
      block(client)
    } finally {
      client.close()
    }
  }
}

object WsTestClient extends WsTestClient {

  private val singletonClient = new SingletonWSClient()

  /**
   * Creates a standalone WSClient, using its own ActorSystem and Netty thread pool.
   *
   * This client has no dependencies at all on the underlying system, but is wasteful of resources.
   *
   * @param port   the port to connect to the server on.
   * @param scheme the scheme to connect on ("http" or "https")
   */
  class InternalWSClient(scheme: String, port: play.api.http.Port) extends WSClient {

    singletonClient.addReference(this)

    def underlying[T] = singletonClient.underlying.asInstanceOf[T]

    def url(url: String): WSRequest = {
      if (url.startsWith("/") && port.value != -1) {
        singletonClient.url(s"$scheme://localhost:$port$url")
      } else {
        singletonClient.url(url)
      }
    }

    /** Closes this client, and releases underlying resources. */
    override def close(): Unit = {
      singletonClient.removeReference(this)
    }
  }

  /**
   * A singleton ws client that keeps an ActorSystem / AHC client around only when
   * it is needed.
   */
  private class SingletonWSClient extends WSClient {
    import java.util.concurrent._
    import java.util.concurrent.atomic._

    import akka.actor.{ ActorSystem, Cancellable, Terminated }
    import akka.stream.ActorMaterializer
    import play.api.libs.ws.ahc.{ AhcWSClient, AhcWSClientConfig }

    import scala.concurrent.Future
    import scala.concurrent.duration._

    private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)

    private val ref = new AtomicReference[WSClient]()

    private val count = new AtomicInteger(1)

    private val references = new ConcurrentLinkedQueue[WSClient]()

    private val idleDuration = 5.seconds

    private var idleCheckTask: Option[Cancellable] = None

    def removeReference(client: InternalWSClient) = {
      references.remove(client)
    }

    def addReference(client: InternalWSClient) = {
      references.add(client)
    }

    private def closeIdleResources(client: WSClient, system: ActorSystem): Future[Terminated] = {
      ref.set(null)
      client.close()
      system.terminate()
    }

    private def instance: WSClient = {
      val client = ref.get()
      if (client == null) {
        val result = createNewClient()
        ref.compareAndSet(null, result)
        ref.get()
      } else {
        client
      }
    }

    private def createNewClient(): WSClient = {
      val name = "ws-test-client-" + count.getAndIncrement()
      logger.warn(s"createNewClient: name = $name")

      val system = ActorSystem(name)
      implicit val materializer = ActorMaterializer(namePrefix = Some(name))(system)
      // Don't retry for tests
      val client = AhcWSClient(AhcWSClientConfig(maxRequestRetry = 0))
      scheduleIdleCheck(client, system)
      client
    }

    private def scheduleIdleCheck(client: WSClient, system: ActorSystem) = {
      val scheduler = system.scheduler
      idleCheckTask match {
        case Some(cancellable) =>
          // Something else got here first...
          logger.error(s"scheduleIdleCheck: looks like a race condition of WsTestClient...")
          closeIdleResources(client, system)

        case None =>
          //
          idleCheckTask = Option(scheduler.schedule(initialDelay = idleDuration, interval = idleDuration) {
            if (references.size() == 0) {
              logger.debug(s"check: no references found on client $client, system $system")
              idleCheckTask.map(_.cancel())
              idleCheckTask = None
              closeIdleResources(client, system)
            } else {
              logger.debug(s"check: client references = ${references.toArray.toSeq}")
            }
          }(system.dispatcher))
      }
    }

    /**
     * The underlying implementation of the client, if any.  You must cast explicitly to the type you want.
     *
     * @tparam T the type you are expecting (i.e. isInstanceOf)
     * @return the backing class.
     */
    override def underlying[T]: T = instance.underlying

    /**
     * Generates a request holder which can be used to build requests.
     *
     * @param url The base URL to make HTTP requests to.
     * @return a WSRequestHolder
     */
    override def url(url: String): WSRequest = instance.url(url)

    override def close(): Unit = {}
  }

}

private[test] object WSClientResolver {
  def currentClient(app: Application): WSClient = {
    // intentionally does not use Application.instanceCache
    app.injector.instanceOf[WSClient]
  }
}

