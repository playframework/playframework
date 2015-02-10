/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.http.routing

import org.specs2.mutable.Specification
import play.api.test.FakeRequest
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test._
import play.api.routing.Router

package controllers {

  object Client {
    def findById(id: Long) = Some("showing client " + id)
  }

  class Clients extends Controller {

    // #show-client-action
    def show(id: Long) = Action {
      Client.findById(id).map { client =>
        Ok(views.html.Clients.display(client))
      }.getOrElse(NotFound)
    }
    // #show-client-action

    def list() = Action(Ok("all clients"))
  }

  class Application extends Controller {
    def download(name: String) = Action(Ok("download " + name))
    def homePage() = Action(Ok("home page"))

    def loadContentFromDatabase(page: String) = Some("showing page " + page)

    // #show-page-action
    def show(page: String) = Action {
      loadContentFromDatabase(page).map { htmlContent =>
        Ok(htmlContent).as("text/html")
      }.getOrElse(NotFound)
    }
    // #show-page-action
  }

  class Items extends Controller {
    def show(id: Long) = Action(Ok("showing item " + id))
  }

  class Api extends Controller {
    def list(version: Option[String]) = Action(Ok("version " + version))
  }
}

package query {
  package object controllers {
    type Application = scalaguide.http.routing.controllers.Application
  }
}

package fixed {
  package object controllers {
    type Application = scalaguide.http.routing.controllers.Application
  }
}

package defaultvalue.controllers {
  class Clients extends Controller {
    def list(page: Int) = Action(Ok("clients page " + page))
  }
}

// #reverse-controller
// ###replace: package controllers
package reverse.controllers {

import play.api._
import play.api.mvc._

class Application extends Controller {

  def hello(name: String) = Action {
    Ok("Hello " + name + "!")
  }

}
// #reverse-controller
}

object ScalaRoutingSpec extends Specification {
  "the scala router" should {
    "support simple routing with a long parameter" in {
      contentOf(FakeRequest("GET", "/clients/10")).trim must_== "showing client 10"
    }
    "support a static path" in {
      contentOf(FakeRequest("GET", "/clients/all")) must_== "all clients"
    }
    "support a path part that spans multiple segments" in {
      contentOf(FakeRequest("GET", "/files/foo/bar")) must_== "download foo/bar"
    }
    "support regex path parts" in {
      contentOf(FakeRequest("GET", "/items/20")) must_== "showing item 20"
    }
    "support parameterless actions" in {
      contentOf(FakeRequest("GET", "/")) must_== "home page"
    }
    "support passing parameters from the path" in {
      contentOf(FakeRequest("GET", "/foo")) must_== "showing page foo"
    }
    "support passing parameters from the query string" in {
      contentOf(FakeRequest("GET", "/?page=foo"), classOf[query.Routes]) must_== "showing page foo"
    }
    "support fixed values for parameters" in {
      contentOf(FakeRequest("GET", "/foo"), classOf[fixed.Routes]) must_== "showing page foo"
      contentOf(FakeRequest("GET", "/"), classOf[fixed.Routes]) must_== "showing page home"
    }
    "support default values for parameters" in {
      contentOf(FakeRequest("GET", "/clients"), classOf[defaultvalue.Routes]) must_== "clients page 1"
      contentOf(FakeRequest("GET", "/clients?page=2"), classOf[defaultvalue.Routes]) must_== "clients page 2"
    }
    "support optional values for parameters" in {
      contentOf(FakeRequest("GET", "/api/list-all")) must_== "version None"
      contentOf(FakeRequest("GET", "/api/list-all?version=3.0")) must_== "version Some(3.0)"
    }
    "support reverse routing" in {
      import reverse.controllers.routes
      import Results.Redirect
      // #reverse-router
      // Redirect to /hello/Bob
      def helloBob = Action {
        Redirect(routes.Application.hello("Bob"))
      }
      // #reverse-router
      val result = helloBob(FakeRequest())
      header(LOCATION, result) must beSome("/hello/Bob")
    }

  }

  def contentOf(rh: RequestHeader, router: Class[_ <: Router] = classOf[Routes]) = {
    val app = FakeApplication()
    running(app) {
      contentAsString(app.injector.instanceOf(router).routes(rh) match {
        case e: EssentialAction => e(rh).run
      })
    }
  }
}
