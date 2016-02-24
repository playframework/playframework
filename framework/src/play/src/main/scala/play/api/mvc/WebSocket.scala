/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.mvc

import akka.stream.Materializer
import akka.stream.scaladsl.{ Source, Sink, Flow }
import akka.util.ByteString
import play.api.http.websocket._
import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.libs.streams.{ AkkaStreams, ActorFlow, Streams }

import scala.concurrent.{ ExecutionContext, Promise, Future }

import play.api.libs.iteratee.Execution.Implicits.trampoline
import akka.actor.{ Props, ActorRef }
import play.api.Application

import scala.util.control.NonFatal

/**
 * A WebSocket handler.
 */
trait WebSocket extends Handler {

  /**
   * Execute the WebSocket.
   *
   * The return value is either a result to reject the WebSocket with (or otherwise respond in a different way), or
   * a flow to handle the WebSocket messages.
   */
  def apply(request: RequestHeader): Future[Either[Result, Flow[Message, Message, _]]]
}

/**
 * Helper utilities to generate WebSocket results.
 */
object WebSocket {

  def apply(f: RequestHeader => Future[Either[Result, Flow[Message, Message, _]]]): WebSocket = {
    new WebSocket {
      def apply(request: RequestHeader) = f(request)
    }
  }

  /**
   * Transforms WebSocket message flows into message flows of another type.
   *
   * The transformation may be more than just converting from one message to another, it may also produce messages, such
   * as close messages with an appropriate error code if the message can't be consumed.
   */
  trait MessageFlowTransformer[+In, -Out] { self =>

    /**
     * Transform the flow of In/Out messages into a flow of WebSocket messages.
     */
    def transform(flow: Flow[In, Out, _]): Flow[Message, Message, _]

    /**
     * Contramap the out type of this transformer.
     */
    def contramap[NewOut](f: NewOut => Out): MessageFlowTransformer[In, NewOut] = {
      new MessageFlowTransformer[In, NewOut] {
        def transform(flow: Flow[In, NewOut, _]) = {
          self.transform(
            flow map f
          )
        }
      }
    }

    /**
     * Map the in type of this transformer.
     */
    def map[NewIn](f: In => NewIn): MessageFlowTransformer[NewIn, Out] = {
      new MessageFlowTransformer[NewIn, Out] {
        def transform(flow: Flow[NewIn, Out, _]) = {
          self.transform(
            Flow[In] map f via flow
          )
        }
      }
    }

    /**
     * Map the in type and contramap the out type of this transformer.
     */
    def map[NewIn, NewOut](f: In => NewIn, g: NewOut => Out): MessageFlowTransformer[NewIn, NewOut] = {
      new MessageFlowTransformer[NewIn, NewOut] {
        def transform(flow: Flow[NewIn, NewOut, _]) = {
          self.transform(
            Flow[In] map f via flow map g
          )
        }
      }
    }
  }

  object MessageFlowTransformer {

    implicit val identityMessageFlowTransformer: MessageFlowTransformer[Message, Message] = {
      new MessageFlowTransformer[Message, Message] {
        def transform(flow: Flow[Message, Message, _]) = flow
      }
    }

    /**
     * Converts text messages to/from Strings.
     */
    implicit val stringMessageFlowTransformer: MessageFlowTransformer[String, String] = {
      new MessageFlowTransformer[String, String] {
        def transform(flow: Flow[String, String, _]) = {
          AkkaStreams.bypassWith[Message, String, Message](Flow[Message] collect {
            case TextMessage(text) => Left(text)
            case BinaryMessage(_) =>
              Right(CloseMessage(Some(CloseCodes.Unacceptable),
                "This WebSocket only supports text frames"))
          })(flow map TextMessage.apply)
        }
      }
    }

    /**
     * Converts binary messages to/from ByteStrings.
     */
    implicit val byteStringMessageFlowTransformer: MessageFlowTransformer[ByteString, ByteString] = {
      new MessageFlowTransformer[ByteString, ByteString] {
        def transform(flow: Flow[ByteString, ByteString, _]) = {
          AkkaStreams.bypassWith[Message, ByteString, Message](Flow[Message] collect {
            case BinaryMessage(data) => Left(data)
            case TextMessage(_) =>
              Right(CloseMessage(Some(CloseCodes.Unacceptable),
                "This WebSocket only supports binary frames"))
          })(flow map BinaryMessage.apply)
        }
      }
    }

