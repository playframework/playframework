package play.api.libs.ws.ning.cache

import com.ning.http.client.{ RequestBuilder, Request, FluentCaseInsensitiveStringsMap }
import play.api.libs.ws.ning.NingAsyncHttpClientConfigBuilder

/**
 * Utility methods to make building requests and responses easier.
 */
trait NingBuilderMethods {

  def generateCache: NingWSCache = {
    val builder = new NingAsyncHttpClientConfigBuilder()
    NingWSCache(builder.build())
  }

  def generateRequest(url: String)(block: FluentCaseInsensitiveStringsMap => FluentCaseInsensitiveStringsMap): Request = {
    val requestBuilder = new RequestBuilder()
    val requestHeaders = block(new FluentCaseInsensitiveStringsMap())

    requestBuilder
      .setUrl(url)
      .setHeaders(requestHeaders)
      .build
  }

}
