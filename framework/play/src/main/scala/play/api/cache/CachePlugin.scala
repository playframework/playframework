package play.api.cache

import play.core._
import play.api._

/**
 * because caching shoudl be swapable, we reference against an abstract class, so users can change implementation by
 * implementing this abstract class
 */
abstract class CachePlugin extends Plugin {
  def api: CacheAPI
}

/**
 * plugin manages BasicCache life cycle
 */
class BasicCachePlugin(app: Application) extends CachePlugin {

  val pluginDisabled = app.configuration.getString("cache.default").filter(_ == "disabled").headOption

  override def enabled = pluginDisabled.isDefined == false

  lazy val cache = new BasicCache

  def api = if (enabled) cache else {
    throw PlayException(
      "you are trying to access Plugin[" + this.getClass.toString + "] which is likely disabled",
      "",
      None)
  }

}
