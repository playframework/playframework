/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.routing

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.Optional

import scala.concurrent.Future
import scala.jdk.FutureConverters._
import scala.jdk.OptionConverters._
import scala.util.control.NonFatal

import akka.stream.scaladsl.Flow
import play.api.http.ActionCompositionConfiguration
import play.api.libs.typedmap.TypedKey
import play.api.mvc._
import play.api.routing.HandlerDef
import play.core.j._
import play.libs.reflect.MethodUtils
import play.mvc.{ BodyParser => JBodyParser }
import play.mvc.Http.RequestBody

/**
 * An object that, when invoked with a thunk, produces a `Handler` that wraps
 * that thunk. Constructed by a `HandlerInvokerFactory`.
 */
trait HandlerInvoker[-T] {

  /**
   * Create a `Handler` that wraps the given thunk. The thunk won't be called
   * until the `Handler` is applied. The returned Handler will be used by
   * Play to service the request.
   */
  def call(call: => T): Handler
}

/**
 * An object that creates a `HandlerInvoker`. Used by the `createInvoker` method
 * to create a `HandlerInvoker` for each route. The `Routes.createInvoker` method looks
 * for an implicit `HandlerInvokerFactory` and uses that to create a `HandlerInvoker`.
 */
@scala.annotation.implicitNotFound("Cannot use a method returning ${T} as a Handler for requests")
trait HandlerInvokerFactory[-T] {

  /**
   * Create an invoker for the given thunk that is never called.
   * @param fakeCall A simulated call to the controller method. Needed to
   * so implicit resolution can use the controller method's return type,
   * but *never actually called*.
   */
  def createInvoker(fakeCall: => T, handlerDef: HandlerDef): HandlerInvoker[T]
}

object HandlerInvokerFactory {
  import play.mvc.{ Result => JResult }
  import play.mvc.{ WebSocket => JWebSocket }
  import play.mvc.Http.{ Request => JRequest }

  /**
   * Create a `HandlerInvokerFactory` for a call that already produces a
   * `Handler`.
   */
  implicit def passThrough[A <: Handler]: HandlerInvokerFactory[A] = new HandlerInvokerFactory[A] {
    def createInvoker(fakeCall: => A, handlerDef: HandlerDef) = new HandlerInvoker[A] {
      def call(call: => A) = call
    }
  }

  private def loadJavaControllerClass(handlerDef: HandlerDef): Class[_] = {
    try {
      handlerDef.classLoader.loadClass(handlerDef.controller.stripPrefix("_root_."))
    } catch {
      case e: ClassNotFoundException =>
        // Try looking up relative to the routers package name.
        // This was primarily implemented for the documentation project so that routers could be namespaced and so
        // they could reference controllers relative to their own package.
        if (handlerDef.routerPackage.length > 0) {
          try {
            handlerDef.classLoader.loadClass(
              handlerDef.routerPackage + "." + handlerDef.controller.stripPrefix("_root_.")
            )
          } catch {
            case NonFatal(_) => throw e
          }
        } else throw e
    }
  }

  private def cachedAnnotations(
      annotations: JavaActionAnnotations,
      config: ActionCompositionConfiguration,
      handlerDef: HandlerDef
  ): JavaActionAnnotations = {
    if (annotations == null) {
      val controller = loadJavaControllerClass(handlerDef)
      val method =
        MethodUtils.getMatchingAccessibleMethod(controller, handlerDef.method, handlerDef.parameterTypes: _*)
      new JavaActionAnnotations(controller, method, config)
    } else {
      annotations
    }
  }

  /**
   * Create a `HandlerInvokerFactory` for a Java action. Caches the annotations.
   */
  private abstract class JavaActionInvokerFactory[A] extends HandlerInvokerFactory[A] {
    override def createInvoker(fakeCall: => A, handlerDef: HandlerDef): HandlerInvoker[A] = new HandlerInvoker[A] {
      // Cache annotations, initializing on first use
      // (It's OK that this is unsynchronized since the initialization should be idempotent.)
      private var _annotations: JavaActionAnnotations = null

      override def call(call: => A): Handler = new JavaHandler {
        def withComponents(handlerComponents: JavaHandlerComponents): Handler = {
          new play.core.j.JavaAction(handlerComponents) {
            override val annotations =
              cachedAnnotations(_annotations, this.handlerComponents.httpConfiguration.actionComposition, handlerDef)
            override val parser = {
              val javaParser = this.handlerComponents.getBodyParser(annotations.parser)
              javaBodyParserToScala(javaParser)
            }
            override def invocation(req: JRequest): CompletionStage[JResult] = resultCall(req, call)
          }
        }
      }
    }

    /**
     * The core logic for this Java action.
     */
    def resultCall(req: JRequest, call: => A): CompletionStage[JResult]
  }

  private[play] def javaBodyParserToScala(parser: play.mvc.BodyParser[_]): BodyParser[RequestBody] = BodyParser {
    request =>
      import scala.language.existentials
      val accumulator = parser.apply(request.asJava).asScala()
      import play.core.Execution.Implicits.trampoline
      accumulator.map { javaEither =>
        if (javaEither.left.isPresent) {
          Left(javaEither.left.get().asScala())
        } else {
          Right(new RequestBody(javaEither.right.get()))
        }
      }
  }

