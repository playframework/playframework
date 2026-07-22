/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http

import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine

import play.api.mvc.Results
import play.api.test.ApplicationFactories
import play.api.test.PlaySpecification
import play.api.test.ServerEndpointRecipe
import play.core.server.SelfSigned
import play.it.test.EndpointIntegrationSpecification
import play.it.test.NettyServerEndpointRecipes
import play.it.test.OkHttpEndpointSupport
import play.it.test.PekkoHttpServerEndpointRecipes
import play.server.api.SSLEngineProvider

object RecordingSSLEngineProvider {
  private val providersCreated = new AtomicInteger
  private val enginesCreated   = new AtomicInteger

  def reset(): Unit = {
    providersCreated.set(0)
    enginesCreated.set(0)
  }

  def recordProviderCreated(): Unit = providersCreated.incrementAndGet()
  def recordEngineCreated(): Unit   = enginesCreated.incrementAndGet()

  def providerCount: Int = providersCreated.get()
  def engineCount: Int   = enginesCreated.get()
}

final class RecordingSSLEngineProvider extends SSLEngineProvider {
  RecordingSSLEngineProvider.recordProviderCreated()

  override def createSSLEngine: SSLEngine = {
    RecordingSSLEngineProvider.recordEngineCreated()
    sslContext.createSSLEngine()
  }

  override def sslContext: SSLContext = SelfSigned.sslContext
}

class SSLEngineProviderSpec
    extends PlaySpecification
    with EndpointIntegrationSpecification
    with OkHttpEndpointSupport
    with ApplicationFactories {

  sequential

  private val endpoints: Seq[ServerEndpointRecipe] = Seq(
    NettyServerEndpointRecipes.Netty11Encrypted,
    PekkoHttpServerEndpointRecipes.PekkoHttp11Encrypted
  ).map(
    _.withExtraServerConfiguration(
      Map("play.server.https.engineProvider" -> classOf[RecordingSSLEngineProvider].getName)
    )
  )

  private val application = withAction { Action =>
    Action(Results.Ok)
  }

  "Play HTTPS servers" should {
    endpoints.map { endpointRecipe =>
      s"create each engine with the configured provider for ${endpointRecipe.description}" in {
        RecordingSSLEngineProvider.reset()

        ServerEndpointRecipe.withEndpoint(endpointRecipe, application) { endpoint =>
          withOkHttpEndpoint(endpoint) { okEndpoint =>
            val responseCodes = (1 to 2).map { _ =>
              val response = okEndpoint.configuredCall("/")(_.header("Connection", "close"))
              try response.code
              finally response.close()
            }

            (responseCodes must contain(exactly(200, 200)))
              .and(RecordingSSLEngineProvider.providerCount must_== 1)
              .and(RecordingSSLEngineProvider.engineCount must be_>=(2))
          }
        }
      }
    }.last
  }
}
