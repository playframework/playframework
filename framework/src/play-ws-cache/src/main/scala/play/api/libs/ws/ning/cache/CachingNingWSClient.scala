package play.api.libs.ws.ning.cache

import com.ning.http.client._
import com.ning.http.client.providers.netty.NettyAsyncHttpProvider
import play.api.libs.ws.ning.{ NingAsyncHttpClientConfigBuilder, NingWSClientConfig, NingWSClient, NingWSRequest }
import play.api.libs.ws.ssl.SystemConfiguration
import play.api.libs.ws.{ EmptyBody, WSRequest }

/**
 * A Ning WS Client with built in caching.
 */
class CachingNingWSClient(config: AsyncHttpClientConfig, c: NingWSCache) extends NingWSClient(config) {

  private val asyncHttpClient: AsyncHttpClient = {
    val httpProvider = new NettyAsyncHttpProvider(config)
    val cacheProvider = new CacheAsyncHttpProvider(config, httpProvider, c)
    new AsyncHttpClient(cacheProvider, config)
  }

  override def underlying[T] = asyncHttpClient.asInstanceOf[T]

  override private[libs] def executeRequest[T](request: Request, handler: AsyncHandler[T]): ListenableFuture[T] = asyncHttpClient.executeRequest(request, handler)

  override def close() = asyncHttpClient.close()

  override def url(url: String): WSRequest = NingWSRequest(this, url, "GET", EmptyBody, Map(), Map(), None, None, None, None, None, None, None)
}

object CachingNingWSClient {
  /**
   * Convenient factory method that uses a [[WSClientConfig]] value for configuration instead of an [[AsyncHttpClientConfig]].
   *
   * Typical usage:
   *
   * {{{
   *   val client = CachingNingWSClient()
   *   val request = client.url(someUrl).get()
   *   request.foreach { response =>
   *     doSomething(response)
   *     client.close()
   *   }
   * }}}
   *
   * @param config configuration settings
   */
  def apply(config: NingWSClientConfig = NingWSClientConfig()): CachingNingWSClient = {
    val asyncClientConfig = new NingAsyncHttpClientConfigBuilder(config).build()
    val ningCache = NingWSCache(asyncClientConfig)
    val client = new CachingNingWSClient(asyncClientConfig, ningCache)
    new SystemConfiguration().configure(config.wsClientConfig)
    client
  }
}
