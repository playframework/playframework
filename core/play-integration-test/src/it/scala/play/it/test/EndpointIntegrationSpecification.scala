/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.test

import org.specs2.execute.AsResult
import org.specs2.execute.PendingUntilFixed
import org.specs2.execute.Result
import org.specs2.execute.ResultExecution
import org.specs2.mutable.SpecLike
import org.specs2.specification.core.Fragment
import play.api.http.HttpProtocol
import play.api.test.ApplicationFactories
import play.api.test.ApplicationFactory
import play.api.test.ServerEndpointRecipe
import play.core.server.ServerEndpoint

/**
 * Mixin class for integration tests that want to run over different
 * backend servers and protocols.
 *
 * @see [[ServerEndpoint]]
 */
trait EndpointIntegrationSpecification extends SpecLike with PendingUntilFixed with ApplicationFactories {

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
     * withResult(Results.Ok("Hello")) withEndpoints(myEndpointRecipes) { endpoint: ServerEndpoint =>
     *   val response = ... connect to endpoint.port ...
     *   response.status must_== 200
     * }
     * }}}
     */
    def withEndpoints[A: AsResult](endpoints: Seq[ServerEndpointRecipe])(block: ServerEndpoint => A): Fragment = {
      endpoints.map { endpointRecipe: ServerEndpointRecipe =>
        s"with ${endpointRecipe.description}" >> {
          ServerEndpointRecipe.withEndpoint(endpointRecipe, appFactory)(block)
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
      withEndpoints(NettyServerEndpointRecipes.AllRecipes ++ AkkaHttpServerEndpointRecipes.AllRecipes)(block)
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
      conditionalPendingUntilFixed(endpoint.protocols.contains(HttpProtocol.HTTP_2_0))
    }
  }
}
