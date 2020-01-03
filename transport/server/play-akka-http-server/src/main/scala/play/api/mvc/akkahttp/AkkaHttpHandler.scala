/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc.akkahttp

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpResponse
import play.api.mvc.Handler
import play.mvc.Http.RequestHeader

import scala.concurrent.Future

trait AkkaHttpHandler extends (HttpRequest => Future[HttpResponse]) with Handler

object AkkaHttpHandler {
  def apply(handler: HttpRequest => Future[HttpResponse]): AkkaHttpHandler = new AkkaHttpHandler {
    def apply(request: HttpRequest): Future[HttpResponse] = handler(request)
  }
}
