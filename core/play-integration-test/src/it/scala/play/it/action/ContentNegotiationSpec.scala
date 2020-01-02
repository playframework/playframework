/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.action

import akka.actor.ActorSystem
import akka.stream.Materializer
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.PlaySpecification

import scala.concurrent.Future

class ContentNegotiationSpec extends PlaySpecification with ControllerHelpers {
  implicit val system = ActorSystem()
  implicit val mat    = Materializer.matFromSystem
  val Action          = ActionBuilder.ignoringBody

  "rendering" should {
    "work with simple results" in {
      status(Action { implicit req =>
        render {
          case Accepts.Json() => Ok
        }
      }(FakeRequest().withHeaders(ACCEPT -> "application/json"))) must_== 200
    }

    "work with simple results in an async action" in {
      status(Action.async { implicit req =>
        Future.successful(render {
          case Accepts.Json() => Ok
        })
      }(FakeRequest().withHeaders(ACCEPT -> "application/json"))) must_== 200
    }

    "work with async results" in {
      status(Action.async { implicit req =>
        render.async {
          case Accepts.Json() => Future.successful(Ok)
        }
      }(FakeRequest().withHeaders(ACCEPT -> "application/json"))) must_== 200
    }
  }
}
