/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.cors

import javax.inject.{ Inject, Provider }

import akka.stream.Materializer
import play.api.Configuration
import play.api.http.HttpErrorHandler
import play.api.inject._

/**
 * Provider for CORSConfig.
 */
class CORSConfigProvider @Inject() (configuration: Configuration) extends Provider[CORSConfig] {
  lazy val get = CORSConfig.fromConfiguration(configuration)
}

/**
 * Provider for CORSFilter.
 */
class CORSFilterProvider @Inject() (configuration: Configuration, errorHandler: HttpErrorHandler, corsConfig: CORSConfig) extends Provider[CORSFilter] {
  lazy val get = {
    val pathPrefixes = configuration.get[Seq[String]]("play.filters.cors.pathPrefixes")
    new CORSFilter(corsConfig, errorHandler, pathPrefixes)
  }
}

/**
 * CORS module.
 */
class CORSModule extends SimpleModule(
  bind[CORSConfig].toProvider[CORSConfigProvider],
  bind[CORSFilter].toProvider[CORSFilterProvider]
)

/**
 * Components for the CORS Filter
 */
trait CORSComponents {
  def configuration: Configuration
  def httpErrorHandler: HttpErrorHandler
  implicit def materializer: Materializer

  lazy val corsConfig: CORSConfig = CORSConfig.fromConfiguration(configuration)
  lazy val corsFilter: CORSFilter = new CORSFilter(corsConfig, httpErrorHandler, corsPathPrefixes)
  lazy val corsPathPrefixes: Seq[String] = configuration.get[Seq[String]]("play.filters.cors.pathPrefixes")
}
