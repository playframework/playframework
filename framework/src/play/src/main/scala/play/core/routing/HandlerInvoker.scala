/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.routing

import java.util.Optional
import java.util.concurrent.{ CompletableFuture, CompletionStage }

import akka.stream.scaladsl.Flow
import play.api.http.ActionCompositionConfiguration
import play.api.mvc._
import play.api.routing.HandlerDef
import play.core.j._
import play.libs.reflect.MethodUtils
import play.mvc.Http.{ Context, RequestBody }

import scala.compat.java8.{ FutureConverters, OptionConverters }
import scala.util.control.NonFatal

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

  import play.mvc.{ Result => JResult, WebSocket => JWebSocket }
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
      handlerDef.classLoader.loadClass(handlerDef.controller)
    } catch {
      case e: ClassNotFoundException =>
        // Try looking up relative to the routers package name.
        // This was primarily implemented for the documentation project so that routers could be namespaced and so
        // they could reference controllers relative to their own package.
        if (handlerDef.routerPackage.length > 0) {
          try {
            handlerDef.classLoader.loadClass(handlerDef.routerPackage + "." + handlerDef.controller)
          } catch {
            case NonFatal(_) => throw e
          }
        } else throw e
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
      def cachedAnnotations(config: ActionCompositionConfiguration) = {
        if (_annotations == null) {
          val controller = loadJavaControllerClass(handlerDef)
          val method = MethodUtils.getMatchingAccessibleMethod(controller, handlerDef.method, handlerDef.parameterTypes: _*)
          _annotations = new JavaActionAnnotations(controller, method, config)
        }
        _annotations
      }

      override def call(call: => A): Handler = new JavaHandler {
        def withComponents(handlerComponents: JavaHandlerComponents): Handler = {
          new play.core.j.JavaAction(handlerComponents) {
            override val annotations = cachedAnnotations(handlerComponents.httpConfiguration.actionComposition)
            override val parser = {
              val javaParser = handlerComponents.getBodyParser(annotations.parser)
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

  private[play] def javaBodyParserToScala(parser: play.mvc.BodyParser[_]): BodyParser[RequestBody] = BodyParser { request =>
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
  implicit def wrapJavaPromise: HandlerInvokerFactory[CompletionStage[JResult]] = new JavaActionInvokerFactory[CompletionStage[JResult]] {
    def resultCall(req: JRequest, call: => CompletionStage[JResult]) = call
  }
  implicit def wrapJavaRequest: HandlerInvokerFactory[JRequest => JResult] = new JavaActionInvokerFactory[JRequest => JResult] {
    def resultCall(req: JRequest, call: => JRequest => JResult) = CompletableFuture.completedFuture(call(req))
  }
  implicit def wrapJavaPromiseRequest: HandlerInvokerFactory[JRequest => CompletionStage[JResult]] = new JavaActionInvokerFactory[JRequest => CompletionStage[JResult]] {
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

  implicit def javaWebSocket: HandlerInvokerFactory[JWebSocket] = new HandlerInvokerFactory[JWebSocket] {
    import play.api.http.websocket._
    import play.core.Execution.Implicits.trampoline
    import play.http.websocket.{ Message => JMessage }

    def createInvoker(fakeCall: => JWebSocket, handlerDef: HandlerDef) = new HandlerInvoker[JWebSocket] {
      def call(call: => JWebSocket) = new JavaHandler {
        def withComponents(handlerComponents: JavaHandlerComponents): WebSocket = {
          WebSocket.acceptOrResult[Message, Message] { request =>
            val javaContext = JavaHelpers.createJavaContext(request, handlerComponents.contextComponents)

            val callWithContext = {
              try {
                Context.setCurrent(javaContext)
                FutureConverters.toScala(call(request.asJava))
              } finally {
                Context.clear()
              }
            }

            callWithContext.map { resultOrFlow =>
              if (resultOrFlow.left.isPresent) {
                Left(resultOrFlow.left.get.asScala())
              } else {
                Right(Flow[Message].map {
                  case TextMessage(text) => new JMessage.Text(text)
                  case BinaryMessage(data) => new JMessage.Binary(data)
                  case PingMessage(data) => new JMessage.Ping(data)
                  case PongMessage(data) => new JMessage.Pong(data)
                  case CloseMessage(code, reason) => new JMessage.Close(OptionConverters.toJava(code).asInstanceOf[Optional[Integer]], reason)
                }.via(resultOrFlow.right.get.asScala).map {
                  case text: JMessage.Text => TextMessage(text.data)
                  case binary: JMessage.Binary => BinaryMessage(binary.data)
                  case ping: JMessage.Ping => PingMessage(ping.data)
                  case pong: JMessage.Pong => PongMessage(pong.data)
                  case close: JMessage.Close => CloseMessage(OptionConverters.toScala(close.code).asInstanceOf[Option[Int]], close.reason)
                })
              }
            }
          }
        }
      }
    }
  }
}
