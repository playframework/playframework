package play.api.libs.ws.ahc

import play.shaded.ahc.org.asynchttpclient.AsyncHttpClient
import play.api.inject.SimpleModule
import play.api.inject.bind
import play.api.libs.ws._

/**
 *
 */
class AhcWSModule extends SimpleModule(
  bind[AsyncHttpClient].toProvider[AsyncHttpClientProvider],
  bind[StandaloneAhcWSClient].toProvider[StandaloneAhcWSClientProvider],
  bind[WSClient].toProvider[WSClientProvider]
)
