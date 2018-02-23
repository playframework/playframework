/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.test

import play.api._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.routing.Router

/**
 * Creates an [[Application]]. Usually created by a helper in [[ApplicationFactories]].
 */
trait ApplicationFactory {
  /** Creates an [[Application]]. */
  def create(): Application
}

/**
 * Mixin with helpers for creating [[ApplicationFactory]] objects.
 */
trait ApplicationFactories {
  def withGuiceApp(builder: GuiceApplicationBuilder): ApplicationFactory = new ApplicationFactory {
    override def create(): Application = builder.build()
  }
  def withComponents(components: => BuiltInComponents): ApplicationFactory = new ApplicationFactory {
    override def create(): Application = components.application
  }
  def withRouter(createRouter: BuiltInComponents => Router): ApplicationFactory =
    withConfigAndRouter(Map.empty)(createRouter)
  def withConfigAndRouter(extraConfig: Map[String, Any])(createRouter: BuiltInComponents => Router): ApplicationFactory = withComponents {
    val context = ApplicationLoader.Context.create(
      environment = Environment.simple(),
      initialSettings = Map[String, AnyRef](Play.GlobalAppConfigKey -> java.lang.Boolean.FALSE) ++ extraConfig.asInstanceOf[Map[String, AnyRef]]
    )
    new BuiltInComponentsFromContext(context) with NoHttpFiltersComponents {
      override lazy val router: Router = createRouter(this)
    }
  }
  def withAction(createAction: DefaultActionBuilder => Action[_]): ApplicationFactory = withRouter { components: BuiltInComponents =>
    val action = createAction(components.defaultActionBuilder)
    Router.from { case _ => action }
  }
  def withResult(result: Result): ApplicationFactory = withAction { Action: DefaultActionBuilder =>
    Action { result }
  }
}

final object ApplicationFactory extends ApplicationFactories
