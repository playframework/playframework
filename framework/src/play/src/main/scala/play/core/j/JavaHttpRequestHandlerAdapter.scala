/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
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
    val handlerForRequest = underlying.handlerForRequest(request.asJava)
    (handlerForRequest.getRequestHeader.asScala, handlerForRequest.getHandler)
  }
}

/**
 * Adapter from a Java HttpRequestHandler to a Scala HttpRequestHandler
 */
class JavaHttpRequestHandlerDelegate @Inject() (underlying: HttpRequestHandler) extends JHttpRequestHandler {
  override def handlerForRequest(requestHeader: JRequestHeader) = {
    val (newRequest, handler) = underlying.handlerForRequest(requestHeader.asScala())
    new HandlerForRequest(newRequest.asJava, handler)
  }
}
