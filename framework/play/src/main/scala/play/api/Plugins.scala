package play.api

import play.api.mvc._

trait Plugin {

  def onStart {}
  def onStop {}

}

/**
 * Global plugin executes application's globalSettings onStart and onStop.
 */
class GlobalPlugin(app: Application) extends Plugin {
  override def onStart = {
    app.global.onStart(app)
  }

  override def onStop = {
    app.global.onStop(app)
  }
}
