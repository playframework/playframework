/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import scala.concurrent.Future
import scala.util.control.NonFatal

import org.apache.pekko.actor.ActorRef
import org.apache.pekko.actor.Props
import org.apache.pekko.stream.scaladsl.Flow
import org.apache.pekko.util.ByteString
import play.api.http.websocket._
import play.api.libs.json._
import play.api.libs.streams.PekkoStreams
import play.core.Execution.Implicits.trampoline

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
    (request: RequestHeader) => f(request)
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
    def contramap[NewOut](f: NewOut => Out): MessageFlowTransformer[In, NewOut] = { (flow: Flow[In, NewOut, _]) =>
      self.transform(
        flow.map(f)
      )
    }

    /**
     * Map the in type of this transformer.
     */
    def map[NewIn](f: In => NewIn): MessageFlowTransformer[NewIn, Out] = { (flow: Flow[NewIn, Out, _]) =>
      self.transform(
        Flow[In].map(f).via(flow)
      )
    }

    /**
     * Map the in type and contramap the out type of this transformer.
     */
    def map[NewIn, NewOut](f: In => NewIn, g: NewOut => Out): MessageFlowTransformer[NewIn, NewOut] = {
      (flow: Flow[NewIn, NewOut, _]) =>
        {
          self.transform(
            Flow[In].map(f).via(flow).map(g)
          )
        }
    }
  }

  object MessageFlowTransformer {
    implicit val identityMessageFlowTransformer: MessageFlowTransformer[Message, Message] = {
      (flow: Flow[Message, Message, _]) => flow
    }

    /**
     * Converts text messages to/from Strings.
     */
    implicit val stringMessageFlowTransformer: MessageFlowTransformer[String, String] = {
      (flow: Flow[String, String, _]) =>
        {
          PekkoStreams.bypassWith[Message, String, Message](Flow[Message].collect {
            case TextMessage(text) => Left(text)
            case BinaryMessage(_)  =>
              Right(CloseMessage(Some(CloseCodes.Unacceptable), "This WebSocket only supports text frames"))
          })(flow.map(TextMessage.apply))
        }
    }

    /**
     * Converts binary messages to/from ByteStrings.
     */
    implicit val byteStringMessageFlowTransformer: MessageFlowTransformer[ByteString, ByteString] = {
      (flow: Flow[ByteString, ByteString, _]) =>
        {
          PekkoStreams.bypassWith[Message, ByteString, Message](Flow[Message].collect {
            case BinaryMessage(data) => Left(data)
            case TextMessage(_)      =>
              Right(CloseMessage(Some(CloseCodes.Unacceptable), "This WebSocket only supports binary frames"))
          })(flow.map(BinaryMessage.apply))
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
      def closeOnException[T](block: => T) =
        try {
          Left(block)
        } catch {
          case NonFatal(e) => Right(CloseMessage(Some(CloseCodes.Unacceptable), "Unable to parse json message"))
        }

      (flow: Flow[JsValue, JsValue, _]) => {
        PekkoStreams.bypassWith[Message, JsValue, Message](Flow[Message].collect {
          case BinaryMessage(data) => closeOnException(Json.parse(data.iterator.asInputStream))
          case TextMessage(text)   => closeOnException(Json.parse(text))
        })(flow.map { json => TextMessage(Json.stringify(json)) })
      }
    }

    /**
     * Converts messages to/from a JSON high level object.
     *
     * If the input messages fail to be parsed, the WebSocket will be closed with an 1003 close code and the parse error
     * serialised to JSON.
     */
    def jsonMessageFlowTransformer[In: Reads, Out: Writes]: MessageFlowTransformer[In, Out] = {
      jsonMessageFlowTransformer.map(
        json =>
          Json
            .fromJson[In](json)
            .fold(
              { errors =>
                throw WebSocketCloseException(
                  CloseMessage(Some(CloseCodes.Unacceptable), Json.stringify(JsError.toJson(errors)))
                )
              },
              identity
            ),
        out => Json.toJson(out)
      )
    }
  }

  /**
   * Accepts a WebSocket using the given flow.
   */
  def accept[In, Out](
      f: RequestHeader => Flow[In, Out, _]
  )(implicit transformer: MessageFlowTransformer[In, Out]): WebSocket = {
    acceptOrResult(f.andThen(flow => Future.successful(Right(flow))))
  }

  /**
   * Creates an action that will either accept the websocket, using the given flow to handle the in and out stream, or
   * return a result to reject the Websocket.
   */
  def acceptOrResult[In, Out](
      f: RequestHeader => Future[Either[Result, Flow[In, Out, _]]]
  )(implicit transformer: MessageFlowTransformer[In, Out]): WebSocket = {
    WebSocket { request => f(request).map(_.map(transformer.transform)) }
  }

  /**
   * A function that, given an actor to send upstream messages to, returns actor props to create an actor to handle
   * the WebSocket
   */
  type HandlerProps = ActorRef => Props
}
