/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.http

import javax.inject.Inject
import javax.inject.Singleton

import com.typesafe.config.ConfigException
import play.api.inject.{ Binding, BindingKey, Injector }
import play.api.{ Configuration, Environment, Logger }
import play.api.mvc.EssentialFilter
import play.utils.Reflect

import scala.collection.JavaConverters._

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
 * filters sequence. This is available for runtime DI users who don't want to do things in configuration using play.filters.enabled, because they need more fine grained control over the injected components.
 *
 * For example:
 *
 * {{{
 *   class Filters @Inject()(defaultFilters: EnabledFilters, corsFilter: CORSFilter)
 *     extends DefaultHttpFilters(defaultFilters.filters :+ corsFilter: _*)
 * }}}
 */
class DefaultHttpFilters @Inject() (val filters: EssentialFilter*) extends HttpFilters

object HttpFilters {

  def bindingsFromConfiguration(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Reflect.bindingsFromConfiguration[HttpFilters, play.http.HttpFilters, JavaHttpFiltersAdapter, JavaHttpFiltersDelegate, EnabledFilters](environment, configuration, "play.http.filters", "Filters")
  }

  def apply(filters: EssentialFilter*): HttpFilters = {
    val f = filters
    new HttpFilters {
      def filters = f
    }
  }
}

/**
 * This class provides filters that are "automatically" enabled through `play.filters.enabled`.
 * A list of default filters are defined in reference.conf.
 *
 * See https://www.playframework.com/documentation/latest/Filters for more information.
 *
 * @param env the environment (classloader is used from here)
 * @param configuration the configuration
 * @param injector finds an instance of filter by the class name
 */
@Singleton
class EnabledFilters @Inject() (env: Environment, configuration: Configuration, injector: Injector) extends HttpFilters {

  private val url = "https://www.playframework.com/documentation/latest/Filters"

  private val logger = Logger(this.getClass)

  private val enabledKey = "play.filters.enabled"

  private val disabledKey = "play.filters.disabled"

  override val filters: Seq[EssentialFilter] = {
    val bindings: Seq[BindingKey[EssentialFilter]] = {
      try {
        val disabledSet = configuration.get[Seq[String]](disabledKey).toSet
        val enabledList = configuration.get[Seq[String]](enabledKey).filterNot(disabledSet.contains)

        for (filterClassName <- enabledList) yield {
          try {
            val filterClass: Class[EssentialFilter] = env.classLoader.loadClass(filterClassName).asInstanceOf[Class[EssentialFilter]]
            BindingKey(filterClass)
          } catch {
            case e: ClassNotFoundException =>
              throw configuration.reportError(enabledKey, s"Cannot load class $filterClassName", Some(e))
          }
        }
      } catch {
        case e: ConfigException.Null =>
          Nil
        case e: ConfigException.Missing =>
          Nil
      }
    }

    bindings.map(injector.instanceOf(_))
  }

  private def printMessageInDevMode(): Unit = {
    if (env.mode == play.api.Mode.Dev) {
      val b = new StringBuffer()
      b.append(s"Enabled Filters (see <$url>):\n\n")
      filters.foreach(f => b.append(s"    ${f.getClass.getCanonicalName}\n"))
      logger.info(b.toString)
    }
  }

  def start(): Unit = {
    printMessageInDevMode()
  }

  start() // on construction
}

/**
 * A filters provider that provides no filters.
 */
class NoHttpFilters extends HttpFilters {
  val filters: Seq[EssentialFilter] = Nil
}

object NoHttpFilters extends NoHttpFilters

/**
 * Adapter from the Java HttpFilters to the Scala HttpFilters interface.
 */
class JavaHttpFiltersAdapter @Inject() (underlying: play.http.HttpFilters)
  extends DefaultHttpFilters(underlying.getFilters.asScala: _*)

class JavaHttpFiltersDelegate @Inject() (delegate: HttpFilters)
  extends play.http.DefaultHttpFilters(delegate.filters.asJava)
