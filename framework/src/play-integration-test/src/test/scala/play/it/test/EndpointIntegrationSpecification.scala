/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.test

import org.specs2.execute.{ AsResult, PendingUntilFixed, Result, ResultExecution }
import org.specs2.mutable.SpecificationLike
import org.specs2.specification.core.Fragment
import play.api.Configuration
import play.core.server._

/**
 * Mixin class for integration tests that want to run over different
 * backend servers and protocols.
 *
 * @see [[ServerEndpoints]]
 */
trait EndpointIntegrationSpecification
    extends SpecificationLike with PendingUntilFixed
    with ServerEndpoints with ApplicationFactories {

  private def http2Conf(enabled: Boolean): Configuration = Configuration("play.server.akka.http2.enabled" -> enabled)

  private val Netty11Plaintext = new HttpServerEndpointRecipe("Netty HTTP/1.1 (plaintext)", NettyServer.provider, Configuration.empty, Set("1.0", "1.1"), Option("netty"))
  private val Netty11Encrypted = new HttpsServerEndpointRecipe("Netty HTTP/1.1 (encrypted)", NettyServer.provider, Configuration.empty, Set("1.0", "1.1"), Option("netty"))
  private val AkkaHttp11Plaintext = new HttpServerEndpointRecipe("Akka HTTP HTTP/1.1 (plaintext)", AkkaHttpServer.provider, http2Conf(false), Set("1.0", "1.1"), None)
  private val AkkaHttp11Encrypted = new HttpsServerEndpointRecipe("Akka HTTP HTTP/1.1 (encrypted)", AkkaHttpServer.provider, http2Conf(false), Set("1.0", "1.1"), None)
  private val AkkaHttp20Encrypted = new HttpsServerEndpointRecipe("Akka HTTP HTTP/2 (encrypted)", AkkaHttpServer.provider, http2Conf(true), Set("1.0", "1.1", "2"), None)

  /**
   * The list of server endpoints supported by this specification.
   */
  private val allEndpointRecipes: Seq[ServerEndpointRecipe] = Seq(
    Netty11Plaintext,
    Netty11Encrypted,
    AkkaHttp11Plaintext,
    AkkaHttp11Encrypted,
    AkkaHttp20Encrypted
  )

  /**
   * Implicit class that enhances [[ApplicationFactory]] with the [[withAllEndpoints()]] method.
   */
  implicit class ApplicationFactoryEndpointBaker(val appFactory: ApplicationFactory) {
    /**
     * Helper that creates a specs2 fragment for the given server endpoints.
     * Each fragment creates an application, starts a server
     * and runs the given block of code.
     *
     * {{{
     * withResult(Results.Ok("Hello")) withAllEndpoints { endpoint: ServerEndpoint =>
     *   val response = ... connect to endpoint.port ...
     *   response.status must_== 200
     * }
     * }}}
     */
    def withEndpoints[A: AsResult](endpoints: Seq[ServerEndpointRecipe])(block: ServerEndpoint => A): Fragment = {
      endpoints.map { endpointRecipe: ServerEndpointRecipe =>
        s"with ${endpointRecipe.description}" >> {
          withEndpoint(endpointRecipe, appFactory)(block)
        }
      }.last
    }

    /**
     * Helper that creates a specs2 fragment for all the server endpoints supported
     * by Play. Each fragment creates an application, starts a server
     * and runs the given block of code.
     *
     * {{{
     * withResult(Results.Ok("Hello")) withAllEndpoints { endpoint: ServerEndpoint =>
     *   val response = ... connect to endpoint.port ...
     *   response.status must_== 200
     * }
     * }}}
     */
    def withAllEndpoints[A: AsResult](block: ServerEndpoint => A): Fragment =
      withEndpoints(allEndpointRecipes)(block)
  }

  /**
   * Implicit class that enhances code blocks with some `pendingUntilFixed`-style methods.
   */
  implicit class EndpointsPendingUntilFixed[T: AsResult](block: => T) {
    /**
     * Same as `pendingUntilFixed`, but only if a condition is met.
     * Otherwise the test executes as normal.
     */
    private def conditionalPendingUntilFixed(pendingCondition: => Boolean): Result = {
      if (pendingCondition) {
        block.pendingUntilFixed
      } else {
        ResultExecution.execute(AsResult(block))
      }
    }

    /**
     * If this is an HTTP/2 endpoint then expect the test to fail, and mark it
     * as *pending*. However, if the test passes, then this is a failure to
     * indicate that the test is no longer pending a fix.
     */
    def pendingUntilHttp2Fixed(endpoint: ServerEndpoint): Result = {
      conditionalPendingUntilFixed(endpoint.expectedHttpVersions.contains("2"))
    }
  }

}