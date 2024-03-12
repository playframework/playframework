/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
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
  def injectorFromBindings(bindings: Seq[Binding[?]]): Injector = {
    new GuiceInjectorBuilder().bindings(bindings).injector()
  }
}
