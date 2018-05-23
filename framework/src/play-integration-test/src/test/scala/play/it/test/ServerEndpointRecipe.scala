/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.test

import play.api.Configuration
import play.core.server.{ AkkaHttpServer, NettyServer, ServerProvider }
import play.it.test.HttpsEndpoint.ServerSSL

/**
 * A recipe for making a [[ServerEndpoint]]. Recipes are often used
 * when describing which tests to run. The recipe can be used to start
 * servers with the correct [[ServerEndpoint]]s.
 *
 * @see [[ServerEndpoint.withEndpoint()]]
 */
trait ServerEndpointRecipe {
  type EndpointType <: ServerEndpoint

  /** A human-readable description of this endpoint. */
  val description: String
  /** The HTTP port to use when configuring the server. */
  val configuredHttpPort: Option[Int]
  /** The HTTPS port to use when configuring the server. */
  val configuredHttpsPort: Option[Int]
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
  def createEndpointFromServer(runningTestServer: play.api.test.TestServer): EndpointType
}

/** Provides a recipe for making an [[HttpEndpoint]]. */
class HttpServerEndpointRecipe(
    override val description: String,
    override val serverProvider: ServerProvider,
    extraServerConfiguration: Configuration = Configuration.empty,
    expectedHttpVersions: Set[String],
    expectedServerAttr: Option[String]
) extends ServerEndpointRecipe {
  recipe =>
  override type EndpointType = HttpEndpoint
  override val configuredHttpPort: Option[Int] = Some(0)
  override val configuredHttpsPort: Option[Int] = None
  override val serverConfiguration: Configuration = extraServerConfiguration
  override def createEndpointFromServer(runningServer: play.api.test.TestServer): HttpEndpoint = {
    new HttpEndpoint {
      override def description: String = recipe.description
      override def port: Int = runningServer.runningHttpPort.get
      override def expectedHttpVersions: Set[String] = recipe.expectedHttpVersions
      override def expectedServerAttr: Option[String] = recipe.expectedServerAttr
    }
  }
  def withDescription(newDescription: String): HttpServerEndpointRecipe =
    new HttpServerEndpointRecipe(newDescription, serverProvider, extraServerConfiguration, expectedHttpVersions, expectedServerAttr)
  def withServerProvider(newProvider: ServerProvider): HttpServerEndpointRecipe =
    new HttpServerEndpointRecipe(description, newProvider, extraServerConfiguration, expectedHttpVersions, expectedServerAttr)
  override def toString: String = s"HttpServerEndpointRecipe($description)"
}

/** Provides a recipe for making an [[HttpsEndpoint]]. */
class HttpsServerEndpointRecipe(
    override val description: String,
    override val serverProvider: ServerProvider,
    extraServerConfiguration: Configuration = Configuration.empty,
    expectedHttpVersions: Set[String],
    expectedServerAttr: Option[String]
) extends ServerEndpointRecipe {
  recipe =>
  override type EndpointType = HttpsEndpoint
  override val configuredHttpPort: Option[Int] = None
  override val configuredHttpsPort: Option[Int] = Some(0)
  override def serverConfiguration: Configuration = Configuration(
    "play.server.https.engineProvider" -> classOf[ServerEndpoint.SelfSignedSSLEngineProvider].getName
  ) ++ extraServerConfiguration
  override def createEndpointFromServer(runningServer: play.api.test.TestServer): HttpsEndpoint = {
    new HttpsEndpoint {
      override def description: String = recipe.description
      override def port: Int = runningServer.runningHttpsPort.get
      override def expectedHttpVersions: Set[String] = recipe.expectedHttpVersions
      override def expectedServerAttr: Option[String] = recipe.expectedServerAttr
      override val serverSsl: ServerSSL = ServerSSL(
        ServerEndpoint.SelfSigned.sslContext,
        ServerEndpoint.SelfSigned.trustManager
      )
    }
  }
  def withDescription(newDescription: String) = new HttpsServerEndpointRecipe(newDescription, serverProvider, extraServerConfiguration, expectedHttpVersions, expectedServerAttr)
  def withServerProvider(newProvider: ServerProvider) = new HttpsServerEndpointRecipe(description, newProvider, extraServerConfiguration, expectedHttpVersions, expectedServerAttr)
  override def toString: String = s"HttpsServerEndpointRecipe($description)"
}

object ServerEndpointRecipe {

  private def http2Conf(enabled: Boolean): Configuration = Configuration("play.server.akka.http2.enabled" -> enabled)

  val Netty11Plaintext = new HttpServerEndpointRecipe("Netty HTTP/1.1 (plaintext)", NettyServer.provider, Configuration.empty, Set("1.0", "1.1"), Option("netty"))
  val Netty11Encrypted = new HttpsServerEndpointRecipe("Netty HTTP/1.1 (encrypted)", NettyServer.provider, Configuration.empty, Set("1.0", "1.1"), Option("netty"))
  val AkkaHttp11Plaintext = new HttpServerEndpointRecipe("Akka HTTP HTTP/1.1 (plaintext)", AkkaHttpServer.provider, http2Conf(false), Set("1.0", "1.1"), None)
  val AkkaHttp11Encrypted = new HttpsServerEndpointRecipe("Akka HTTP HTTP/1.1 (encrypted)", AkkaHttpServer.provider, http2Conf(false), Set("1.0", "1.1"), None)
  val AkkaHttp20Encrypted = new HttpsServerEndpointRecipe("Akka HTTP HTTP/2 (encrypted)", AkkaHttpServer.provider, http2Conf(true), Set("1.0", "1.1", "2"), None)

  /**
   * The list of server endpoints.
   */
  val AllRecipes: Seq[ServerEndpointRecipe] = Seq(
    Netty11Plaintext,
    Netty11Encrypted,
    AkkaHttp11Plaintext,
    AkkaHttp11Encrypted,
    AkkaHttp20Encrypted
  )
}