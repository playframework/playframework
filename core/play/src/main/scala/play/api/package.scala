/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

/**
 * Play framework.
 *
 * == Play ==
 * [[http://www.playframework.com http://www.playframework.com]]
 */
package object play

package play {

  /**
   * Contains the public API for Scala developers.
   *
   * ==== Read configuration ====
   * {{{
   * val poolSize = configuration.getInt("engine.pool.size")
   * }}}
   *
   * ==== Use the logger ====
   * {{{
   * Logger.info("Hello!")
   * }}}
   *
   * ==== Define a Plugin ====
   * {{{
   * class MyPlugin(app: Application) extends Plugin
   * }}}
   *
   * ==== Create adhoc applications (for testing) ====
   * {{{
   * val application = Application(new File("."), this.getClass.getClassloader, None, Play.Mode.DEV)
   * }}}
   */
  package object api
}
