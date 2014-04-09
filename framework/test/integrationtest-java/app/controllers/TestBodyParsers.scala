/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package controllers

import play.api.libs.iteratee._
import play.api.mvc._
import play.core.j.JavaParsers.DefaultRequestBody

object TestBodyParsers extends BodyParsers {

  import play.mvc.Http.{ RequestBody }

  def threadName(): BodyParser[RequestBody] = BodyParser("threadDebug") { request =>
    val threadName = Thread.currentThread.getName
    val requestBody: RequestBody = DefaultRequestBody(text = Some(threadName))
    Done(Right(requestBody), Input.Empty)
  }

}
