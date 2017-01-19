package play.api.libs.ws.ahc

import akka.stream.Materializer
import play.api._
import play.api.inject.ApplicationLifecycle
import play.api.libs.ws.WSClient

/**
 * AsyncHttpClient WS API implementation components.
 */
trait AhcWSComponents {

  def environment: Environment

  def configuration: Configuration

  def applicationLifecycle: ApplicationLifecycle

  def materializer: Materializer

  lazy val wsClient: WSClient = {
    implicit val mat = materializer
    val asyncHttpClient = new AsyncHttpClientProvider(configuration, environment, applicationLifecycle).get
    new WSClientProvider(asyncHttpClient).get
  }
}
