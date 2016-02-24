/*
  * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.ws.ning

import akka.stream.Materializer
import org.asynchttpclient._
import play.api._
import play.api.inject.ApplicationLifecycle
import play.api.libs.ws._
import play.api.libs.ws.ahc.{ AhcWSClientConfigParser, AhcWSAPI, AhcWSClient, AhcWSRequest }
import play.api.libs.ws.ssl._

/**
 * A WS client backed by a Ning AsyncHttpClient.
 *
 * If you need to debug Ning, set logger.com.ning.http.client=DEBUG in your application.conf file.
 *
 * @param config a client configuration object
 */
@deprecated("Use AhcWSClient instead", "2.5")
case class NingWSClient(config: AsyncHttpClientConfig)(implicit materializer: Materializer) extends WSClient {

  private val ahcWsClient = AhcWSClient(config)

  def underlying[T]: T = ahcWsClient.underlying

  private[libs] def executeRequest[T](request: Request, handler: AsyncHandler[T]): ListenableFuture[T] = ahcWsClient.executeRequest(request, handler)

  def close(): Unit = ahcWsClient.close()

  def url(url: String): WSRequest = AhcWSRequest(ahcWsClient, url, "GET", EmptyBody, Map(), Map(), None, None, None, None, None, None, None)
}

@deprecated("Use AhcWSClient instead", "2.5")
object NingWSClient {
  /**
   * Convenient factory method that uses a [[WSClientConfig]] value for configuration instead of an [[https://asynchttpclient.github.io/async-http-client/apidocs/com/ning/http/client/AsyncHttpClientConfig.html org.asynchttpclient.AsyncHttpClientConfig]].
   *
   * Typical usage:
   *
   * {{{
   *   val client = NingWSClient()
   *   val request = client.url(someUrl).get()
   *   request.foreach { response =>
   *     doSomething(response)
   *     client.close()
   *   }
   * }}}
   *
   * @param config configuration settings
   */
  def apply(config: NingWSClientConfig = NingWSClientConfig())(implicit materializer: Materializer): NingWSClient = {
    val client = new NingWSClient(new NingAsyncHttpClientConfigBuilder(config).build())
    new SystemConfiguration().configure(config.wsClientConfig)
    client
  }
}

/**
 * Ning WS API implementation components.
 */
@deprecated("Use AhcWSClient instead", "2.5")
trait NingWSComponents {

  def environment: Environment
  def configuration: Configuration
  def applicationLifecycle: ApplicationLifecycle
  def materializer: Materializer

  lazy val wsClientConfig: WSClientConfig = new WSConfigParser(configuration, environment).parse()
  private lazy val ahcWsClientConfig = new AhcWSClientConfigParser(wsClientConfig, configuration, environment).parse()
  lazy val ningWsClientConfig: NingWSClientConfig =
    NingWSClientConfig(
      wsClientConfig = wsClientConfig,
      maxConnectionsPerHost = ahcWsClientConfig.maxConnectionsPerHost,
      maxConnectionsTotal = ahcWsClientConfig.maxConnectionsTotal,
      maxConnectionLifetime = ahcWsClientConfig.maxConnectionLifetime,
      idleConnectionInPoolTimeout = ahcWsClientConfig.idleConnectionInPoolTimeout,
      maxNumberOfRedirects = ahcWsClientConfig.maxNumberOfRedirects,
      maxRequestRetry = ahcWsClientConfig.maxRequestRetry,
      disableUrlEncoding = ahcWsClientConfig.disableUrlEncoding,
      keepAlive = ahcWsClientConfig.keepAlive
    )

  lazy val wsApi: WSAPI = new AhcWSAPI(environment, ahcWsClientConfig, applicationLifecycle)(materializer)
  lazy val wsClient: WSClient = wsApi.client
}
