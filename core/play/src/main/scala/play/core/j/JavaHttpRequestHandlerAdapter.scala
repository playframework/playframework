/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.j

import jakarta.inject.Inject
import play.api.http.HttpRequestHandler
import play.api.mvc.RequestHeader
import play.http.{ HttpRequestHandler => JHttpRequestHandler }
import play.http.HandlerForRequest
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
