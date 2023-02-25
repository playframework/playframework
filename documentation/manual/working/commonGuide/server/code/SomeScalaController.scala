/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import javax.inject._

import play.api.mvc._
//#server-request-attribute
import play.api.mvc.request.RequestAttrKey

class SomeScalaController @Inject() (cc: ControllerComponents) extends AbstractController(cc) {
  def index: Action[AnyContent] = Action { request =>
    assert(request.attrs.get(RequestAttrKey.Server) == Option("netty"))
    // ...
    // ###skip: 1
    Ok("")
  }
}
//#server-request-attribute
