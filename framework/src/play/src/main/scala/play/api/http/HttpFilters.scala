/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.http

import javax.inject.Inject

import com.typesafe.config.ConfigException
import play.api.inject.{ Binding, BindingKey, Injector }
import play.api.{ Configuration, Environment }
import play.api.mvc.EssentialFilter
import play.core.{ HandleWebCommandSupport, WebCommands }
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
 * This class pulls in a list of filters through configuration property, binding them to a list of classes.
 *
 * @param env the environment (classloader is used from here)
 * @param configuration the configuration
 * @param injector finds an instance of filter by the class name
 */
class EnabledFilters @Inject() (env: Environment, configuration: Configuration, injector: Injector, webCommands: WebCommands) extends HttpFilters {
  private val enabledKey = "play.filters.enabled"
  private val disabledKey = "play.filters.disabled"

  private val defaultBindings: Seq[BindingKey[EssentialFilter]] = {
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

  /**
   * Return the filters that should filter every request
   */
  override lazy val filters: Seq[EssentialFilter] = defaultBindings.map(injector.instanceOf(_))

  def start(): Unit = {
    webCommands.addHandler(new EnabledFiltersWebCommands(this))
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
  extends DefaultHttpFilters(underlying.filters: _*)

class JavaHttpFiltersDelegate @Inject() (delegate: HttpFilters)
  extends play.http.DefaultHttpFilters(delegate.filters: _*)

/**
 * Web command handler for listing filters on application start.
 */
class EnabledFiltersWebCommands @Inject() (enabledFilters: EnabledFilters) extends HandleWebCommandSupport {
  private val logger = play.api.Logger("play.api.http.EnabledFilters")

  private var enabled = true

  def handleWebCommand(request: play.api.mvc.RequestHeader, buildLink: play.core.BuildLink, path: java.io.File): Option[play.api.mvc.Result] = {
    if (enabled) {
      enabled = false
      val b = new StringBuffer()
      b.append("Enabled Filters: ")
      enabledFilters.filters.foreach(f => b.append(s"  ${f.getClass.getCanonicalName}\n"))
      b.append("Play comes with filters enabled by default for security: https://www.playframework.com/documentation/latest/Filters\n")
      logger.info(b.toString)
    }
    None
  }
}
