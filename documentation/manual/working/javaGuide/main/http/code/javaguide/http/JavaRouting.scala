/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.http

import java.util.concurrent.CompletableFuture

import javaguide.http.routing._
import javaguide.testhelpers.MockJavaAction
import org.specs2.mutable.Specification
import play.api.mvc.EssentialAction
import play.api.mvc.RequestHeader
import play.api.routing.Router
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.core.j.JavaHandlerComponents
import play.mvc.Http
import play.mvc.Result

class JavaRouting extends Specification {
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
    "support invoking Default controller actions" in {
      statusOf(FakeRequest("GET", "/about"), classOf[defaultcontroller.Routes]) must_== SEE_OTHER
      statusOf(FakeRequest("GET", "/orders"), classOf[defaultcontroller.Routes]) must_== NOT_FOUND
      statusOf(FakeRequest("GET", "/clients"), classOf[defaultcontroller.Routes]) must_== INTERNAL_SERVER_ERROR
      statusOf(FakeRequest("GET", "/posts"), classOf[defaultcontroller.Routes]) must_== NOT_IMPLEMENTED
    }
    "support optional values for parameters" in {
      contentOf(FakeRequest("GET", "/api/list-all")) must_== "version null"
      contentOf(FakeRequest("GET", "/api/list-all?version=3.0")) must_== "version 3.0"
    }
    "support list values for parameters" in {
      contentOf(FakeRequest("GET", "/api/list-items?item=apples&item=bananas")) must_== "params apples,bananas"
      contentOf(FakeRequest("GET", "/api/list-int-items?item=1&item=42")) must_== "params 1,42"
    }
    "support reverse routing" in {
      running() { app =>
        implicit val mat = app.materializer
        header(
          "Location",
          call(
            new MockJavaAction(app.injector.instanceOf[JavaHandlerComponents]) {
              override def invocation(req: Http.Request): CompletableFuture[Result] =
                CompletableFuture.completedFuture(new javaguide.http.routing.controllers.Application().index())
            },
            FakeRequest()
          )
        ) must beSome("/hello/Bob")
      }
    }
  }

  def contentOf(rh: RequestHeader, router: Class[? <: Router] = classOf[Routes]) = {
    running(_.configure("play.http.router" -> router.getName)) { app =>
      implicit val mat = app.materializer
      contentAsString(app.requestHandler.handlerForRequest(rh)._2 match {
        case e: EssentialAction => e(rh).run()
      })
    }
  }

  def statusOf(rh: RequestHeader, router: Class[? <: Router] = classOf[Routes]) = {
    running(_.configure("play.http.router" -> router.getName)) { app =>
      implicit val mat = app.materializer
      status(app.requestHandler.handlerForRequest(rh)._2 match {
        case e: EssentialAction => e(rh).run()
      })
    }
  }
}

package routing.query.controllers {
  import play.api.mvc.AbstractController
  import play.api.mvc.ControllerComponents

  class Application @jakarta.inject.Inject() (components: ControllerComponents) extends AbstractController(components) {
    def show(page: String) = Action {
      Ok("showing page " + page)
    }
  }
}

package routing.fixed.controllers {
  import play.api.mvc.AbstractController
  import play.api.mvc.ControllerComponents

  class Application @jakarta.inject.Inject() (components: ControllerComponents) extends AbstractController(components) {
    def show(page: String) = Action {
      Ok("showing page " + page)
    }
  }
}

package routing.defaultvalue.controllers {
  import play.api.mvc.AbstractController
  import play.api.mvc.ControllerComponents

  class Clients @jakarta.inject.Inject() (components: ControllerComponents) extends AbstractController(components) {
    def list(page: Int) = Action {
      Ok("clients page " + page)
    }
  }
}

package routing.defaultcontroller.controllers {
  class Default extends _root_.controllers.Default
}
