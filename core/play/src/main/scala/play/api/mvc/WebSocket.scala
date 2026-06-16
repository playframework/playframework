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
import play.api.http.HeaderNames
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
  def apply(request: RequestHeader): Future[Either[Result, Flow[Message, Message, ?]]]

  /**
   * Execute the WebSocket, including WebSocket handshake metadata.
   *
   * The return value is either a result to reject the WebSocket with (or otherwise respond in a different way), or
   * an accepted WebSocket containing a flow to handle the WebSocket messages and optional handshake metadata.
   */
  def applyWithOptions(request: RequestHeader): Future[Either[Result, WebSocket.Accepted[Message, Message]]] = {
    apply(request).map(_.map(flow => WebSocket.Accepted(flow, WebSocket.firstRequestedSubprotocol(request))))
  }
}

/**
 * Helper utilities to generate WebSocket results.
 */
object WebSocket {
  private val ReservedHandshakeResponseHeaders = Set(
    "connection",
    "sec-websocket-accept",
    "sec-websocket-extensions",
    "sec-websocket-protocol",
    "upgrade"
  )

  private[play] def firstRequestedSubprotocol(request: RequestHeader): Option[String] = {
    request.headers
      .get("Sec-WebSocket-Protocol")
      .toSeq
      .flatMap(_.split(",").iterator.map(_.trim).filter(_.nonEmpty))
      .headOption
  }

  private[play] def allowedHandshakeResponseHeaders(headers: Headers): Seq[(String, String)] = {
    headers.headers.filterNot {
      case (name, _) =>
        ReservedHandshakeResponseHeaders(name.toLowerCase(java.util.Locale.ROOT))
    }
  }

  def apply(f: RequestHeader => Future[Either[Result, Flow[Message, Message, ?]]]): WebSocket = {
    (request: RequestHeader) => f(request)
  }

