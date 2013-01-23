package test

import org.specs2.mutable.Specification
import play.api.mvc._
import play.api.Routes
import play.api.test._
import play.api.test.Helpers._
import scala.concurrent.duration.Duration
import scala.concurrent.Await

object FiltersSpec extends Specification {
  "filters should" should {
    "be able to access request tags" in {

      object MockGlobal extends WithFilters(Filter { (f, rh) =>
        rh.tags.get(Routes.ROUTE_VERB).map(verb => Results.Ok(verb)).getOrElse(Results.NotFound)
      })

      "helpers routing" in new WithApplication(FakeApplication(withGlobal = Some(MockGlobal))) {
        val result = route(FakeRequest("GET", "/")).get
        status(result) must_== 200
        contentAsString(result) must_== "GET"
      }

      "running server" in new WithServer(FakeApplication(withGlobal = Some(MockGlobal))) {
        val response = Await.result(wsCall(controllers.routes.Application.index()).get(), Duration.Inf)
        response.status must_== 200
        response.body must_== "GET"
      }
    }
  }
}
