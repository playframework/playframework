/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.tests

package controllers

import javax.inject.Inject

import play.api.mvc._

class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  def index() = Action {
    Ok("Hello Bob").as("text/plain")
  }
}
