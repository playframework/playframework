/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package views.html

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import play.api.data.Form
import play.api.i18n.MessagesProvider
import play.api.mvc._
import ExecutionContext.Implicits.global

object formTemplate extends Results {
  def apply[T](form: Form[T])(implicit provider: MessagesProvider): Future[Result] = {
    Future(
      Ok("ok").as("text/html")
    )
  }
}
