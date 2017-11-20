/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.test

import org.specs2.execute.AsResult
import org.specs2.mutable.SpecLike
import org.specs2.specification.core.Fragment

/**
 * Mixin class for integration tests that want to run over different
 * backend servers and protocols.
 *
 * @see [[ServerEndpoint]]
 */
trait EndpointIntegrationSpecification
    extends SpecLike with ApplicationFactories {

  trait ServerEndpointOps[T] {
    def filteredEndpoints: Seq[(ServerEndpointRecipe, TestFilter)]
    def withEndpoints(filteredEndpoints: Seq[(ServerEndpointRecipe, TestFilter)]): ServerEndpointOps[T]

    final def filterEndpoints(f: ((ServerEndpointRecipe, TestFilter) => TestFilter)): ServerEndpointOps[T] = {
      val newEndpoints = filteredEndpoints.map {
        case (recipe, filter) => (recipe, f(recipe, filter))
      }
      withEndpoints(newEndpoints)
    }

    final def pending(p: ServerEndpointRecipe => Boolean): ServerEndpointOps[T] = {
      filterEndpoints {
        case (recipe, _) if p(recipe) => TestFilter.Pending
        case (_, filter) => filter
      }
    }

    final def skipped(p: ServerEndpointRecipe => Boolean): ServerEndpointOps[T] = {
      filterEndpoints {
        case (recipe, _) if p(recipe) => TestFilter.Skipped
        case (_, filter) => filter
      }
    }

    def runBlock[A: AsResult](recipe: ServerEndpointRecipe)(block: T => A): A

    /**
     * Helper that creates a specs2 fragment for all the server endpoints supported
     * by Play. Each fragment creates an application, starts a server
     * and runs the given block of code.
     *
     * {{{
     * serveOk.forEndpoints { endpoint: ServerEndpoint =>
     *   val response = ... connect to endpoint.port ...
     *   response.status must_== 200
     * }
     * }}}
     */
    final def forEndpoints[A: AsResult](block: T => A): Fragment = {
      filteredEndpoints.map {
        case (recipe, filter) =>
          s"with ${recipe.description}" >> {
            filter.executeFiltered(runBlock(recipe)(block))
          }
      }.last
    }
  }

  /**
   * Abstract class to help convert operations on a `U` (e.g. a [[ServerEndpoint]]) into
   * operations on some other kind of object.
   * @param delegate Underlying object to run operations on `U`.
   * @tparam T The new type of of object to run operations on.
   * @tparam U The old type of object to run operations on.
   */
  abstract class AdaptingServerEndpointOps[T, U](delegate: ServerEndpointOps[U])
      extends ServerEndpointOps[T] {
    final override def filteredEndpoints: Seq[(ServerEndpointRecipe, TestFilter)] = delegate.filteredEndpoints
    final override def withEndpoints(filteredEndpoints: Seq[(ServerEndpointRecipe, TestFilter)]): ServerEndpointOps[T] = {
      val newDelegate: ServerEndpointOps[U] = delegate.withEndpoints(filteredEndpoints)
      withDelegate(newDelegate)
    }
    final override def runBlock[A: AsResult](recipe: ServerEndpointRecipe)(block: T => A): A = {
      val adaptedBlock: U => A = adaptBlock(block)
      delegate.runBlock(recipe)(adaptedBlock)
    }
    protected def adaptBlock[A](block: T => A): U => A
    protected def withDelegate(newDelegate: ServerEndpointOps[U]): ServerEndpointOps[T]
  }

  /**
   * Class that runs operations over all [[ServerEndpoint]]s, using an [[ApplicationFactory]]
   * to create an `Application` for each endpoint.
   */
  final class PlainServerEndpointOps(
      val appFactory: ApplicationFactory,
      override val filteredEndpoints: Seq[(ServerEndpointRecipe, TestFilter)] = ServerEndpointRecipe.AllRecipes.map((_, TestFilter.Run))
  ) extends ServerEndpointOps[ServerEndpoint] {

    override def runBlock[A: AsResult](recipe: ServerEndpointRecipe)(block: ServerEndpoint => A): A = {
      ServerEndpoint.withEndpoint(recipe, appFactory)(block)
    }

    override def withEndpoints(filteredEndpoints: Seq[(ServerEndpointRecipe, TestFilter)]): PlainServerEndpointOps = {
      new PlainServerEndpointOps(appFactory, filteredEndpoints)
    }
  }

}