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
import play.api.libs.iteratee.Enumerator
import play.api.libs.streams.Streams
import play.api.libs.ws.{ DefaultWSResponseHeaders, StreamedResponse, WSResponseHeaders }

import scala.concurrent.{ Future, Promise }

private[play] object Streamed {

  def execute(client: AsyncHttpClient, request: Request): Future[StreamedResponse] = {
    val promise = Promise[(WSResponseHeaders, Publisher[HttpResponseBodyPart])]()
    client.executeRequest(request, new DefaultStreamedAsyncHandler(promise))
    import play.api.libs.iteratee.Execution.Implicits.trampoline
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

  // This method was introduced because in Play we have utilities that makes it easy to convert a `Publisher` into an `Enumerator`, 
  // while it's not as easy to convert an akka-stream Source to a reactive-streams `Publisher` (as it requires materialization of 
  // the stream). This is why `DefaultStreamedAsyncHandler`'s constructor takes a `Promise[(WSResponseHeaders, Publisher[HttpResponseBodyPart])]` 
  // and not a `Promise[(WSResponseHeaders, Source[ByteString])]`. In fact, the moment this method is removed, we should refactor the 
  // `DefaultStreamedAsyncHandler`' constructor parameter's type to the latter.
  // This method is `deprecated` because we should remember to remove it together with `AhcWSRequest.streamWithEnumerator`.
  @deprecated("2.5", "Use `execute()` instead.")
  def execute2(client: AsyncHttpClient, request: Request): Future[(WSResponseHeaders, Enumerator[Array[Byte]])] = {
    val promise = Promise[(WSResponseHeaders, Publisher[HttpResponseBodyPart])]()
    client.executeRequest(request, new DefaultStreamedAsyncHandler(promise))
    import play.api.libs.iteratee.Execution.Implicits.trampoline
    promise.future.map {
      case (headers, publisher) =>
        val enumerator = Streams.publisherToEnumerator(publisher).map(_.getBodyPartBytes)
        (headers, enumerator)
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
