/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.test

import akka.annotation.ApiMayChange
import play.api.Application
import play.api.Configuration
import play.core.server.SelfSigned
import play.core.server.SelfSignedSSLEngineProvider
import play.core.server.ServerConfig
import play.core.server.ServerEndpoint
import play.core.server.ServerEndpoints
import play.core.server.ServerProvider

/**
 * A recipe for making a [[ServerEndpoint]]. Recipes are often used
 * when describing which tests to run. The recipe can be used to start
 * servers with the correct [[ServerEndpoint]]s.
 *
 * @see [[ServerEndpointRecipe.withEndpoint()]]
 */
@ApiMayChange sealed trait ServerEndpointRecipe {

  /** A human-readable description of this endpoint. */
  def description: String

  /** The HTTP port to use when configuring the server. */
  def configuredHttpPort: Option[Int]

  /** The HTTPS port to use when configuring the server. */
  def configuredHttpsPort: Option[Int]

  /**
   * Any extra configuration to use when configuring the server. This
   * configuration will be applied last so it will override any existing
   * configuration.
   */
  def serverConfiguration: Configuration

  /** The provider used to create the server instance. */
  def serverProvider: ServerProvider

  def withDescription(newDescription: String): ServerEndpointRecipe
  def withServerProvider(newProvider: ServerProvider): ServerEndpointRecipe

  /**
   * Once a server has been started using this recipe, the running instance
   * can be queried to create an endpoint. Usually this just involves asking
   * the server what port it is using.
   */
  def createEndpointFromServer(runningTestServer: TestServer): ServerEndpoint
}

/** Provides a recipe for making an HTTP [[ServerEndpoint]]. */
@ApiMayChange final class HttpServerEndpointRecipe(
    override val description: String,
    override val serverProvider: ServerProvider,
    extraServerConfiguration: Configuration = Configuration.empty,
    expectedHttpVersions: Set[String],
    expectedServerAttr: Option[String]
) extends ServerEndpointRecipe { recipe =>

  override val configuredHttpPort: Option[Int]    = Some(0)
  override val configuredHttpsPort: Option[Int]   = None
  override val serverConfiguration: Configuration = extraServerConfiguration

  override def createEndpointFromServer(runningServer: TestServer): ServerEndpoint = {
    ServerEndpoint(
      description = recipe.description,
      scheme = "http",
      host = "localhost",
      port = runningServer.runningHttpPort.get,
      protocols = recipe.expectedHttpVersions,
      serverAttribute = recipe.expectedServerAttr,
      ssl = None
    )
  }

  def withDescription(newDescription: String): HttpServerEndpointRecipe =
    new HttpServerEndpointRecipe(
      newDescription,
      serverProvider,
      extraServerConfiguration,
      expectedHttpVersions,
      expectedServerAttr
    )

  def withServerProvider(newProvider: ServerProvider): HttpServerEndpointRecipe =
    new HttpServerEndpointRecipe(
      description,
      newProvider,
      extraServerConfiguration,
      expectedHttpVersions,
      expectedServerAttr
    )

  override def toString: String = s"HttpServerEndpointRecipe($description)"
}

/** Provides a recipe for making an HTTPS [[ServerEndpoint]]. */
@ApiMayChange final class HttpsServerEndpointRecipe(
    override val description: String,
    override val serverProvider: ServerProvider,
    extraServerConfiguration: Configuration = Configuration.empty,
    expectedHttpVersions: Set[String],
    expectedServerAttr: Option[String]
) extends ServerEndpointRecipe { recipe =>

  override val configuredHttpPort: Option[Int]  = None
  override val configuredHttpsPort: Option[Int] = Some(0)
  override def serverConfiguration: Configuration = extraServerConfiguration.withFallback(
    Configuration(
      "play.server.https.engineProvider" -> classOf[SelfSignedSSLEngineProvider].getName
    )
  )

  override def createEndpointFromServer(runningServer: TestServer): ServerEndpoint = {
    ServerEndpoint(
      description = recipe.description,
      scheme = "https",
      host = "localhost",
      port = runningServer.runningHttpsPort.get,
      protocols = recipe.expectedHttpVersions,
      serverAttribute = recipe.expectedServerAttr,
      ssl = Some(SelfSigned.sslContext)
    )
  }

  def withDescription(newDescription: String) =
    new HttpsServerEndpointRecipe(
      newDescription,
      serverProvider,
      extraServerConfiguration,
      expectedHttpVersions,
      expectedServerAttr
    )

  def withServerProvider(newProvider: ServerProvider) =
    new HttpsServerEndpointRecipe(
      description,
      newProvider,
      extraServerConfiguration,
      expectedHttpVersions,
      expectedServerAttr
    )

  override def toString: String = s"HttpsServerEndpointRecipe($description)"
}

@ApiMayChange object ServerEndpointRecipe {

  /**
   * Starts a server by following a [[ServerEndpointRecipe]] and using the
   * application provided by an [[ApplicationFactory]]. The server's endpoint
   * is passed to the given `block` of code.
   */
  def startEndpoint[A](
      endpointRecipe: ServerEndpointRecipe,
      appFactory: ApplicationFactory
  ): (ServerEndpoint, AutoCloseable) = {
    val app: Application = appFactory.create()

    val testServerFactory = new DefaultTestServerFactory {
      override def serverConfig(app: Application): ServerConfig = {
        super
          .serverConfig(app)
          .copy(
            port = endpointRecipe.configuredHttpPort,
            sslPort = endpointRecipe.configuredHttpsPort
          )
      }

      override def overrideServerConfiguration(app: Application): Configuration =
        endpointRecipe.serverConfiguration

      override def serverProvider(app: Application): ServerProvider = endpointRecipe.serverProvider

      override def serverEndpoints(testServer: TestServer): ServerEndpoints = {
        ServerEndpoints(Seq(endpointRecipe.createEndpointFromServer(testServer)))
      }
    }

    val runningServer = testServerFactory.start(app)
    (runningServer.endpoints.endpoints.head, runningServer.stopServer)
  }

  def withEndpoint[A](endpointRecipe: ServerEndpointRecipe, appFactory: ApplicationFactory)(
      block: ServerEndpoint => A
  ): A = {
    val (endpoint, endpointCloseable) = startEndpoint(endpointRecipe, appFactory)
    try block(endpoint)
    finally endpointCloseable.close()
  }
}
