/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.j

import javax.inject.Inject

import play.api.http.HttpRequestHandler
import play.api.mvc.RequestHeader
import play.http.{ HttpRequestHandler => JHttpRequestHandler, HandlerForRequest }
import play.mvc.Http.{ RequestHeader => JRequestHeader }

/**
 * Adapter from a Java HttpRequestHandler to a Scala HttpRequestHandler
 */
class JavaHttpRequestHandlerAdapter @Inject() (underlying: JHttpRequestHandler) extends HttpRequestHandler {
  override def handlerForRequest(request: RequestHeader) = {
    val handlerForRequest = underlying.handlerForRequest(new RequestHeaderImpl(request))
    (handlerForRequest.getRequest._underlyingHeader(), handlerForRequest.getHandler)
  }
}

/**
 * Adapter from a Java HttpRequestHandler to a Scala HttpRequestHandler
 */
class JavaHttpRequestHandlerDelegate @Inject() (underlying: HttpRequestHandler) extends JHttpRequestHandler {
  override def handlerForRequest(request: JRequestHeader) = {
    val (newRequest, handler) = underlying.handlerForRequest(request._underlyingHeader())
    new HandlerForRequest(new RequestHeaderImpl(newRequest), handler)
  }
}
