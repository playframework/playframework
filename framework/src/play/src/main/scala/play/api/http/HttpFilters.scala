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

class JavaHttpFiltersDelegate @Inject() (delegate: HttpFilters) extends play.http.HttpFilters {
  def filters() = delegate.filters.map(_.asJava).toArray
}