    /**
     * Converts binary messages to/from byte arrays.
     */
    implicit val byteArrayMessageFlowTransformer: MessageFlowTransformer[Array[Byte], Array[Byte]] = {
      byteStringMessageFlowTransformer.map(_.toArray, ByteString.apply)
    }

    /**
     * Converts messages to/from JsValue
     */
    implicit val jsonMessageFlowTransformer: MessageFlowTransformer[JsValue, JsValue] = {
      def closeOnException[T](block: => T) = try {
        Left(block)
      } catch {
        case NonFatal(e) => Right(CloseMessage(Some(CloseCodes.Unacceptable),
          "Unable to parse json message"))
      }

      new MessageFlowTransformer[JsValue, JsValue] {
        def transform(flow: Flow[JsValue, JsValue, _]) = {
          AkkaStreams.bypassWith[Message, JsValue, Message](Flow[Message].collect {
            case BinaryMessage(data) => closeOnException(Json.parse(data.iterator.asInputStream))
            case TextMessage(text) => closeOnException(Json.parse(text))
          })(flow map { json => TextMessage(Json.stringify(json)) })
        }
      }
    }

    /**
     * Converts messages to/from a JSON high level object.
     *
     * If the input messages fail to be parsed, the WebSocket will be closed with an 1003 close code and the parse error
     * serialised to JSON.
     */
    def jsonMessageFlowTransformer[In: Reads, Out: Writes]: MessageFlowTransformer[In, Out] = {
      jsonMessageFlowTransformer.map(json => Json.fromJson[In](json).fold({ errors =>
        throw WebSocketCloseException(CloseMessage(Some(CloseCodes.Unacceptable), Json.stringify(JsError.toJson(errors))))
      }, identity), out => Json.toJson(out))
    }
  }

  @deprecated("Use MessageFlowTransformer instead", "2.5.0")
  type FrameFormatter[A] = MessageFlowTransformer[A, A]

  /**
   * Defaults frame formatters.
   */
  object FrameFormatter {

    /**
     * Json WebSocket frames, parsed into/formatted from objects of type A.
     */
    @deprecated("Use MessageFlowTransformer.jsonMessageFlowTransformer instead", "2.5.0")
    def jsonFrame[A: Format]: MessageFlowTransformer[A, A] = MessageFlowTransformer.jsonMessageFlowTransformer[A, A]
  }

  /**
   * Accepts a WebSocket using the given inbound/outbound channels.
   */
  @deprecated("Use accept with an Akka streams flow instead", "2.5.0")
  def using[A](f: RequestHeader => (Iteratee[A, _], Enumerator[A]))(implicit frameFormatter: MessageFlowTransformer[A, A]): WebSocket = {
    tryAccept[A](f.andThen(handler => Future.successful(Right(handler))))
  }

  /**
   * Creates a WebSocket that will adapt the incoming stream and send it back out.
   */
  @deprecated("Use accept with an Akka streams flow instead", "2.5.0")
  def adapter[A](f: RequestHeader => Enumeratee[A, A])(implicit transformer: MessageFlowTransformer[A, A]): WebSocket = {
    using(f.andThen { enumeratee =>
      val (iteratee, enumerator) = Concurrent.joined[A]
      (enumeratee &> iteratee, enumerator)
    })
  }

  /**
   * Creates an action that will either reject the websocket with the given result, or will be handled by the given
   * inbound and outbound channels, asynchronously
   */
  @deprecated("Use acceptOrResult with an Akka streams flow instead", "2.5.0")
  def tryAccept[A](f: RequestHeader => Future[Either[Result, (Iteratee[A, _], Enumerator[A])]])(implicit transformer: MessageFlowTransformer[A, A]): WebSocket = {
    acceptOrResult[A, A](f.andThen(_.map(_.right.map {
      case (iteratee, enumerator) =>
        // Play 2.4 and earlier only closed the WebSocket if the enumerator specifically fed EOF. So, you could
        // return an empty enumerator, and it would never close the socket. Converting an empty enumerator to a
        // publisher however will close the socket, so, we need to ensure the enumerator only completes if EOF
        // is sent.
        val enumeratorCompletion = Promise[Enumerator[A]]()
        val nonCompletingEnumerator = onEOF(enumerator, () => {
          enumeratorCompletion.success(Enumerator.empty)
        }) >>> Enumerator.flatten(enumeratorCompletion.future)
        val publisher = Streams.enumeratorToPublisher(nonCompletingEnumerator)
        val (subscriber, _) = Streams.iterateeToSubscriber(iteratee)
        Flow.fromSinkAndSource(Sink.fromSubscriber(subscriber), Source.fromPublisher(publisher))
    })))
  }

