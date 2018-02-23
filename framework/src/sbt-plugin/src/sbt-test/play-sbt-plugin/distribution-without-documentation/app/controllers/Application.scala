/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */
package controllers

import play.api._
import play.api.mvc._
import scala.collection.JavaConverters._

import javax.inject.Inject

/**
 * i will fail since I check for a undefined class [[Documentation]]
 */
class Application @Inject() (action: DefaultActionBuilder) extends ControllerHelpers {

  def index = action {
    Ok
  }

}
