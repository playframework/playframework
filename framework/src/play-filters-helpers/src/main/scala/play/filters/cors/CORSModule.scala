/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.filters.cors

import javax.inject.{ Inject, Provider }

import play.api.http.{ DefaultHttpErrorHandler, HttpErrorHandler }
import play.api.{ Environment, PlayConfig, Configuration }
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
class CORSFilterProvider @Inject() (configuration: Configuration, errorHandler: HttpErrorHandler, corsConfig: CORSConfig) extends Provider[CORSFilter] {

  def this(configuration: Configuration, corsConfig: CORSConfig) = this(configuration, DefaultHttpErrorHandler, corsConfig)

  lazy val get = {
    val pathPrefixes = PlayConfig(configuration).get[Seq[String]]("play.filters.cors.pathPrefixes")
    new CORSFilter(corsConfig, errorHandler, pathPrefixes)
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
 * Components for the CORS Filter
 */
trait CORSComponents {
  def configuration: Configuration
  def httpErrorHandler: HttpErrorHandler

  lazy val corsConfig: CORSConfig = CORSConfig.fromConfiguration(configuration)
  lazy val corsFilter: CORSFilter = new CORSFilter(corsConfig, httpErrorHandler, corsPathPrefixes)
  lazy val corsPathPrefixes: Seq[String] = PlayConfig(configuration).get[Seq[String]]("play.filters.cors.pathPrefixes")
}
