/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.http

import javax.inject.Inject

import com.typesafe.config.ConfigException
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
 *   class Filters @Inject()(defaultFilters: DefaultFilters, csrfFilter: CSRFFilter, corsFilter: CORSFilter)
 *     extends DefaultHttpFilters(defaultFilters, csrfFilter, corsFilter)
 * }}}
 */
class DefaultHttpFilters @Inject() (defaultFilters: HttpFilters, userFilters: EssentialFilter*) extends HttpFilters {

  val filters: Seq[EssentialFilter] = {
    Option(defaultFilters).map(_.filters).getOrElse(Nil) ++ userFilters
  }

  /**
   * @deprecated this constructor does not take an injected [[play.api.http.DefaultFilters]] instance
   * @param filters the list of user defined filters
   */
  @deprecated("Use DefaultHttpFilters @Inject()(defaultFilters: DefaultFilters, myFilter: MyFilter)", "2.6.0")
  def this(filters: EssentialFilter*) = {
    this(null: DefaultFilters, filters: _*)
  }
}

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

/**
 * This class pulls in a list of filters through configuration property, binding them to a list of classes.
 *
 * @param env the environment (classloader is used from here)
 * @param configuration the configuration ("play.filters.defaults" used from here)
 * @param injector finds an instance of filter by the class name
 */
class DefaultFilters @Inject() (env: Environment, configuration: Configuration, injector: Injector) extends HttpFilters {

  private val configListKey = "play.filters.defaults"

  private val defaultBindings: Seq[BindingKey[EssentialFilter]] = {
    try {
      for (filterClassName <- configuration.get[Seq[String]](configListKey)) yield {
        try {
          val filterClass: Class[EssentialFilter] = env.classLoader.loadClass(filterClassName).asInstanceOf[Class[EssentialFilter]]
          BindingKey(filterClass)
        } catch {
          case e: ClassNotFoundException =>
            // Give an explicit warning here so that the user has an idea what configuration property
            // caused the class not found exception...
            throw new IllegalStateException(s"The ${configListKey} configuration property cannot load class ${filterClassName}")
        }
      }
    } catch {
      case e: ConfigException.Null =>
        Nil
      case e: ConfigException.Missing =>
        Nil
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