  /**
   * Accepts a WebSocket using the given flow.
   */
  def accept[In, Out](f: RequestHeader => Flow[In, Out, _])(implicit transformer: MessageFlowTransformer[In, Out]): WebSocket = {
    acceptOrResult(f.andThen(flow => Future.successful(Right(flow))))
  }

  /**
   * Creates an action that will either accept the websocket, using the given flow to handle the in and out stream, or
   * return a result to reject the Websocket.
   */
  def acceptOrResult[In, Out](f: RequestHeader => Future[Either[Result, Flow[In, Out, _]]])(implicit transformer: MessageFlowTransformer[In, Out]): WebSocket = {
    WebSocket { request =>
      f(request).map(_.right.map(transformer.transform))
    }
  }

  /**
   * A function that, given an actor to send upstream messages to, returns actor props to create an actor to handle
   * the WebSocket
   */
  type HandlerProps = ActorRef => Props

  /**
   * Create a WebSocket that will pass messages to/from the actor created by the given props.
   *
   * Given a request and an actor ref to send messages to, the function passed should return the props for an actor
   * to create to handle this WebSocket.
   *
   * For example:
   *
   * {{{
   *   def webSocket = WebSocket.acceptWithActor[JsValue, JsValue] { req => out =>
   *     MyWebSocketActor.props(out)
   *   }
   * }}}
   */
  @deprecated("Use accept with a flow that wraps a Sink.actorRef and Source.actorRef, or play.api.libs.Streams.ActorFlow.actorRef", "2.5.0")
  def acceptWithActor[In, Out](f: RequestHeader => HandlerProps)(implicit transformer: MessageFlowTransformer[In, Out],
    app: Application, mat: Materializer): WebSocket = {
    tryAcceptWithActor { req =>
      Future.successful(Right((actorRef) => f(req)(actorRef)))
    }
  }

  /**
   * Create a WebSocket that will pass messages to/from the actor created by the given props asynchronously.
   *
   * Given a request, this method should return a future of either:
   *
   * - A result to reject the WebSocket with, or
   * - A function that will take the sending actor, and create the props that describe the actor to handle this
   * WebSocket
   *
   * For example:
   *
   * {{{
   *   def subscribe = WebSocket.tryAcceptWithActor[JsValue, JsValue] { req =>
   *     val isAuthenticated: Future[Boolean] = authenticate(req)
   *     isAuthenticated.map {
   *       case false => Left(Forbidden)
   *       case true => Right(MyWebSocketActor.props)
   *     }
   *   }
   * }}}
   */
  @deprecated("Use acceptOrResult with a flow that wraps a Sink.actorRef and Source.actorRef, or play.api.libs.Streams.ActorFlow.actorRef", "2.5.0")
  def tryAcceptWithActor[In, Out](f: RequestHeader => Future[Either[Result, HandlerProps]])(implicit transformer: MessageFlowTransformer[In, Out],
    app: Application, mat: Materializer): WebSocket = {

    implicit val system = app.actorSystem

    acceptOrResult(f.andThen(_.map(_.right.map { props =>
      ActorFlow.actorRef(props)
    })))
  }

  /**
   * Like Enumeratee.onEOF, however enumeratee.onEOF always gets fed an EOF (by the enumerator if nothing else).
   */
  private def onEOF[E](enumerator: Enumerator[E], action: () => Unit): Enumerator[E] = new Enumerator[E] {
    def apply[A](i: Iteratee[E, A]) = enumerator(wrap(i))

    def wrap[A](i: Iteratee[E, A]): Iteratee[E, A] = new Iteratee[E, A] {
      def fold[B](folder: (Step[E, A]) => Future[B])(implicit ec: ExecutionContext) = i.fold {
        case Step.Cont(k) => folder(Step.Cont {
          case eof @ Input.EOF =>
            action()
            wrap(k(eof))
          case other => wrap(k(other))
        })
        case other => folder(other)
      }(ec)
    }
  }
}
