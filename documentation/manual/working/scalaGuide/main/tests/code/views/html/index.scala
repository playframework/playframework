/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package views.html

import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global

object index extends Results {

  def apply(input:String) : Future[Result] = {
    Future(
      Ok("Hello Coco") as("text/html")
    )
  }
}
