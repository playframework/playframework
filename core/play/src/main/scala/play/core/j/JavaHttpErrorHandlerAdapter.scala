/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.j

import javax.inject.Inject

import play.api.http.HttpErrorHandler
import play.api.mvc.RequestHeader
import play.core.Execution
import play.http.{ HttpErrorHandler => JHttpErrorHandler }

import scala.compat.java8.FutureConverters

/**
 * Adapter from a Java HttpErrorHandler to a Scala HttpErrorHandler
 */
class JavaHttpErrorHandlerAdapter @Inject()(underlying: JHttpErrorHandler, contextComponents: JavaContextComponents)
    extends HttpErrorHandler {

  def onClientError(request: RequestHeader, statusCode: Int, message: String) = {
    FutureConverters
      .toScala(underlying.onClientError(request.asJava, statusCode, message))
      .map(_.asScala)(Execution.trampoline)
  }

  def onServerError(request: RequestHeader, exception: Throwable) = {
    FutureConverters.toScala(underlying.onServerError(request.asJava, exception)).map(_.asScala)(Execution.trampoline)
  }
}
