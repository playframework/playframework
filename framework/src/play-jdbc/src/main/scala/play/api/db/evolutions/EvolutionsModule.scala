/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.db.evolutions

import play.api.{ Configuration, Environment }
import play.api.db.DBApi
import play.api.inject.Module
import play.core.WebCommands

/**
 * Default module for evolutions API.
 */
class EvolutionsModule extends Module {
  def bindings(environment: Environment, configuration: Configuration) = {
    if (configuration.underlying.getBoolean("play.modules.db.evolutions.enabled")) {
      Seq(
        bind[EvolutionsConfig].toProvider[DefaultEvolutionsConfigParser],
        bind[EvolutionsReader].toSelf,
        bind[EvolutionsApi].to[DefaultEvolutionsApi],
        bind[ApplicationEvolutions].toSelf.eagerly
      )
    } else {
      Nil
    }
  }
}

/**
 * Components for default implementation of the evolutions API.
 */
trait EvolutionsComponents {
  def environment: Environment
  def configuration: Configuration
  def dynamicEvolutions: DynamicEvolutions
  def dbApi: DBApi
  def webCommands: WebCommands

  lazy val evolutionsConfig: EvolutionsConfig = new DefaultEvolutionsConfigParser(configuration).parse
  lazy val evolutionsReader: EvolutionsReader = new EvolutionsReader(environment)
  lazy val evolutionsApi: EvolutionsApi = new DefaultEvolutionsApi(dbApi)
  lazy val applicationEvolutions: ApplicationEvolutions = new ApplicationEvolutions(evolutionsConfig, evolutionsReader, evolutionsApi, dynamicEvolutions, dbApi, environment, webCommands)
}
