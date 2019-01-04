/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
import javax.inject._
import play.api.mvc._
//#server-request-attribute
import play.api.mvc.request.RequestAttrKey

class SomeScalaController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def index = Action { request =>
    assert(request.attrs.get(RequestAttrKey.Server) == Option("netty"))
    // ...
    //###skip: 1
    Ok("")
  }

}
//#server-request-attribute
