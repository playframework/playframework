/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.j

import javax.inject.Inject

import play.api.http.HttpRequestHandler
import play.api.mvc.RequestHeader
import play.http.{ HttpRequestHandler => JHttpRequestHandler }

/**
 * Adapter from a Java HttpRequestHandler to a Scala HttpRequestHandler
 */
class JavaHttpRequestHandlerAdapter @Inject() (underlying: JHttpRequestHandler) extends HttpRequestHandler {
  override def handlerForRequest(request: RequestHeader) = {
    val handlerForRequest = underlying.handlerForRequest(new RequestHeaderImpl(request))
    (handlerForRequest.getRequest._underlyingHeader(), handlerForRequest.getHandler)
  }
}
