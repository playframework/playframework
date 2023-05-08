/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.j

import scala.jdk.FutureConverters._

import jakarta.inject.Inject
import play.api.http.HttpErrorHandler
import play.api.mvc.RequestHeader
import play.core.Execution
import play.http.{ HttpErrorHandler => JHttpErrorHandler }

/**
 * Adapter from a Java HttpErrorHandler to a Scala HttpErrorHandler
 */
class JavaHttpErrorHandlerAdapter @Inject() (underlying: JHttpErrorHandler) extends HttpErrorHandler {
  @deprecated("Use constructor without JavaContextComponents", "2.8.0")
  def this(underlying: JHttpErrorHandler, contextComponents: JavaContextComponents) = {
    this(underlying)
  }

  def onClientError(request: RequestHeader, statusCode: Int, message: String) = {
    underlying
      .onClientError(request.asJava, statusCode, message)
      .asScala
      .map(_.asScala)(Execution.trampoline)
  }

  def onServerError(request: RequestHeader, exception: Throwable) = {
    underlying.onServerError(request.asJava, exception).asScala.map(_.asScala)(Execution.trampoline)
  }
}
