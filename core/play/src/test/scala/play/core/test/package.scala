/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core

import com.typesafe.config.{ Config, ConfigFactory }
import play.api._
import play.api.inject.DefaultApplicationLifecycle
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

    // So that we don't need a `application.conf` file.
    // There are tests to verify the application fails to start
    // if application.conf is not present in the classpath. So
    // adding it will conflict with those tests.
    val underlyingConfig = Configuration.load(
      environment.classLoader,
      new java.util.Properties(),
      Map.empty,
      allowMissingApplicationConf = true
    ).underlying

    val initialConfiguration = new Configuration(
      underlyingConfig.withFallback(extraConfig)
    )

    val context = ApplicationLoader.Context(
      environment,
      initialConfiguration,
      new DefaultApplicationLifecycle(),
      None
    )

    val app = new BuiltInComponentsFromContext(context) with NoHttpFiltersComponents {
      override def router: Router = play.api.routing.Router.empty
    }.application
    Play.start(app)
    try {
      block(app)
    } finally {
      Play.stop(app)
    }
  }

}
