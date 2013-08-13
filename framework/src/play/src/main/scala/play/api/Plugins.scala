package play.api

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

