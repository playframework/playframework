/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.inject.guice

import com.google.inject.Module
import play.api.{ Application, ApplicationLoader, Configuration, Environment, OptionalSourceMapper }
import play.api.inject.{ bind, Injector => PlayInjector, Module => PlayModule }
import play.core.WebCommands

/**
 * An ApplicationLoader that uses Guice to bootstrap the application.
 */
class GuiceApplicationLoader(builder: GuiceApplicationBuilder) extends ApplicationLoader {

  // empty constructor needed for instantiating via reflection
  def this() = this(new GuiceApplicationBuilder)

  def load(context: ApplicationLoader.Context): Application = {
    builder
      .in(context.environment)
      .loadConfig(context.initialConfiguration)
      .bindings(modules(context: ApplicationLoader.Context): _*)
      .overrides(overrides(context): _*).build
  }

  def modules(context: ApplicationLoader.Context): Array[GuiceableModule] = {
    playModules(context: ApplicationLoader.Context).map(GuiceableModule.guiceable(_)) ++ guiceModules(context: ApplicationLoader.Context).map(GuiceableModule.guiceable(_))
  }

  /**
   * Override to provide your own Guice modules
   */
  def guiceModules(context: ApplicationLoader.Context): Array[Module] = {
    Array[Module]()
  }

  /**
   * Override to provide your own Play modules
   */
  def playModules(context: ApplicationLoader.Context): Array[PlayModule] = {
    Array[PlayModule]()
  }

  /**
   * Override to provide your own override modules
   */
  def overrides(context: ApplicationLoader.Context): Array[GuiceableModule] = {
    Array[GuiceableModule](
      bind[OptionalSourceMapper] to new OptionalSourceMapper(context.sourceMapper),
      bind[WebCommands] to context.webCommands)
  }

}
