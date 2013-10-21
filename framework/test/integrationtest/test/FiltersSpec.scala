package test

import org.specs2.mutable.Specification
import play.api.mvc._
import play.api.Routes
import play.api.test._
import play.api.test.Helpers._
import scala.concurrent.duration.Duration
import scala.concurrent._

object FiltersSpec extends Specification {
  "filters should" should {
    "be able to access request tags" in {

      val notFoundText = "MockGlobal returned 404"
      
      object MockGlobal extends WithFilters(Filter { (f, rh) =>
        Future.successful(rh.tags.get(Routes.ROUTE_VERB).map(verb => Results.Ok(verb)).getOrElse(Results.NotFound(notFoundText)))
      })

      "helpers routing" in new WithApplication(FakeApplication(withGlobal = Some(MockGlobal))) {
        val result = route(FakeRequest("GET", "/")).get
        status(result) must_== 200
        contentAsString(result) must_== "GET"
      }

      "a filter can access request tags" in new WithServer(FakeApplication(withGlobal = Some(MockGlobal))) {
        val response = Await.result(wsCall(controllers.routes.Application.index()).get(), Duration.Inf)
        response.status must_== 200
        response.body must_== "GET"
      }

      "filters fire for every single request, even those not matching a route" in new WithServer(FakeApplication(withGlobal = Some(MockGlobal))) {
        val response = Await.result(wsUrl("/not-a-real-route").get(), Duration.Inf)
        response.status must_== 404
        response.body must_== notFoundText
      }

      object MockGlobal2 extends play.api.GlobalSettings {

        override def doFilter(next: RequestHeader => Handler): (RequestHeader => Handler) = {
          rh => {
            rh.tags.get("ROUTE_CONTROLLER").collect {
              case "controllers.Application" => next(rh)
            }.getOrElse {
              Action {
                Results.BadRequest("Unexpected path!")
              }
            }
          }
        }

      }

      "running server" in new WithServer(FakeApplication(withGlobal = Some(MockGlobal2))) {
        val response = Await.result(wsCall(controllers.routes.Application.plainHelloWorld()).get(), Duration.Inf)
        response.status must_== 200
        response.body must_== ("Hello World")

        val response2 = Await.result(wsCall(controllers.routes.JavaApi.index()).get(), Duration.Inf)
        response2.status must_== 400
        response2.body must_== ("Unexpected path!")
      }


    }
  }
}
