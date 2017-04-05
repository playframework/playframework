/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.ws

import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.mvc.MultipartFormData

import scala.concurrent.Future

/**
 * A WS Request builder.
 */
trait WSRequest extends StandaloneWSRequest {
  override type Self <: WSRequest { type Self <: WSRequest.this.Self }
  override type Response <: WSResponse

  /**
   * Sets a multipart body for this request
   */
  def withBody(body: Source[MultipartFormData.Part[Source[ByteString, _]], _]): Self

  /**
   * Perform a PATCH on the request asynchronously.
   */
  def patch(body: Source[MultipartFormData.Part[Source[ByteString, _]], _]): Future[Response]

  /**
   * Perform a POST on the request asynchronously.
   */
  def post(body: Source[MultipartFormData.Part[Source[ByteString, _]], _]): Future[Response]

  /**
   * Perform a PUT on the request asynchronously.
   */
  def put(body: Source[MultipartFormData.Part[Source[ByteString, _]], _]): Future[Response]

}
