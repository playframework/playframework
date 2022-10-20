/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.tests

package controllers

import javax.inject.Inject

import play.api.mvc._

class HomeController @Inject() (cc: ControllerComponents) extends AbstractController(cc) {
  def index = Action {
    Ok("Hello Bob").as("text/plain")
  }
}
