package views.html.helper.twitterBootstrap

import play.api._

/**
 * Plugin for twitterBootstrap.
 *
 * To enable this, add the following to your Build.scala file:
 *
 * <pre>
 * val appDependencies = Seq(
 *   twitterBootstrap
 * )
 * </pre>
 *
 * And add a file play.plugins to conf/ directory with the following text:
 *
 * <pre>
 *   1050:views.html.helper.twitterBootstrap.TwitterBootstrapPlugin
 * </pre>
 *
 * Then, you should be able to add the twitter bootstrap plugin by adding this to your
 * form:
 *
 * <pre>
 * @(myForm: Form[User])
 *
 * @import helper.twitterBootstrap._
 *
 * @helper.form(action = routes.Application.submit) {
 *    @helper.inputText(myForm("username"))
 *    @helper.inputPassword(myForm("password"))
 * }
 * </pre>
 *
 * and you will see:
 *
 * <pre>
 *   &lt;form action="/" method="POST" &gt;
 *
 * &lt;div class="control-group  " id="username_field"&gt;
 * &lt;label class="control-label" for="username"&gt;username&lt;/label&gt;
 * &lt;div class="controls"&gt;
 *
 * &lt;input type="text" id="username" name="username" value="" &gt;
 *
 * &lt;span class="help-inline"&gt;&lt;/span&gt;
 * &lt;p class="help-block"&gt;&lt;/p&gt;
 * &lt;/div&gt;
 * &lt;/div&gt;
 *
 * &lt;div class="control-group  " id="password_field"&gt;
 * &lt;label class="control-label" for="password"&gt;password&lt;/label&gt;
 * &lt;div class="controls"&gt;
 *
 * &lt;input type="password" id="password" name="password" &gt;
 *
 * &lt;span class="help-inline"&gt;&lt;/span&gt;
 * &lt;p class="help-block"&gt;Required&lt;/p&gt;
 * &lt;/div&gt;
 * &lt;/div&gt;
 *
 * &lt;/form&gt;
 * </pre>
 *
 * as the output.
 *
 * @param app
 */
class TwitterBootstrapPlugin(app: Application) extends Plugin {

  val logger = Logger("twitterbootstrap")

  override val enabled = true

  override def onStart() {
    logger.debug("twitterbootstrap started.")
  }

  override def onStop() {
    logger.debug("twitterbootstrap stopped.")
  }
}
