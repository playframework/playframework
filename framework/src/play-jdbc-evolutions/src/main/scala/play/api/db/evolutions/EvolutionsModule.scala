/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.db.evolutions

import javax.inject._

import play.api.db.DBApi
import play.api.inject._
import play.api.{ Configuration, Environment }
import play.core.WebCommands

/**
 * Default module for evolutions API.
 */
class EvolutionsModule extends SimpleModule(
  bind[EvolutionsConfig].toProvider[DefaultEvolutionsConfigParser],
  bind[EvolutionsReader].to[EnvironmentEvolutionsReader],
  bind[EvolutionsApi].to[DefaultEvolutionsApi],
  bind[ApplicationEvolutions].toProvider[ApplicationEvolutionsProvider].eagerly
)

/**
 * Components for default implementation of the evolutions API.
 */
trait EvolutionsComponents {
  def environment: Environment
  def configuration: Configuration
  def dbApi: DBApi
  def webCommands: WebCommands

  lazy val dynamicEvolutions: DynamicEvolutions = new DynamicEvolutions
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