  /**
   * An accepted WebSocket, including the flow that handles WebSocket messages and optional handshake metadata.
   *
   * @param flow the flow that handles WebSocket messages
   * @param subprotocol the WebSocket subprotocol selected by the application, if any
   * @param headers additional HTTP headers to send with the WebSocket upgrade response
   * @param newCookies additional cookies to send with the WebSocket upgrade response
   */
  case class Accepted[In, Out](
      flow: Flow[In, Out, ?],
      subprotocol: Option[String] = None,
      headers: Headers = Headers(),
      newCookies: Seq[Cookie] = Seq.empty
  ) {

    /**
     * Adds headers to this WebSocket upgrade response.
     */
    def withHeaders(headers: (String, String)*): Accepted[In, Out] = {
      copy(headers = this.headers.add(headers*))
    }

    /**
     * Discards a header from this WebSocket upgrade response.
     */
    def discardingHeader(name: String): Accepted[In, Out] = {
      copy(headers = headers.remove(name))
    }

    /**
     * Adds cookies to this WebSocket upgrade response. If the response already contains cookies then cookies with the
     * same name in the new list will override existing ones.
     */
    def withCookies(cookies: Cookie*): Accepted[In, Out] = {
      val filteredCookies = newCookies.filter(cookie => !cookies.exists(_.name == cookie.name))
      if (cookies.isEmpty) this else copy(newCookies = filteredCookies ++ cookies)
    }

    /**
     * Discards cookies along this WebSocket upgrade response.
     */
    def discardingCookies(cookies: DiscardingCookie*): Accepted[In, Out] = {
      withCookies(cookies.map(_.toCookie)*)
    }

    private[play] def bakeCookies(
        cookieHeaderEncoding: CookieHeaderEncoding = new DefaultCookieHeaderEncoding()
    ): Accepted[In, Out] = {
      val allCookies = {
        val setCookieCookies =
          cookieHeaderEncoding.decodeSetCookieHeader(headers.get(HeaderNames.SET_COOKIE).getOrElse(""))
        setCookieCookies ++ newCookies
      }

      if (allCookies.isEmpty) {
        this
      } else {
        withHeaders(HeaderNames.SET_COOKIE -> cookieHeaderEncoding.encodeSetCookieHeader(allCookies))
      }
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
    def transform(flow: Flow[In, Out, ?]): Flow[Message, Message, ?]

    /**
     * Contramap the out type of this transformer.
     */
    def contramap[NewOut](f: NewOut => Out): MessageFlowTransformer[In, NewOut] = { (flow: Flow[In, NewOut, ?]) =>
      self.transform(
        flow.map(f)
      )
    }

    /**
     * Map the in type of this transformer.
     */
    def map[NewIn](f: In => NewIn): MessageFlowTransformer[NewIn, Out] = { (flow: Flow[NewIn, Out, ?]) =>
      self.transform(
        Flow[In].map(f).via(flow)
      )
    }

    /**
     * Map the in type and contramap the out type of this transformer.
     */
    def map[NewIn, NewOut](f: In => NewIn, g: NewOut => Out): MessageFlowTransformer[NewIn, NewOut] = {
      (flow: Flow[NewIn, NewOut, ?]) =>
        {
          self.transform(
            Flow[In].map(f).via(flow).map(g)
          )
        }
    }
  }

  object MessageFlowTransformer {
    implicit val identityMessageFlowTransformer: MessageFlowTransformer[Message, Message] = {
      (flow: Flow[Message, Message, ?]) => flow
    }

    /**
     * Converts text messages to/from Strings.
     */
    implicit val stringMessageFlowTransformer: MessageFlowTransformer[String, String] = {
      (flow: Flow[String, String, ?]) =>
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
      (flow: Flow[ByteString, ByteString, ?]) =>
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

      (flow: Flow[JsValue, JsValue, ?]) => {
        PekkoStreams.bypassWith[Message, JsValue, Message](Flow[Message].collect {
          case BinaryMessage(data) => closeOnException(Json.parse(data.asInputStream))
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
      f: RequestHeader => Flow[In, Out, ?]
  )(implicit transformer: MessageFlowTransformer[In, Out]): WebSocket = {
    acceptOrResult(f.andThen(flow => Future.successful(Right(flow))))
  }

  /**
   * Accepts a WebSocket using the given flow and handshake metadata.
   */
  def acceptWithOptions[In, Out](
      f: RequestHeader => Accepted[In, Out]
  )(implicit transformer: MessageFlowTransformer[In, Out]): WebSocket = {
    acceptOrResultWithOptions(f.andThen(accepted => Future.successful(Right(accepted))))
  }

  /**
   * Creates an action that will either accept the websocket, using the given flow to handle the in and out stream, or
   * return a result to reject the Websocket.
   */
  def acceptOrResult[In, Out](
      f: RequestHeader => Future[Either[Result, Flow[In, Out, ?]]]
  )(implicit transformer: MessageFlowTransformer[In, Out]): WebSocket = {
    WebSocket { request => f(request).map(_.map(transformer.transform)) }
  }

  /**
   * Creates an action that will either accept the websocket, using the given flow and handshake metadata, or return a
   * result to reject the Websocket.
   */
  def acceptOrResultWithOptions[In, Out](
      f: RequestHeader => Future[Either[Result, Accepted[In, Out]]]
  )(implicit transformer: MessageFlowTransformer[In, Out]): WebSocket = {
    new WebSocket {
      override def apply(request: RequestHeader): Future[Either[Result, Flow[Message, Message, ?]]] = {
        applyWithOptions(request).map(_.map(_.flow))
      }

      override def applyWithOptions(request: RequestHeader): Future[Either[Result, Accepted[Message, Message]]] = {
        f(request).map(_.map { accepted =>
          Accepted(transformer.transform(accepted.flow), accepted.subprotocol, accepted.headers, accepted.newCookies)
        })
      }
    }
  }

  /**
   * A function that, given an actor to send upstream messages to, returns actor props to create an actor to handle
   * the WebSocket
   */
  type HandlerProps = ActorRef => Props
}
