/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.mvc

import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._

import scala.concurrent.Future

import play.core.Execution.Implicits.internalContext
import akka.actor.{ Props, ActorRef }
import play.api.Application
import scala.reflect.ClassTag
import play.core.actors.WebSocketActor._
import play.core.websocket.BasicFrameFormatter

/**
 * A WebSocket handler.
 *
 * @tparam In the type of messages coming in
 * @tparam Out the type of messages going out
 * @param f the socket messages generator
 */
case class WebSocket[In, Out](f: RequestHeader => Future[Either[Result, (Enumerator[In], Iteratee[Out, Unit]) => Unit]])(implicit val inFormatter: WebSocket.FrameFormatter[In], val outFormatter: WebSocket.FrameFormatter[Out]) extends Handler {

  type FramesIn = In
  type FramesOut = Out

  /**
   * Returns itself, for better support in the routes file.
   *
   * @return itself
   */
  def apply() = this

}

/**
 * Helper utilities to generate WebSocket results.
 */
object WebSocket {

  /**
   * Typeclass to handle WebSocket frames format.
   */
  trait FrameFormatter[A] {

    /**
     * Transform a FrameFormatter[A] to a FrameFormatter[B]
     */
    def transform[B](fba: B => A, fab: A => B): FrameFormatter[B]

  }

  /**
   * Defaults frame formatters.
   */
  object FrameFormatter {

    /**
     * String WebSocket frames.
     */
    implicit val stringFrame: FrameFormatter[String] = BasicFrameFormatter.textFrame

    /**
     * Array[Byte] WebSocket frames.
     */
    implicit val byteArrayFrame: FrameFormatter[Array[Byte]] = BasicFrameFormatter.binaryFrame

    /**
     * Either String or Array[Byte] WebSocket frames.
     */
    implicit val mixedFrame: FrameFormatter[Either[String, Array[Byte]]] = BasicFrameFormatter.mixedFrame

    /**
     * Json WebSocket frames.
     */
    implicit val jsonFrame: FrameFormatter[JsValue] = stringFrame.transform(Json.stringify, Json.parse)

    /**
     * Json WebSocket frames, parsed into/formatted from objects of type A.
     */
    def jsonFrame[A: Format]: FrameFormatter[A] = jsonFrame.transform[A](
      out => Json.toJson(out),
      in => Json.fromJson[A](in).fold(
        error => throw new RuntimeException("Error parsing JSON: " + error),
        a => a
      )
    )
  }

  /**
   * Accepts a WebSocket using the given inbound/outbound channels.
   */
  def using[A](f: RequestHeader => (Iteratee[A, _], Enumerator[A]))(implicit frameFormatter: FrameFormatter[A]): WebSocket[A, A] = {
    tryAccept[A](f.andThen(handler => Future.successful(Right(handler))))
  }

  /**
   * Creates a WebSocket that will adapt the incoming stream and send it back out.
   */
  def adapter[A](f: RequestHeader => Enumeratee[A, A])(implicit frameFormatter: FrameFormatter[A]): WebSocket[A, A] = {
    WebSocket[A, A](h => Future.successful(Right((in, out) => { in &> f(h) |>> out })))
  }

  /**
   * Creates an action that will either reject the websocket with the given result, or will be handled by the given
   * inbound and outbound channels, asynchronously
   */
  def tryAccept[A](f: RequestHeader => Future[Either[Result, (Iteratee[A, _], Enumerator[A])]])(implicit frameFormatter: FrameFormatter[A]): WebSocket[A, A] = {
    WebSocket[A, A](f.andThen(_.map { resultOrSocket =>
      resultOrSocket.right.map {
        case (readIn, writeOut) => (e, i) => { e |>> readIn; writeOut |>> i }
      }
    }))
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
  def acceptWithActor[In, Out](f: RequestHeader => HandlerProps)(implicit in: FrameFormatter[In],
    out: FrameFormatter[Out], app: Application, outMessageType: ClassTag[Out]): WebSocket[In, Out] = {
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
  def tryAcceptWithActor[In, Out](f: RequestHeader => Future[Either[Result, HandlerProps]])(implicit in: FrameFormatter[In],
    out: FrameFormatter[Out], app: Application, outMessageType: ClassTag[Out]): WebSocket[In, Out] = {
    WebSocket[In, Out] { request =>
      f(request).map { resultOrProps =>
        resultOrProps.right.map { props =>
          (enumerator, iteratee) =>
            WebSocketsExtension(Akka.system).actor !
              WebSocketsActor.Connect(request.id, enumerator, iteratee, props)
        }
      }
    }
  }

}
