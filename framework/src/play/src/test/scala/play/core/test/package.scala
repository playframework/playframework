/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core

import com.typesafe.config.{ Config, ConfigFactory }
import play.api._
import play.api.routing.Router

package object test {

  /**
   * Run the given block of code with an application.
   */
  def withApplication[T](block: => T): T = {
    val app = new BuiltInComponentsFromContext(ApplicationLoader.Context.create(Environment.simple())) with NoHttpFiltersComponents {
      override def router: Router = play.api.routing.Router.empty
    }.application
    Play.start(app)
    try {
      block
    } finally {
      Play.stop(app)
    }
  }

  def withApplication[T](block: Application => T): T = {
    withApplicationAndConfig(Environment.simple(), ConfigFactory.empty())(block)
  }

  def withApplication[T](environment: Environment)(block: Application => T): T = {
    withApplicationAndConfig(environment, ConfigFactory.empty())(block)
  }

  def withApplicationAndConfig[T](environment: Environment, extraConfig: Config)(block: Application => T): T = {
    val app = new BuiltInComponentsFromContext(ApplicationLoader.Context.create(environment)) with NoHttpFiltersComponents {
      override def router: Router = play.api.routing.Router.empty
      override def configuration: Configuration = {
        val underlyingConfig = super.configuration.underlying
        new Configuration(underlyingConfig.withFallback(extraConfig))
      }
    }.application
    Play.start(app)
    try {
      block(app)
    } finally {
      Play.stop(app)
    }
  }

}
