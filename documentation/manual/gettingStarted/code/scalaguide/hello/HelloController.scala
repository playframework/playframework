package scalaguide.hello

import play.api.mvc._
import javax.inject.Inject

class HelloController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {

  //#hello-world-index-action
  def index = Action {
    Ok(views.html.index())
  }
  //#hello-world-index-action

  //#hello-world-hello-action
  def hello = Action {
    Ok(views.html.hello())
  }
  //#hello-world-hello-action

  /*
  //#hello-world-hello-error-action
  def hello(name: String) = Action {
    Ok(views.html.hello())
  }
  //#hello-world-hello-error-action
   */

  /*
  //#hello-world-hello-correct-action
  def hello(name: String) = Action {
    Ok(views.html.hello(name))
  }
  //#hello-world-hello-correct-action
   */
}