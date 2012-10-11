package play.api

import play.api.mvc._

/**
 * A Play plugin.
 *
 * You can define a Play plugin this way:
 * {{{
 * class MyPlugin(app: Application) extends Plugin
 * }}}
 *
 * The plugin class must be declared in a play.plugins file available in the classpath root:
 * {{{
 * 1000:myapp.MyPlugin
 * }}}
 * The associated int defines the plugin priority.
 */
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

