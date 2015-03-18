/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.http

import org.specs2.mutable.Specification
import play.api.mvc.{EssentialAction, RequestHeader}
import play.api.routing.Router
import javaguide.http.routing._
import play.api.test.Helpers._
import play.api.test.{FakeRequest, FakeApplication}
import javaguide.testhelpers.MockJavaAction
import play.libs.F

object JavaRouting extends Specification {

  "the java router" should {
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
      contentOf(FakeRequest("GET", "/api/list-all")) must_== "version null"
      contentOf(FakeRequest("GET", "/api/list-all?version=3.0")) must_== "version 3.0"
    }
    "support reverse routing" in {
      running(FakeApplication()) {
        header("Location", call(new MockJavaAction {
          override def invocation = F.Promise.pure(new javaguide.http.routing.controllers.Application().index())
        }, FakeRequest())) must beSome("/hello/Bob")
      }
    }

  }

  def contentOf(rh: RequestHeader, router: Class[_ <: Router] = classOf[Routes]) = {
    val app = FakeApplication(additionalConfiguration = Map("play.http.router" -> router.getName))
    running(app) {
      contentAsString(app.requestHandler.handlerForRequest(rh)._2 match {
        case e: EssentialAction => e(rh).run
      })
    }
  }
}

package routing.query.controllers {

import play.api.mvc.{Controller, Action}

class Application extends Controller {
  def show(page: String) = Action {
    Ok("showing page " + page)
  }
}
}

package routing.fixed.controllers {

import play.api.mvc.{Controller, Action}

class Application extends Controller {
  def show(page: String) = Action {
    Ok("showing page " + page)
  }
}
}

package routing.defaultvalue.controllers {

import play.api.mvc.{Controller, Action}

class Clients extends Controller {
  def list(page: Int) = Action {
    Ok("clients page " + page)
  }
}
}

