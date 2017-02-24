/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.http

import javax.inject.Inject

import play.api.inject.{ Binding, BindingKey, Injector }
import play.api.{ Configuration, Environment }
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

  def bindingsFromConfiguration(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Reflect.bindingsFromConfiguration[HttpFilters, play.http.HttpFilters, JavaHttpFiltersAdapter, JavaHttpFiltersDelegate, DefaultFilters](environment, configuration, "play.http.filters", "Filters")
  }

  def apply(filters: EssentialFilter*): HttpFilters = {
    val f = filters
    new HttpFilters {
      def filters = f
    }
  }
}

class DefaultFilters @Inject() (env: Environment, configuration: Configuration, injector: Injector) extends HttpFilters {

  private val defaultBindings: Seq[BindingKey[EssentialFilter]] = {
    for (filterClassName <- configuration.get[Seq[String]]("play.filters.defaults")) yield {
      val filterClass: Class[EssentialFilter] = env.classLoader.loadClass(filterClassName).asInstanceOf[Class[EssentialFilter]]
      BindingKey(filterClass)
    }
  }

  /**
   * Return the filters that should filter every request
   */
  override lazy val filters: Seq[EssentialFilter] = defaultBindings.map(injector.instanceOf(_))
}

/**
 * A filters provider that provides no filters.
 */
class NoHttpFilters @Inject() () extends DefaultHttpFilters

object NoHttpFilters extends NoHttpFilters

/**
 * Adapter from the Java HttpFilters to the Scala HttpFilters interface.
 */
class JavaHttpFiltersAdapter @Inject() (underlying: play.http.HttpFilters)
  extends DefaultHttpFilters(underlying.filters: _*)

class JavaHttpFiltersDelegate @Inject() (delegate: HttpFilters)
  extends play.http.DefaultHttpFilters(delegate.filters: _*)
