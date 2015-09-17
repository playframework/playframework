package play.api.libs.ws.ning

import scala.concurrent.Future
import scala.concurrent.Promise

import org.asynchttpclient.AsyncHandler
import org.asynchttpclient.AsyncHandler.State
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.HttpResponseBodyPart
import org.asynchttpclient.HttpResponseHeaders
import org.asynchttpclient.HttpResponseStatus
import org.asynchttpclient.Request

import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.libs.iteratee.Done
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.Error
import play.api.libs.iteratee.Input.El
import play.api.libs.iteratee.Iteratee
import play.api.libs.iteratee.Step
import play.api.libs.streams.Streams
import play.api.libs.ws.DefaultWSResponseHeaders
import play.api.libs.ws.WSResponseHeaders
import play.api.libs.ws.StreamedResponse

private[play] object StreamedRequest {

  def execute(client: AsyncHttpClient, request: Request): Future[StreamedResponse] = {
    val result = executeAndReturnEnumerator(client, request)
    import play.core.Execution.Implicits.internalContext
    result.map {
      case (response, enumerator) =>
        val publisher = Streams.enumeratorToPublisher(enumerator)
        StreamedResponse(response, Source(publisher).map(ByteString(_)))
    }
  }

  def executeAndReturnEnumerator(client: AsyncHttpClient, request: Request): Future[(WSResponseHeaders, Enumerator[Array[Byte]])] = {
    val result = Promise[(WSResponseHeaders, Enumerator[Array[Byte]])]()

    val errorInStream = Promise[Unit]()

    val promisedIteratee = Promise[Iteratee[Array[Byte], Unit]]()

    @volatile var doneOrError = false
    @volatile var statusCode = 0
    @volatile var current: Iteratee[Array[Byte], Unit] = Iteratee.flatten(promisedIteratee.future)

    client.executeRequest(request, new AsyncHandler[Unit]() {
      @throws(classOf[Exception])
      override def onStatusReceived(status: HttpResponseStatus): State = {
        statusCode = status.getStatusCode
        State.CONTINUE
      }

      @throws(classOf[Exception])
      override def onHeadersReceived(h: HttpResponseHeaders): State = {
        val headers = h.getHeaders

        val responseHeader = DefaultWSResponseHeaders(statusCode, NingWSRequest.ningHeadersToMap(headers))
        val enumerator = new Enumerator[Array[Byte]]() {
          def apply[A](i: Iteratee[Array[Byte], A]) = {

            val doneIteratee = Promise[Iteratee[Array[Byte], A]]()

            import play.api.libs.iteratee.Execution.Implicits.trampoline

            // Map it so that we can complete the iteratee when it returns
            val mapped = i.map {
              a =>
                doneIteratee.trySuccess(Done(a))
                ()
            }.recover {
              // but if an error happens, we want to propogate that
              case e =>
                doneIteratee.tryFailure(e)
                throw e
            }

            // Redeem the iteratee that we promised to the AsyncHandler
            promisedIteratee.trySuccess(mapped)

            // If there's an error in the stream from upstream, then fail this returned future with that
            errorInStream.future.onFailure {
              case e => doneIteratee.tryFailure(e)
            }

            doneIteratee.future
          }
        }

        result.trySuccess((responseHeader, enumerator))
        State.CONTINUE
      }

      @throws(classOf[Exception])
      override def onBodyPartReceived(bodyPart: HttpResponseBodyPart): State = {
        if (!doneOrError) {
          import play.api.libs.concurrent.Execution.Implicits.defaultContext
          current = current.pureFlatFold {
            case Step.Done(a, e) =>
              doneOrError = true
              Done(a, e)

            case Step.Cont(k) =>
              k(El(bodyPart.getBodyPartBytes))

            case Step.Error(e, input) =>
              doneOrError = true
              Error(e, input)

          }
          State.CONTINUE
        } else {
          current = null
          // Must close underlying connection, otherwise async http client will drain the stream
          bodyPart.markUnderlyingConnectionAsToBeClosed()
          State.ABORT
        }
      }

      @throws(classOf[Exception])
      override def onCompleted(): Unit = {
        Option(current).foreach(_.run)
      }

      override def onThrowable(t: Throwable): Unit = {
        result.tryFailure(t)
        errorInStream.tryFailure(t)
      }
    })
    result.future
  }
}