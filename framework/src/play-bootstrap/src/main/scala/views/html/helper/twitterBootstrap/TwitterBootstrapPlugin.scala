package views.html.helper.twitterBootstrap

import play.api._

class TwitterBootstrapPlugin(app: Application) extends Plugin {
  @volatile var loaded = false

  /**
    * Is this plugin enabled.
    *
    * {{{
    * bootstrapplugin=disabled
    * }}}
    */
   override lazy val enabled = {
     !app.configuration.getString("bootstrapplugin").filter(_ == "disabled").isDefined
   }

   override def onStart() {
     // does nothing
   }

   override def onStop() {
     // does nothing
   }
}
