/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package views.html

import play.api.mvc._

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import ExecutionContext.Implicits.global

object index extends Results {

  def apply(input: String): Future[Result] = {
    Future(
      Ok("Hello Coco").as("text/html")
    )
  }
}
