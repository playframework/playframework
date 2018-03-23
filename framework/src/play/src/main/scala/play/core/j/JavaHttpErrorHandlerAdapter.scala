/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.j

import javax.inject.Inject

import play.api.http.HttpErrorHandler
import play.api.mvc.RequestHeader
import play.http.{ HttpErrorHandler => JHttpErrorHandler }

import java.util.Optional

/**
 * Adapter from a Java HttpErrorHandler to a Scala HttpErrorHandler
 */
class JavaHttpErrorHandlerAdapter @Inject() (underlying: JHttpErrorHandler, contextComponents: JavaContextComponents) extends HttpErrorHandler {

  def onClientError(request: RequestHeader, statusCode: Int, message: String) = {
    JavaHelpers.invokeWithContext(request, contextComponents, (req, ctx) => underlying.onClientError(req, statusCode, message, Optional.ofNullable(ctx)))
  }

  def onServerError(request: RequestHeader, exception: Throwable) = {
    JavaHelpers.invokeWithContext(request, contextComponents, (req, ctx) => underlying.onServerError(req, exception, Optional.ofNullable(ctx)))
  }
}
