/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package commonguide.http.routing

import org.specs2.mutable.Specification
import play.api.mvc._
import play.api.routing.Router
import play.api.test._
import play.api.test.FakeRequest
import play.api.test.Helpers._

package controllers {
  import jakarta.inject.Inject
  import play.api.libs.json.JsValue

  class Api @Inject() (cc: ControllerComponents) extends AbstractController(cc) {
    def newThing: Action[JsValue] = Action(parse.json) { request => Ok(request.body) }
  }
}
