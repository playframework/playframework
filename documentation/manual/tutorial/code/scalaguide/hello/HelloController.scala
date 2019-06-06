/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.hello {
  import play.api.mvc._
  import javax.inject.Inject

  package views {

    import play.twirl.api.Html

    object html {
      def index(): Html             = Html("Index page")
      def hello(): Html             = Html("Hello page")
      def hello(name: String): Html = Html(s"Hello $name")
    }
  }

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

    //#hello-world-hello-correct-action
    def hello(name: String) = Action {
      Ok(views.html.hello(name))
    }
    //#hello-world-hello-correct-action
  }
}
