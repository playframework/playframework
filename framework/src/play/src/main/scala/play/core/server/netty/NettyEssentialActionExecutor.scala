/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server.netty

import play.core.system.{ HandlerExecutorContext, HandlerExecutor }
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.{ DefaultGlobal, Play, Application }
import play.api.libs.iteratee.Input.EOF
import com.typesafe.netty.http.pipelining.{ OrderedDownstreamChannelEvent, OrderedUpstreamMessageEvent }
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.HttpHeaders.Names._
import org.jboss.netty.handler.codec.http.HttpHeaders._

import scala.concurrent.Future

/**
 * EssentialAction handler executor for the Netty backend.
 */
object NettyEssentialActionExecutor extends HandlerExecutor[NettyBackendRequest] with RequestBodyHandler {

  def apply(context: HandlerExecutorContext[NettyBackendRequest], request: RequestHeader, backend: NettyBackendRequest,
    handler: Handler) = handler match {
    case action: EssentialAction =>
      import play.api.libs.iteratee.Execution.Implicits.trampoline
      val a = EssentialAction { rh =>
        Iteratee.flatten(action(rh).unflatten.map(_.it).recover {
          case error =>
            Iteratee.flatten(
              context.application.map(_.handleError(rh, error))
                .getOrElse(DefaultGlobal.onError(rh, error))
                .map(result => Done(result, Input.Empty))
            ): Iteratee[Array[Byte], SimpleResult]
        })
      }
      Some(handleAction(context.application, request, backend, a))
    case _ => None
  }

  def handleAction(application: Option[Application], request: RequestHeader, backend: NettyBackendRequest, action: EssentialAction): Future[_] = {
    Play.logger.trace("Serving this request with: " + action)

    // It is a pre-requesite that we're using the http pipelining capabilities provided and that we have a
    // handler downstream from this one that produces these events.
    implicit val ctx = backend.context
    implicit val oue = backend.event.asInstanceOf[OrderedUpstreamMessageEvent]
    val nettyHttpRequest = backend.event.getMessage.asInstanceOf[HttpRequest]

    val keepAlive = isKeepAlive(nettyHttpRequest)
    val nettyVersion = nettyHttpRequest.getProtocolVersion

    val bodyParser = Iteratee.flatten(
      scala.concurrent.Future(action(request))(play.api.libs.concurrent.Execution.defaultContext)
    )

    import play.api.libs.iteratee.Execution.Implicits.trampoline

    val expectContinue: Option[_] = request.headers.get("Expect").filter(_.equalsIgnoreCase("100-continue"))

    // Regardless of whether the client is expecting 100 continue or not, we need to feed the body here in the
    // Netty thread, so that the handler is replaced in this thread, so that if the client does start sending
    // body chunks (which it might according to the HTTP spec if we're slow to respond), we can handle them.

    val eventuallyResult: Future[SimpleResult] = if (nettyHttpRequest.isChunked) {

      val pipeline = ctx.getChannel.getPipeline
      val current = pipeline.get("handler")
      val result = newRequestBodyUpstreamHandler(bodyParser, { handler =>
        pipeline.replace("handler", "handler", handler)
      }, {
        pipeline.replace("handler", "handler", current)
      })

      result

    } else {

      val bodyEnumerator = {
        val body = {
          val cBuffer = nettyHttpRequest.getContent
          val bytes = new Array[Byte](cBuffer.readableBytes())
          cBuffer.readBytes(bytes)
          bytes
        }
        Enumerator(body).andThen(Enumerator.enumInput(EOF))
      }

      bodyEnumerator |>>> bodyParser
    }

    // An iteratee containing the result and the sequence number.
    // Sequence number will be 1 if a 100 continue response has been sent, otherwise 0.
    val eventuallyResultWithSequence: Future[(SimpleResult, Int)] = expectContinue match {
      case Some(_) => {
        bodyParser.unflatten.flatMap {
          case Step.Cont(k) =>
            sendDownstream(0, false, new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE))
            eventuallyResult.map((_, 1))
          case Step.Done(result, _) => {
            // Return the result immediately, and ensure that the connection is set to close
            // Connection must be set to close because whatever comes next in the stream is either the request
            // body, because the client waited too long for our response, or the next request, and there's no way
            // for us to know which.  See RFC2616 Section 8.2.3.
            Future.successful((result.copy(connection = HttpConnection.Close), 0))
          }
          case Step.Error(msg, _) => {
            backend.event.getChannel.setReadable(true)
            val error = new RuntimeException("Body parser iteratee in error: " + msg)
            val result = application.map(_.handleError(request, error)).getOrElse(DefaultGlobal.onError(request, error))
            result.map(r => (r.copy(connection = HttpConnection.Close), 0))
          }
        }
      }
      case None => eventuallyResult.map((_, 0))
    }

    val sent = eventuallyResultWithSequence.recoverWith {
      case error =>
        Play.logger.error("Cannot invoke the action, eventually got an error: " + error)
        backend.event.getChannel.setReadable(true)
        application.map(_.handleError(request, error))
          .getOrElse(DefaultGlobal.onError(request, error))
          .map((_, 0))
    }.flatMap {
      case (result, sequence) =>
        NettyResultStreamer.sendResult(cleanFlashCookie(request, result), !keepAlive, nettyVersion, sequence)
    }
    sent
  }

  def cleanFlashCookie(request: RequestHeader, result: SimpleResult): SimpleResult = {
    val header = result.header

    val flashCookie = {
      header.headers.get(SET_COOKIE)
        .map(Cookies.decode(_))
        .flatMap(_.find(_.name == Flash.COOKIE_NAME)).orElse {
          Option(request.flash).filterNot(_.isEmpty).map { _ =>
            Flash.discard.toCookie
          }
        }
    }

    flashCookie.map { newCookie =>
      result.withHeaders(SET_COOKIE -> Cookies.merge(header.headers.get(SET_COOKIE).getOrElse(""), Seq(newCookie)))
    }.getOrElse(result)
  }

  def sendDownstream(subSequence: Int, last: Boolean, message: Object)(implicit ctx: ChannelHandlerContext, oue: OrderedUpstreamMessageEvent) = {
    val ode = new OrderedDownstreamChannelEvent(oue, subSequence, last, message)
    ctx.sendDownstream(ode)
    ode.getFuture
  }

}
