package play.api.libs.ws.ahc

import javax.inject.{ Inject, Provider, Singleton }

import akka.stream.Materializer
import com.typesafe.sslconfig.ssl.SystemConfiguration
import com.typesafe.sslconfig.ssl.debug.DebugConfiguration
import play.api.inject.ApplicationLifecycle
import play.api.libs.ws.{ WSClient, WSClientConfig, WSConfigParser }
import play.api.{ Configuration, Environment }
import play.shaded.ahc.org.asynchttpclient.{ AsyncHttpClient, DefaultAsyncHttpClient }

import scala.concurrent.Future

/**
 * Provides an instance of AsyncHttpClient configured from the Configuration object.
 *
 * @param configuration the Play configuration
 * @param environment the Play environment
 * @param applicationLifecycle app lifecycle, instance is closed automatically.
 */
@Singleton
class AsyncHttpClientProvider @Inject() (
    configuration: Configuration,
    environment: Environment,
    applicationLifecycle: ApplicationLifecycle) extends Provider[AsyncHttpClient] {

  private val wsClientConfig: WSClientConfig = {
    new WSConfigParser(configuration.underlying, environment.classLoader).parse()
  }

  private val ahcWsClientConfig: AhcWSClientConfig = {
    new AhcWSClientConfigParser(wsClientConfig, configuration.underlying, environment.classLoader).parse()
  }

  private val asyncHttpClientConfig = new AhcConfigBuilder(ahcWsClientConfig).build()

  private def configure(): Unit = {
    // JSSE depends on various system properties which must be set before JSSE classes
    // are pulled into memory, so these must come first.
    val loggerFactory = StandaloneAhcWSClient.loggerFactory
    if (wsClientConfig.ssl.debug.enabled) {
      new DebugConfiguration(loggerFactory).configure(wsClientConfig.ssl.debug)
    }
    new SystemConfiguration(loggerFactory).configure(wsClientConfig.ssl)
  }

  lazy val get: AsyncHttpClient = {
    configure()
    new DefaultAsyncHttpClient(asyncHttpClientConfig)
  }

  // Always close the AsyncHttpClient afterwards.
  applicationLifecycle.addStopHook(() =>
    Future.successful(get.close())
  )
}

@Singleton
class WSClientProvider @Inject() (asyncHttpClient: AsyncHttpClient)(implicit materializer: Materializer)
    extends Provider[WSClient] {

  lazy val get: WSClient = {
    new AhcWSClient(new StandaloneAhcWSClient(asyncHttpClient))
  }
}
