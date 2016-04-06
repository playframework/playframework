/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.ws.ahc

import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.asynchttpclient.AsyncHandler.State
import org.asynchttpclient.{ AsyncHttpClient, HttpResponseBodyPart, HttpResponseHeaders, HttpResponseStatus, Request }
import org.asynchttpclient.handler.StreamedAsyncHandler
import org.reactivestreams.{ Publisher, Subscriber, Subscription }
import play.api.libs.ws.{ DefaultWSResponseHeaders, StreamedResponse, WSResponseHeaders }

import scala.concurrent.{ Future, Promise }

private[play] object Streamed {

  def execute(client: AsyncHttpClient, request: Request): Future[StreamedResponse] = {
    val promise = Promise[(WSResponseHeaders, Publisher[HttpResponseBodyPart])]()
    client.executeRequest(request, new DefaultStreamedAsyncHandler(promise))
    import play.core.Execution.Implicits.trampoline
    promise.future.map {
      case (headers, publisher) =>
        // this transformation is not part of `DefaultStreamedAsyncHandler.onCompleted` because 
        // a reactive-streams `Publisher` needs to be returned to implement `execute2`. Though, 
        // once `execute2` is removed, we should move the code here inside 
        // `DefaultStreamedAsyncHandler.onCompleted`.
        val source = Source.fromPublisher(publisher).map(bodyPart => ByteString(bodyPart.getBodyPartBytes))
        StreamedResponse(headers, source)
    }
  }

  private class DefaultStreamedAsyncHandler(promise: Promise[(WSResponseHeaders, Publisher[HttpResponseBodyPart])]) extends StreamedAsyncHandler[Unit] {
    private var statusCode: Int = _
    private var responseHeaders: WSResponseHeaders = _
    private var publisher: Publisher[HttpResponseBodyPart] = _

    def onStream(publisher: Publisher[HttpResponseBodyPart]): State = {
      if (this.publisher != null) State.ABORT
      else {
        this.publisher = publisher
        promise.success((responseHeaders, publisher))
        State.CONTINUE
      }
    }

    override def onStatusReceived(status: HttpResponseStatus): State = {
      if (this.publisher != null) State.ABORT
      else {
        statusCode = status.getStatusCode
        State.CONTINUE
      }
    }

    override def onHeadersReceived(h: HttpResponseHeaders): State = {
      if (this.publisher != null) State.ABORT
      else {
        val headers = h.getHeaders
        responseHeaders = DefaultWSResponseHeaders(statusCode, AhcWSRequest.ahcHeadersToMap(headers))
        State.CONTINUE
      }
    }

    override def onBodyPartReceived(bodyPart: HttpResponseBodyPart): State =
      throw new IllegalStateException("Should not have received body part")

    override def onCompleted(): Unit = {
      // EmptyPublisher can be replaces with `Source.empty` when we carry out the refactoring 
      // mentioned in the `execute2` method.
      promise.trySuccess((responseHeaders, EmptyPublisher))
    }

    override def onThrowable(t: Throwable): Unit = promise.tryFailure(t)
  }

  private case object EmptyPublisher extends Publisher[HttpResponseBodyPart] {
    def subscribe(s: Subscriber[_ >: HttpResponseBodyPart]): Unit = {
      if (s eq null) throw new NullPointerException("Subscriber must not be null, rule 1.9")
      s.onSubscribe(CancelledSubscription)
      s.onComplete()
    }
    private case object CancelledSubscription extends Subscription {
      override def request(elements: Long): Unit = ()
      override def cancel(): Unit = ()
    }
  }
}
