/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.http

import javax.inject.Inject

import play.api.{ Configuration, Environment }
import play.api.mvc.EssentialFilter
import play.utils.Reflect

/**
 * Provides filters to the [[play.http.HttpRequestHandler]].
 */
trait HttpFilters {

  /**
   * Return the filters that should filter every request
   */
  def filters: Seq[EssentialFilter]
}

object HttpFilters {

  def bindingsFromConfiguration(environment: Environment, configuration: Configuration) = {
    Reflect.bindingsFromConfiguration[HttpFilters, play.http.HttpFilters, JavaHttpFiltersAdapter, NoHttpFilters](environment,
      configuration, "play.http.filters", "Filters")
  }
}

/**
 * A filters provider that provides no filters.
 */
class NoHttpFilters extends HttpFilters {
  def filters = Nil
}

object NoHttpFilters extends NoHttpFilters

/**
 * Adapter from the Java HttpFliters to the Scala HttpFilters interface.
 */
class JavaHttpFiltersAdapter @Inject() (underlying: play.http.HttpFilters) extends HttpFilters {
  def filters = underlying.filters()
}
