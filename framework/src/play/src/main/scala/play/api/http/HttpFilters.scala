/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.http

import javax.inject.Inject

import play.api.{ PlayConfig, Configuration, Environment }
import play.api.mvc.EssentialFilter
import play.utils.Reflect

/**
 * Provides filters to the [[play.api.http.HttpRequestHandler]].
 */
trait HttpFilters {

  /**
   * Return the filters that should filter every request
   */
  def filters: Seq[EssentialFilter]

  def asJava: play.http.HttpFilters = new JavaHttpFiltersDelegate(this)
}

/**
 * A default implementation of HttpFilters that accepts filters as a varargs constructor and exposes them as a
 * filters sequence. For example:
 *
 * {{{
 *   class Filters @Inject()(csrfFilter: CSRFFilter, corsFilter: CORSFilter)
 *     extends DefaultHttpFilters(csrfFilter, corsFilter)
 * }}}
 */
class DefaultHttpFilters(val filters: EssentialFilter*) extends HttpFilters

object HttpFilters {

  def bindingsFromConfiguration(environment: Environment, configuration: Configuration) = {
    Reflect.bindingsFromConfiguration[HttpFilters, play.http.HttpFilters, JavaHttpFiltersAdapter, JavaHttpFiltersDelegate, NoHttpFilters](environment, PlayConfig(configuration), "play.http.filters", "Filters")
  }

  def apply(filters: EssentialFilter*): HttpFilters = {
    val f = filters
    new HttpFilters {
      def filters = f
    }
  }
}

/**
 * A filters provider that provides no filters.
 */
class NoHttpFilters extends DefaultHttpFilters

object NoHttpFilters extends NoHttpFilters

/**
 * Adapter from the Java HttpFilters to the Scala HttpFilters interface.
 */
class JavaHttpFiltersAdapter @Inject() (underlying: play.http.HttpFilters)
  extends DefaultHttpFilters(underlying.filters: _*)

class JavaHttpFiltersDelegate @Inject() (delegate: HttpFilters)
  extends play.http.DefaultHttpFilters(delegate.filters: _*)
