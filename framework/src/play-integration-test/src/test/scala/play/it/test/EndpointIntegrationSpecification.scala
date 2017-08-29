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

  /**
   * The list of server endpoints supported by this specification.
   * @return
   */
  def allEndpointRecipes: Seq[ServerEndpointRecipe]

  /**
   * Implicit class that enhances [[ApplicationFactory]] with the [[withAllEndpoints()]] method.
   */
  implicit class ApplicationFactoryEndpointBaker(val appFactory: ApplicationFactory) {
    /**
     * Helper that creates a specs2 fragment for the server endpoints given in
     * [[allEndpointRecipes]]. Each fragment creates an application, starts a server
     * and runs the given block of code.
     *
     * {{{
     * withResult(Results.Ok("Hello")) withAllEndpoints { endpoint: ServerEndpoint =>
     *   val response = ... connect to endpoint.port ...
     *   response.status must_== 200
     * }
     * }}}
     */
    def withAllEndpoints[A: AsResult](block: ServerEndpoint => A): Fragment = {
      allEndpointRecipes.map { endpointRecipe: ServerEndpointRecipe =>
        s"with ${endpointRecipe.description}" >> {
          withEndpoint(endpointRecipe, appFactory)(block)
        }
      }.last
    }
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

/**
 * Mixin for an integration test that wants to test against supported servers and protocols.
 */
trait AllEndpointsIntegrationSpecification extends EndpointIntegrationSpecification {
  override val allEndpointRecipes: Seq[ServerEndpointRecipe] = {
    def http2Conf(enabled: Boolean): Configuration = Configuration("play.server.akka.http2.enabled" -> enabled)
    Seq(
      new HttpServerEndpointRecipe("Netty HTTP/1.1 via HTTP", NettyServer.provider, Configuration.empty, Set("1.0", "1.1"), Option("netty")),
      new HttpsServerEndpointRecipe("Netty HTTP/1.1 via HTTPS", NettyServer.provider, Configuration.empty, Set("1.0", "1.1"), Option("netty")),
      new HttpServerEndpointRecipe("akka-http HTTP/1.1 via HTTP", AkkaHttpServer.provider, http2Conf(false), Set("1.0", "1.1"), None),
      new HttpsServerEndpointRecipe("akka-http HTTP/1.1 via HTTPS", AkkaHttpServer.provider, http2Conf(false), Set("1.0", "1.1"), None),
      new HttpsServerEndpointRecipe("akka-http HTTP/2 via HTTPS", AkkaHttpServer.provider, http2Conf(true), Set("1.0", "1.1", "2"), None)
    )
  }
}

