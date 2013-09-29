/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.http

import org.jboss.netty.handler.codec.http._
import play.api.mvc._

/**
 * Tests for Play's interactions with Netty. Allows observation and control of
 * individual Netty events, e.g. can simulate failure sending a response and
 * observe whether the connection is closed properly.
 */
object NettyHandlingSpec extends NettySpecification {

  "Play's Netty handling" should {

    "handle HTTP/1.1 requests without Connection headers" in withQueuingChannel(Action(Results.Ok)) { qc =>
      val request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/")
      qc.sendOrderedMessage(request)

      val responseMessage = qc.receive.asInstanceOf[Message]
      val httpResponse = responseMessage.msg.asInstanceOf[DefaultHttpResponse]
      httpResponse.getStatus.getCode must_== OK
      httpResponse.getHeader(HttpHeaders.Names.CONTENT_LENGTH) must_== "0"
      qc.succeed

      qc.dontReceive // Must not close connection
    }

    "handle HTTP/1.1 requests with 'Connection: Keep-Alive' headers" in withQueuingChannel(Action(Results.Ok)) { qc =>
      val request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/")
      request.addHeader(CONNECTION, "Keep-Alive")
      qc.sendOrderedMessage(request)

      val responseMessage = qc.receive.asInstanceOf[Message]
      val httpResponse = responseMessage.msg.asInstanceOf[DefaultHttpResponse]
      httpResponse.getStatus.getCode must_== OK
      httpResponse.getHeader(HttpHeaders.Names.CONTENT_LENGTH) must_== "0"
      qc.succeed

      qc.dontReceive // Must not close connection
    }

    "handle HTTP/1.1 requests with 'Connection: Close' headers" in withQueuingChannel(Action(Results.Ok)) { qc =>
      val request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/")
      request.addHeader(CONNECTION, "Close")
      qc.sendOrderedMessage(request)

      val responseMessage = qc.receive.asInstanceOf[Message]
      val httpResponse = responseMessage.msg.asInstanceOf[DefaultHttpResponse]
      httpResponse.getStatus.getCode must_== OK
      httpResponse.getHeader(HttpHeaders.Names.CONTENT_LENGTH) must_== "0"
      qc.succeed

      qc.receive.asInstanceOf[Close.type]
      qc.dontReceive // Must not close connection twice
    }

    "handle HTTP/1.1 requests with 'Connection: Close' headers when an error occurs sending a response" in withQueuingChannel(Action(Results.Ok)) { qc =>
      val request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/")
      request.addHeader(CONNECTION, "Close")
      qc.sendOrderedMessage(request)

      val responseMessage = qc.receive.asInstanceOf[Message]
      val httpResponse = responseMessage.msg.asInstanceOf[DefaultHttpResponse]
      httpResponse.getStatus.getCode must_== OK
      httpResponse.getHeader(HttpHeaders.Names.CONTENT_LENGTH) must_== "0"
      qc.fail(new Exception("Dummy exception sending"))

      qc.receive.asInstanceOf[Close.type]
      qc.dontReceive // Must not close connection twice
    }

    "handle HTTP/1.0 requests without Connection headers" in withQueuingChannel(Action(Results.Ok)) { qc =>
      val request = new DefaultHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.GET, "/")
      qc.sendOrderedMessage(request)

      val responseMessage = qc.receive.asInstanceOf[Message]
      val httpResponse = responseMessage.msg.asInstanceOf[DefaultHttpResponse]
      httpResponse.getStatus.getCode must_== OK
      httpResponse.getHeader(HttpHeaders.Names.CONTENT_LENGTH) must_== "0"
      qc.succeed

      qc.receive.asInstanceOf[Close.type]
      qc.dontReceive // Must not close connection twice
    }

    "handle HTTP/1.0 requests with 'Connection: Keep-Alive' headers" in withQueuingChannel(Action(Results.Ok)) { qc =>
      val request = new DefaultHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.GET, "/")
      request.addHeader(CONNECTION, "Keep-Alive")
      qc.sendOrderedMessage(request)

      val responseMessage = qc.receive.asInstanceOf[Message]
      val httpResponse = responseMessage.msg.asInstanceOf[DefaultHttpResponse]
      httpResponse.getStatus.getCode must_== OK
      httpResponse.getHeader(HttpHeaders.Names.CONTENT_LENGTH) must_== "0"
      qc.succeed

      qc.dontReceive // Must not close connection
    }

    "handle HTTP/1.0 requests with 'Connection: Close' headers" in withQueuingChannel(Action(Results.Ok)) { qc =>
      val request = new DefaultHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.GET, "/")
      request.addHeader(CONNECTION, "Close")
      qc.sendOrderedMessage(request)

      val responseMessage = qc.receive.asInstanceOf[Message]
      val httpResponse = responseMessage.msg.asInstanceOf[DefaultHttpResponse]
      httpResponse.getStatus.getCode must_== OK
      httpResponse.getHeader(HttpHeaders.Names.CONTENT_LENGTH) must_== "0"
      qc.succeed

      qc.receive.asInstanceOf[Close.type]
      qc.dontReceive // Must not close connection twice
    }

  }

}