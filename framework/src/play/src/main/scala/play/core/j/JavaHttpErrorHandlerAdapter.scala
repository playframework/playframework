/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.j

import javax.inject.Inject

import play.api.http.HttpErrorHandler
import play.api.mvc.RequestHeader
import play.http.{ HttpErrorHandler => JHttpErrorHandler }

/**
 * Adapter from a Java HttpErrorHandler to a Scala HttpErrorHandler
 */
class JavaHttpErrorHandlerAdapter @Inject() (underlying: JHttpErrorHandler, requestComponents: JavaRequestComponents) extends HttpErrorHandler {

  def onClientError(request: RequestHeader, statusCode: Int, message: String) = {
    JavaHelpers.invokeWithRequest(request, requestComponents, req => underlying.onClientError(req, statusCode, message))
  }

  def onServerError(request: RequestHeader, exception: Throwable) = {
    JavaHelpers.invokeWithRequest(request, requestComponents, req => underlying.onServerError(req, exception))
  }
}
