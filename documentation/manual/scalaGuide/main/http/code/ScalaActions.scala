package http.scalaactions

import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import org.specs2.mutable.Specification

object ScalaActionsSpec extends Specification with Controller {

  "A scala action" should {
    "Allow writing a simple echo action" in {
      //#echo-action
      val echo = Action { request =>
        Ok("Got request [" + request + "]")
      }
      //#echo-action
      running(FakeApplication()) {
        status(echo(FakeRequest())) must_== 200
      }
    }
  }

}
