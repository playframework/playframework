/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.inject.guice

import play.api._
import play.api.inject.{ ApplicationLifecycle, bind }

/**
 * An ApplicationLoader that uses Guice to bootstrap the application.
 *
 * Subclasses can override the `builder` and `overrides` methods.
 */
class GuiceApplicationLoader(protected val initialBuilder: GuiceApplicationBuilder) extends ApplicationLoader {

  // empty constructor needed for instantiating via reflection
  def this() = this(new GuiceApplicationBuilder)

  override final def load(context: ApplicationLoader.Context): Application = {
    builder(context).build
  }

  /**
   * Construct a builder to use for loading the given context.
   */
  protected def builder(context: ApplicationLoader.Context): GuiceApplicationBuilder = {
    initialBuilder
      .disableCircularProxies()
      .in(context.environment)
      .loadConfig(context.initialConfiguration)
      .overrides(overrides(context): _*)
  }

  /**
   * Override some bindings using information from the context. The default
   * implementation of this method provides bindings that most applications
   * should include.
   */
  protected def overrides(context: ApplicationLoader.Context): Seq[GuiceableModule] = {
    GuiceApplicationLoader.defaultOverrides(context)
  }

}

object GuiceApplicationLoader {
  /**
   * The default overrides provided by the Scala and Java GuiceApplicationLoaders.
   */
  def defaultOverrides(context: ApplicationLoader.Context): Seq[GuiceableModule] = {
    Seq(
      bind[OptionalDevContext] to new OptionalDevContext(context.devContext),
      bind[ApplicationLifecycle] to context.lifecycle)
  }
}
