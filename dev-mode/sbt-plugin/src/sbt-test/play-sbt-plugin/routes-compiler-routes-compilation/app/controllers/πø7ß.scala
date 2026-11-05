/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package controllers

import play.api.mvc._
import jakarta.inject.Inject

class πø7ß @Inject() (c: ControllerComponents) extends AbstractController(c) {
  def ôü65t(i: Int) = Action {
    Ok
  }
  def ôü65tJavaRequest(req: play.mvc.Http.Request, i: java.lang.Integer): play.mvc.Result = {
    play.mvc.Results.ok(req.uri() + " " + i)
  }
}