  implicit def wrapJava: HandlerInvokerFactory[JResult] = new JavaActionInvokerFactory[JResult] {
    def resultCall(req: JRequest, call: => JResult) = CompletableFuture.completedFuture(call)
  }
  implicit def wrapJavaPromise: HandlerInvokerFactory[CompletionStage[JResult]] =
    new JavaActionInvokerFactory[CompletionStage[JResult]] {
      def resultCall(req: JRequest, call: => CompletionStage[JResult]) = call
    }
  implicit def wrapJavaRequest: HandlerInvokerFactory[JRequest => JResult] =
    new JavaActionInvokerFactory[JRequest => JResult] {
      def resultCall(req: JRequest, call: => JRequest => JResult) = CompletableFuture.completedFuture(call(req))
    }
  implicit def wrapJavaPromiseRequest: HandlerInvokerFactory[JRequest => CompletionStage[JResult]] =
    new JavaActionInvokerFactory[JRequest => CompletionStage[JResult]] {
      def resultCall(req: JRequest, call: => JRequest => CompletionStage[JResult]) = call(req)
    }

  /**
   * Create a `HandlerInvokerFactory` for a Java WebSocket.
   */
  private abstract class JavaWebSocketInvokerFactory[A, B] extends HandlerInvokerFactory[A] {
    def webSocketCall(call: => A): WebSocket
    def createInvoker(fakeCall: => A, handlerDef: HandlerDef): HandlerInvoker[A] = new HandlerInvoker[A] {
      override def call(call: => A): Handler = webSocketCall(call)
    }
  }

  private val PASS_THROUGH_REQUEST: TypedKey[JRequest] = TypedKey("Pass-Through-Request") // Do not make this public

  implicit def javaWebSocket: HandlerInvokerFactory[JWebSocket] = new HandlerInvokerFactory[JWebSocket] {
    import play.api.http.websocket._
    import play.core.Execution.Implicits.trampoline
    import play.http.websocket.{ Message => JMessage }

    def createInvoker(fakeCall: => JWebSocket, handlerDef: HandlerDef) = new HandlerInvoker[JWebSocket] {

      // Cache annotations, initializing on first use
      // (It's OK that this is unsynchronized since the initialization should be idempotent.)
      private var _annotations: JavaActionAnnotations = null

      def call(wsCall: => JWebSocket) = new JavaHandler {
        def withComponents(handlerComponents: JavaHandlerComponents): WebSocket = {
          WebSocket.acceptOrResult[Message, Message] { request =>
            def callWebSocketAction(req: RequestHeader) = wsCall(req.asJava).asScala.map { resultOrFlow =>
              if (resultOrFlow.left.isPresent) {
                Left(resultOrFlow.left.get.asScala())
              } else {
                Right(
                  Flow[Message]
                    .map {
                      case TextMessage(text)   => new JMessage.Text(text)
                      case BinaryMessage(data) => new JMessage.Binary(data)
                      case PingMessage(data)   => new JMessage.Ping(data)
                      case PongMessage(data)   => new JMessage.Pong(data)
                      case CloseMessage(code, reason) =>
                        new JMessage.Close(code.toJava.asInstanceOf[Optional[Integer]], reason)
                    }
                    .via(resultOrFlow.right.get.asScala)
                    .map {
                      case text: JMessage.Text     => TextMessage(text.data)
                      case binary: JMessage.Binary => BinaryMessage(binary.data)
                      case ping: JMessage.Ping     => PingMessage(ping.data)
                      case pong: JMessage.Pong     => PongMessage(pong.data)
                      case close: JMessage.Close =>
                        CloseMessage(close.code.toScala.asInstanceOf[Option[Int]], close.reason)
                    }
                )
              }
            }
            if (handlerComponents.httpConfiguration.actionComposition.includeWebSocketActions) {
              new play.core.j.JavaAction(handlerComponents) {
                override def invocation(req: JRequest): CompletionStage[JResult] = // Simulates called action method
                  CompletableFuture.completedFuture(
                    Results.Ok  // This Result does not matter, will never be used in the end
                      .addAttr( // Save request that went through the ActionCreator + annotations
                        PASS_THROUGH_REQUEST,
                        req
                      )
                      .asJava
                  )
                override val annotations = cachedAnnotations(
                  _annotations,
                  this.handlerComponents.httpConfiguration.actionComposition,
                  handlerDef
                )
                override val parser = {
                  // WebSockets do not have a body so we always ignore it and therefore we use the Empty body parser
                  val javaParser =
                    this.handlerComponents.getBodyParser(classOf[JBodyParser.Empty]) // Also see Optional.empty() below
                  javaBodyParserToScala(javaParser)
                }
              }.apply(
                // We never parse a body of a WebSocket request, Optional.empty() is also what JBodyParser.Empty returns
                Request(request, new RequestBody(Optional.empty()))
              ).flatMap(result =>
                result.attrs
                  .get(PASS_THROUGH_REQUEST)
                  .map(passedThroughRequest =>
                    // So we attached the request before therefore we know it passed through the ActionCreator + annotations
                    // and nothing did cancel the action chain so we can call the WebSocket now
                    callWebSocketAction(passedThroughRequest.asScala())
                  )
                  .getOrElse(
                    Future
                      .successful(Left(result)) // An action returned a result so we don't call the WebSocket anymore
                  )
              )
            } else {
              callWebSocketAction(request)
            }
          }
        }
      }
    }
  }
}
