/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
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
class JavaHttpErrorHandlerAdapter @Inject() (underlying: JHttpErrorHandler) extends HttpErrorHandler {
  @deprecated("Use constructor without JavaContextComponents", "2.8.0")
  def this(underlying: JHttpErrorHandler, contextComponents: JavaContextComponents) {
    this(underlying)
  }

  def onClientError(request: RequestHeader, statusCode: Int, message: String) = {
    FutureConverters
      .toScala(underlying.onClientError(request.asJava, statusCode, message))
      .map(_.asScala)(Execution.trampoline)
  }

  def onServerError(request: RequestHeader, exception: Throwable) = {
    FutureConverters.toScala(underlying.onServerError(request.asJava, exception)).map(_.asScala)(Execution.trampoline)
  }
}
