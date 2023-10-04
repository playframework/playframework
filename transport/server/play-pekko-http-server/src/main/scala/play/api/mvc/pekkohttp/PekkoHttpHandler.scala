/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc.pekkohttp

import scala.concurrent.Future

import org.apache.pekko.http.scaladsl.model.HttpRequest
import org.apache.pekko.http.scaladsl.model.HttpResponse
import play.api.mvc.Handler

trait PekkoHttpHandler extends (HttpRequest => Future[HttpResponse]) with Handler

object PekkoHttpHandler {
  def apply(handler: HttpRequest => Future[HttpResponse]): PekkoHttpHandler = new PekkoHttpHandler {
    def apply(request: HttpRequest): Future[HttpResponse] = handler(request)
  }
}
