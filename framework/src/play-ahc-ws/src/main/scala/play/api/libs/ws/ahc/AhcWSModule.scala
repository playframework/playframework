package play.api.libs.ws.ahc

import play.shaded.ahc.org.asynchttpclient.AsyncHttpClient
import play.api.inject.SimpleModule
import play.api.inject.bind
import play.api.libs.ws._

/**
 * A Play binding for the Scala WS API to the AsyncHTTPClient implementation.
 */
class AhcWSModule extends SimpleModule(
  bind[AsyncHttpClient].toProvider[AsyncHttpClientProvider],
  bind[WSClient].toProvider[WSClientProvider]
)
