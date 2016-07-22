/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.http

import play.api.libs.json.{ JsError, Json }
import play.api.mvc.{ Codec, RequestHeader, Result, Results }
import play.core.j.JavaHelpers

sealed trait HttpError[T] {

  def error: T

  def entity: HttpEntity

  def request: RequestHeader

  def asJava: play.http.HttpError[T]

  def asResult: Result

}

case class HttpClientError(request: RequestHeader, statusCode: Int, error: AnyRef, entity: HttpEntity) extends HttpError[AnyRef] {

  override def asJava: play.http.HttpClientError = JavaHelpers.withContext(request) { ctx =>
    new play.http.HttpClientError(ctx.request(), statusCode, error, entity.asJava)
  }

  override def asResult: Result = Results.Status(statusCode).sendEntity(entity)

}

object HttpError {

  def fromString(request: RequestHeader, statusCode: Int, message: String = ""): HttpClientError = {
    val entity = HttpEntity.Strict(Codec.utf_8.encode(message), Option(ContentTypes.HTML))
    new HttpClientError(request, statusCode, message, entity)
  }

  def fromJsError(request: RequestHeader, statusCode: Int, jsError: JsError): HttpClientError = {
    val entity = HttpEntity.Strict(Codec.utf_8.encode(Json.stringify(JsError.toJson(jsError))), Option(ContentTypes.JSON))
    new HttpClientError(request, statusCode, jsError, entity)
  }

}

case class HttpServerError(request: RequestHeader, error: Throwable) extends HttpError[Throwable] {

  override def entity: HttpEntity = {
    HttpEntity.Strict(Codec.utf_8.encode(s"A server error occurred:  ${error.getMessage}"), Option(ContentTypes.TEXT))
  }

  override def asJava: play.http.HttpError[Throwable] = JavaHelpers.withContext(request) { ctx =>
    new play.http.HttpServerError(ctx.request(), error)
  }

  override def asResult: Result = Results.InternalServerError.sendEntity(entity)

}