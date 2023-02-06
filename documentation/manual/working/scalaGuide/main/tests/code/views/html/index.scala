/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package views.html

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import play.api.mvc._
import ExecutionContext.Implicits.global

object index extends Results {
  def apply(input: String): Future[Result] = {
    Future(
      Ok("Hello Coco").as("text/html")
    )
  }
}
