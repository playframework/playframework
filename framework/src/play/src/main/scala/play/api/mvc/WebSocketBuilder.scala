package play.api.mvc

import akka.actor.{ Props, ActorRef, ActorSystem }
import com.google.inject.Inject
import play.api.{ Configuration, Environment }
import play.api.inject.Module
import play.api.libs.iteratee._
import play.api.libs.json.{ Format, Json, JsValue }
import play.core.actors.WebSocketActor._
import play.core.Execution.Implicits.internalContext
import play.core.websocket.BasicFrameFormatter
import scala.concurrent.Future
import scala.reflect.ClassTag
import WebSocket._

trait WebSocketBuilder {

  /**
   * The WebSocket API stores a reference to an ActorSystem on which it will
   * create actors to handle websocket connections.
   */
  def actorSystem: ActorSystem

  /**
   * Create a WebSocket that will pass messages to/from the actor created by the given props.
   *
   * Given a request and an actor ref to send messages to, the function passed should return the props for an actor
   * to create to handle this WebSocket.
   *
   * For example:
   *
   * {{{
   *   def webSocket = WebSocketBuilder(actorSystem).acceptWithActor[JsValue, JsValue] { req => out =>
   *     MyWebSocketActor.props(out)
   *   }
   * }}}
   */
  def acceptWithActor[In, Out](f: RequestHeader => HandlerProps)(implicit in: FrameFormatter[In],
    out: FrameFormatter[Out], outMessageType: ClassTag[Out]): WebSocket[In, Out] = {
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
   *   def subscribe = WebSocketBuilder(actorSystem).tryAcceptWithActor[JsValue, JsValue] { req =>
   *     val isAuthenticated: Future[Boolean] = authenticate(req)
   *     isAuthenticated.map {
   *       case false => Left(Forbidden)
   *       case true => Right(MyWebSocketActor.props)
   *     }
   *   }
   * }}}
   */
  def tryAcceptWithActor[In, Out](f: RequestHeader => Future[Either[Result, HandlerProps]])(implicit in: FrameFormatter[In],
    out: FrameFormatter[Out], outMessageType: ClassTag[Out]): WebSocket[In, Out] = {
    WebSocket[In, Out] { request =>
      f(request).map { resultOrProps =>
        resultOrProps.right.map { props =>
          (enumerator, iteratee) =>
            WebSocketsExtension(actorSystem).actor !
              WebSocketsActor.Connect(request.id, enumerator, iteratee, props)
        }
      }
    }
  }

  /**
   * A function that, given an actor to send upstream messages to, returns actor props to create an actor to handle
   * the WebSocket
   */
  type HandlerProps = ActorRef => Props
}

object WebSocketBuilder {

  /**
   * Creates a default WebSocketBuilder
   *
   * @param actorSystem the actorsSystem that will be used to handle WebSocket connections
   * @return
   */
  def apply(actorSystem: ActorSystem) = new DefaultWebSocketBuilder(actorSystem)

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
}

case class DefaultWebSocketBuilder @Inject() (actorSystem: ActorSystem) extends WebSocketBuilder

class WebSocketBuilderModule extends Module {
  def bindings(environment: Environment, configuration: Configuration) = {
    Seq(
      bind[WebSocketBuilder].to[DefaultWebSocketBuilder]
    )
  }
}

trait WebSocketBuilderComponents {

  def actorSystem: ActorSystem

  lazy val webSocketBuilder = new DefaultWebSocketBuilder(actorSystem)

}
