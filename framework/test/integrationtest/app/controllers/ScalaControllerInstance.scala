/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package controllers

import play.api._
import play.api.libs.json.Json._
import play.api.mvc._

class ScalaControllerInstance extends Controller {

  def index = Action {
    Ok(toJson(Map(
        "peter" -> toJson("foo"),
        "yay" -> toJson("value")
    )));
  }

}
