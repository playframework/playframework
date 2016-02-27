/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.filters.cors

import javax.inject.{ Inject, Provider }

import akka.stream.Materializer
import play.api.http.HttpErrorHandler
import play.api.{ BuiltInComponents, Environment, PlayConfig, Configuration }
import play.api.inject.Module

/**
 * Provider for CORSConfig.
 */
class CORSConfigProvider @Inject() (configuration: Configuration) extends Provider[CORSConfig] {
  lazy val get = CORSConfig.fromConfiguration(configuration)
}

/**
 * Provider for CORSFilter.
 */
class CORSFilterProvider @Inject() (configuration: Configuration, errorHandler: HttpErrorHandler, corsConfig: CORSConfig,
    materializer: Materializer) extends Provider[CORSFilter] {
  lazy val get = {
    val pathPrefixes = PlayConfig(configuration).get[Seq[String]]("play.filters.cors.pathPrefixes")
    new CORSFilter(corsConfig, errorHandler, pathPrefixes)(materializer)
  }
}

/**
 * CORS module.
 */
class CORSModule extends Module {
  def bindings(environment: Environment, configuration: Configuration) = Seq(
    bind[CORSConfig].toProvider[CORSConfigProvider],
    bind[CORSFilter].toProvider[CORSFilterProvider]
  )
}

/**
 * CORS Filter components for compile-time DI
 */
trait CORSComponents {
  def corsConfig: CORSConfig
  def corsFilter: CORSFilter
  def corsPathPrefixes: Seq[String]
}

/**
 * CORS Filter components default implementation
 */
trait DefaultCORSComponents {
  this: BuiltInComponents =>

  lazy val corsConfig: CORSConfig = CORSConfig.fromConfiguration(configuration)
  lazy val corsFilter: CORSFilter = new CORSFilter(corsConfig, httpErrorHandler, corsPathPrefixes)
  lazy val corsPathPrefixes: Seq[String] = PlayConfig(configuration).get[Seq[String]]("play.filters.cors.pathPrefixes")
}
