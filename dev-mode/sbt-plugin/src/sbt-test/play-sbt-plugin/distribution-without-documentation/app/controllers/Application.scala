/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package controllers

import jakarta.inject.Inject
import play.api._
import play.api.mvc._

/**
 * i will fail since I check for a undefined class [[Documentation]]
 */
class Application @Inject() (action: DefaultActionBuilder) extends ControllerHelpers {

  def index = action {
    Ok
  }

}
