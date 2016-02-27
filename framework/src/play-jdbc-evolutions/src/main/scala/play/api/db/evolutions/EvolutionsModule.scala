/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.db.evolutions

import javax.inject._

import play.api.db.{ DBComponents, DBApi }
import play.api.inject.{ Injector, Module }
import play.api.{ BuiltInComponents, Configuration, Environment }
import play.core.WebCommands

/**
 * Default module for evolutions API.
 */
class EvolutionsModule extends Module {
  def bindings(environment: Environment, configuration: Configuration) = {
    Seq(
      bind[EvolutionsConfig].toProvider[DefaultEvolutionsConfigParser],
      bind[EvolutionsReader].to[EnvironmentEvolutionsReader],
      bind[EvolutionsApi].to[DefaultEvolutionsApi],
      bind[ApplicationEvolutions].toProvider[ApplicationEvolutionsProvider].eagerly
    )
  }
}

/**
 * Evolutions components for compile-time DI
 */
trait EvolutionsComponents {
  def evolutionsConfig: EvolutionsConfig
  def evolutionsReader: EvolutionsReader
  def evolutionsApi: EvolutionsApi
  def applicationEvolutions: ApplicationEvolutions
}

/**
 * Evolution components default implementation
 */
trait DefaultEvolutionsComponents extends EvolutionsComponents {
  this: BuiltInComponents with DBComponents with DynamicEvolutionsComponents =>

  lazy val evolutionsConfig: EvolutionsConfig = new DefaultEvolutionsConfigParser(configuration).parse
  lazy val evolutionsReader: EvolutionsReader = new EnvironmentEvolutionsReader(environment)
  lazy val evolutionsApi: EvolutionsApi = new DefaultEvolutionsApi(dbApi)
  lazy val applicationEvolutions: ApplicationEvolutions = new ApplicationEvolutions(evolutionsConfig, evolutionsReader, evolutionsApi, dynamicEvolutions, dbApi, environment, webCommands)
}

@Singleton
class ApplicationEvolutionsProvider @Inject() (
    config: EvolutionsConfig,
    reader: EvolutionsReader,
    evolutions: EvolutionsApi,
    dbApi: DBApi,
    environment: Environment,
    webCommands: WebCommands,
    injector: Injector) extends Provider[ApplicationEvolutions] {

  lazy val get = new ApplicationEvolutions(config, reader, evolutions, injector.instanceOf[DynamicEvolutions], dbApi,
    environment, webCommands)
}

