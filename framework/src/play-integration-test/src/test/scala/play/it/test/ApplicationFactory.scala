/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.test

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.routing.Router
import play.api.{ BuiltInComponents, _ }

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

  /**
   * Used to create `Application`s with a `GuiceApplicationBuilder`.
   */
  def serveFromGuice(builder: GuiceApplicationBuilder): ApplicationFactory = new ApplicationFactory {
    override def create(): Application = builder.build()
  }

  /**
   * Used to create `Application`s from `BuiltinComponents`.
   */
  def serveComponents(components: => BuiltInComponents): ApplicationFactory = new ApplicationFactory {
    override def create(): Application = components.application
  }

  /**
   * Used to create `Application`s from `BuiltinComponents` via a `ComponentsBuilderApplicationFactory`.
   */
  def serveComponents: ComponentsBuilderApplicationFactory = ComponentsBuilderApplicationFactory.Default

  def serveRouter(createRouter: BuiltInComponents => Router): ComponentsBuilderApplicationFactory =
    serveComponents.withRouter(createRouter)
  def serveExtraConfig(extraConfig: Map[String, Any]): ComponentsBuilderApplicationFactory =
    serveComponents.withExtraConfig(extraConfig = extraConfig)
  def serveAction(createAction: BuiltInComponents => Action[_]): ComponentsBuilderApplicationFactory =
    serveComponents.withAction(createAction)
  def serveResult(createResult: BuiltInComponents => Result): ComponentsBuilderApplicationFactory =
    serveComponents.withResult(createResult)
  def serveResult(result: => Result): ComponentsBuilderApplicationFactory =
    serveComponents.withResult(result)
  def serveOk(message: String): ComponentsBuilderApplicationFactory = serveComponents.withOk(message)
  def serveOk: ComponentsBuilderApplicationFactory = serveComponents.withOk
}

final object ApplicationFactory extends ApplicationFactories

