/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.action

import play.api.test.{ FakeRequest, PlaySpecification }
import play.api.mvc.{ Action, Controller }
import scala.concurrent.Future

object ContentNegotiationSpec extends PlaySpecification with Controller {

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
