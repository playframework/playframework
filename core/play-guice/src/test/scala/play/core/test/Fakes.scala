/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.test

import play.api.inject.guice.GuiceInjectorBuilder
import play.api.inject.Binding
import play.api.inject.Injector

/**
 * Utilities to help with testing
 */
object Fakes {

  /**
   * Create an injector from the given bindings.
   *
   * @param bindings The bindings
   * @return The injector
   */
  def injectorFromBindings(bindings: Seq[Binding[_]]): Injector = {
    new GuiceInjectorBuilder().bindings(bindings).injector
  }
}
