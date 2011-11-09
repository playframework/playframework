/**
 * Play framework.
 *
 * == Play 2.0 ==
 * [[http://www.playframework.org http://www.playframework.org]]
 */
package object play

package play {

  /**
   * Contains the public API for Scala developers.
   *
   * ==== Access the current Play application ====
   * {{{
   * import play.api.Play.current
   * }}}
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
   *
   */
  package object api

}

