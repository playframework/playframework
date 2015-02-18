/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api

import play.api.inject.Injector
import play.utils.{ Reflect, PlayIO }

import scala.collection.IndexedSeqLike

/**
 * A Play plugin.
 *
 * A plugin must define a single argument constructor that accepts an [[play.api.Application]].  For example:
 * {{{
 * class MyPlugin(app: Application) extends Plugin {
 *   override def onStart() = {
 *     Logger.info("Plugin started!")
 *   }
 * }
 * }}}
 *
 * The plugin class must be declared in a play.plugins file available in the classpath root:
 * {{{
 * 1000:myapp.MyPlugin
 * }}}
 * The associated int defines the plugin priority.
 */
@deprecated("Use modules instead", since = "2.4.0")
trait Plugin {

  /**
   * Called when the application starts.
   */
  def onStart() {}

  /**
   * Called when the application stops.
   */
  def onStop() {}

  /**
   * Is the plugin enabled?
   */
  def enabled: Boolean = true

}

/**
 * Workaround to suppress deprecation warnings within the Play build.
 * Based on https://issues.scala-lang.org/browse/SI-7934
 */
private[play] object Plugin {
  type Deprecated = Plugin
}

class Plugins(plugins: => IndexedSeq[Plugin.Deprecated]) extends IndexedSeqLike[Plugin.Deprecated, IndexedSeq[Plugin.Deprecated]] with IndexedSeq[Plugin.Deprecated] {
  // Fix circular dependency
  private lazy val thePlugins = plugins
  def length = thePlugins.length
  def apply(idx: Int) = thePlugins(idx)
}

object Plugins {

  /**
   * Load all the plugin class names from the environment.
   */
  def loadPluginClassNames(env: Environment): Seq[String] = {
    import scala.collection.JavaConverters._

    val PluginDeclaration = """([0-9_]+):(.*)""".r

    val pluginFiles = env.classLoader.getResources("play.plugins").asScala.toList

    pluginFiles.distinct.map { plugins =>
      PlayIO.readUrlAsString(plugins).split("\n").map(_.replaceAll("#.*$", "").trim).filterNot(_.isEmpty).map {
        case PluginDeclaration(priority, className) => (priority.toInt, className)
      }
    }.flatten.sortBy(_._1).map(_._2)
  }

  /**
   * Load all the plugin classes from the given environment.
   */
  def loadPlugins(classNames: Seq[String], env: Environment, injector: Injector): Seq[Plugin.Deprecated] = {
    classNames.map { className =>
      val clazz = Reflect.getClass[Plugin.Deprecated](className, env.classLoader)
      injector.instanceOf(clazz)
    }.filter(_.enabled)
  }

  /**
   * Load all the plugins from the given environment.
   */
  def apply(env: Environment, injector: Injector): Plugins = {
    val classNames = loadPluginClassNames(env)
    // parameter is by name, this avoids a circular dependency between plugins and their application, so the plugins
    // are not instantiated until they're used
    new Plugins(loadPlugins(classNames, env, injector).toIndexedSeq)
  }

  def empty = new Plugins(IndexedSeq.empty)
}
