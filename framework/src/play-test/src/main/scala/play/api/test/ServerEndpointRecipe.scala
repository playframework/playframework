/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.test

import akka.annotation.ApiMayChange

import play.api.{ Application, Configuration }
import play.core.server.ServerEndpoint.ClientSsl
import play.core.server.{ AkkaHttpServer, NettyServer, SelfSigned, SelfSignedSSLEngineProvider, ServerConfig, ServerEndpoint, ServerEndpoints, ServerProvider }

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

  override val configuredHttpPort: Option[Int] = Some(0)
  override val configuredHttpsPort: Option[Int] = None
  override val serverConfiguration: Configuration = extraServerConfiguration

  override def createEndpointFromServer(runningServer: TestServer): ServerEndpoint = {
    ServerEndpoint(
      description = recipe.description,
      scheme = "http",
      host = "localhost",
      port = runningServer.runningHttpPort.get,
      expectedHttpVersions = recipe.expectedHttpVersions,
      expectedServerAttr = recipe.expectedServerAttr,
      ssl = None
    )
  }

  def withDescription(newDescription: String): HttpServerEndpointRecipe =
    new HttpServerEndpointRecipe(newDescription, serverProvider, extraServerConfiguration, expectedHttpVersions, expectedServerAttr)
  def withServerProvider(newProvider: ServerProvider): HttpServerEndpointRecipe =
    new HttpServerEndpointRecipe(description, newProvider, extraServerConfiguration, expectedHttpVersions, expectedServerAttr)
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

  override val configuredHttpPort: Option[Int] = None
  override val configuredHttpsPort: Option[Int] = Some(0)
  override def serverConfiguration: Configuration = Configuration(
    "play.server.https.engineProvider" -> classOf[SelfSignedSSLEngineProvider].getName
  ) ++ extraServerConfiguration

  override def createEndpointFromServer(runningServer: TestServer): ServerEndpoint = {
    ServerEndpoint(
      description = recipe.description,
      scheme = "https",
      host = "localhost",
      port = runningServer.runningHttpsPort.get,
      expectedHttpVersions = recipe.expectedHttpVersions,
      expectedServerAttr = recipe.expectedServerAttr,
      ssl = Some(ClientSsl(
        SelfSigned.sslContext,
        SelfSigned.trustManager
      ))
    )
  }

  def withDescription(newDescription: String) = new HttpsServerEndpointRecipe(newDescription, serverProvider, extraServerConfiguration, expectedHttpVersions, expectedServerAttr)
  def withServerProvider(newProvider: ServerProvider) = new HttpsServerEndpointRecipe(description, newProvider, extraServerConfiguration, expectedHttpVersions, expectedServerAttr)
  override def toString: String = s"HttpsServerEndpointRecipe($description)"
}

@ApiMayChange object ServerEndpointRecipe {

  private def http2Conf(enabled: Boolean, alwaysForInsecure: Boolean = false): Configuration = Configuration(
    "play.server.akka.http2.enabled" -> enabled,
    "play.server.akka.http2.alwaysForInsecure" -> alwaysForInsecure
  )

  val Netty11Plaintext = new HttpServerEndpointRecipe("Netty HTTP/1.1 (plaintext)", NettyServer.provider, Configuration.empty, Set("1.0", "1.1"), Option("netty"))
  val Netty11Encrypted = new HttpsServerEndpointRecipe("Netty HTTP/1.1 (encrypted)", NettyServer.provider, Configuration.empty, Set("1.0", "1.1"), Option("netty"))
  val AkkaHttp11Plaintext = new HttpServerEndpointRecipe("Akka HTTP HTTP/1.1 (plaintext)", AkkaHttpServer.provider, http2Conf(false), Set("1.0", "1.1"), None)
  val AkkaHttp11Encrypted = new HttpsServerEndpointRecipe("Akka HTTP HTTP/1.1 (encrypted)", AkkaHttpServer.provider, http2Conf(false), Set("1.0", "1.1"), None)
  @ApiMayChange
  val AkkaHttp20Plaintext = new HttpServerEndpointRecipe("Akka HTTP HTTP/2 (plaintext)", AkkaHttpServer.provider, http2Conf(enabled = true, alwaysForInsecure = true), Set("2"), None)
  val AkkaHttp20Encrypted = new HttpsServerEndpointRecipe("Akka HTTP HTTP/2 (encrypted)", AkkaHttpServer.provider, http2Conf(enabled = true), Set("1.0", "1.1", "2"), None)

  /**
   * All non-experimental server endpoint recipes.
   */
  val AllRecipes: Seq[ServerEndpointRecipe] = Seq(
    Netty11Plaintext,
    Netty11Encrypted,
    AkkaHttp11Plaintext,
    AkkaHttp11Encrypted,
    AkkaHttp20Encrypted
  )

  /**
   * All server endpoint recipes including experimental.
   */
  @ApiMayChange
  val AllRecipesIncludingExperimental: Seq[ServerEndpointRecipe] = AllRecipes :+ AkkaHttp20Plaintext
  /**
   * Starts a server by following a [[ServerEndpointRecipe]] and using the
   * application provided by an [[ApplicationFactory]]. The server's endpoint
   * is passed to the given `block` of code.
   */
  def startEndpoint[A](endpointRecipe: ServerEndpointRecipe, appFactory: ApplicationFactory): (ServerEndpoint, AutoCloseable) = {
    val app: Application = appFactory.create()

    val testServerFactory = new DefaultTestServerFactory {
      override def serverConfig(app: Application) = {
        super.serverConfig(app).copy(
          port = endpointRecipe.configuredHttpPort,
          sslPort = endpointRecipe.configuredHttpsPort
        )
      }

      override def overrideServerConfiguration(app: Application) =
        endpointRecipe.serverConfiguration

      override def serverProvider(app: Application) = endpointRecipe.serverProvider

      override def serverEndpoints(testServer: TestServer) = {
        ServerEndpoints(Seq(endpointRecipe.createEndpointFromServer(testServer)))
      }
    }

    val runningServer = testServerFactory.start(app)
    (runningServer.endpoints.endpoints.head, runningServer.stopServer)
  }

  def withEndpoint[A](endpointRecipe: ServerEndpointRecipe, appFactory: ApplicationFactory)(block: ServerEndpoint => A): A = {
    val (endpoint, endpointCloseable) = startEndpoint(endpointRecipe, appFactory)
    try block(endpoint) finally endpointCloseable.close()
  }

}
