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
}
