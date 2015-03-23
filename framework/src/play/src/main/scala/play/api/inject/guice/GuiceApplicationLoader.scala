/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.inject.guice

import play.api.{ Application, ApplicationLoader, Configuration, Environment, OptionalSourceMapper }
import play.api.inject.{ bind, Injector => PlayInjector }
import play.core.WebCommands

/**
 * An ApplicationLoader that uses Guice to bootstrap the application.
 */
class GuiceApplicationLoader(builder: GuiceApplicationBuilder) extends ApplicationLoader {

  // empty constructor needed for instantiating via reflection
  def this() = this(new GuiceApplicationBuilder)

  def load(context: ApplicationLoader.Context): Application = {
    builder(context).build
  }

  def builder(context: ApplicationLoader.Context): GuiceApplicationBuilder = {
    builder
      .in(context.environment)
      .loadConfig(context.initialConfiguration)
      .overrides(overrides(context): _*)
  }

  def overrides(context: ApplicationLoader.Context): Seq[GuiceableModule] = {
    Seq(
      bind[OptionalSourceMapper] to new OptionalSourceMapper(context.sourceMapper),
      bind[WebCommands] to context.webCommands)
  }

}
