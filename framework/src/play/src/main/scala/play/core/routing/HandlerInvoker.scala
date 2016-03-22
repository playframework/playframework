/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.routing

import java.util.Optional
import java.util.concurrent.{CompletableFuture, CompletionStage}

import akka.stream.scaladsl.Flow
import org.apache.commons.lang3.reflect.MethodUtils
import play.api.mvc._
import play.core.j
import play.core.j.{JavaActionAnnotations, JavaHandler, JavaHandlerComponents}
import play.mvc.Http.{Context, RequestBody}

import scala.compat.java8.{FutureConverters, OptionConverters}
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
 * An invoker that wraps another invoker, ensuring the request is tagged appropriately.
 */
private class TaggingInvoker[-A](underlyingInvoker: HandlerInvoker[A], handlerDef: HandlerDef) extends HandlerInvoker[A] {
  import HandlerInvokerFactory._
  val cachedHandlerTags = handlerTags(handlerDef)
  def call(call: => A): Handler = {
    val handler = underlyingInvoker.call(call)
    // All JavaAction's should already be tagged
    handler match {
      case alreadyTagged: RequestTaggingHandler => alreadyTagged
      case action: EssentialAction => new EssentialAction with RequestTaggingHandler {
        def apply(rh: RequestHeader) = action(rh)
        def tagRequest(rh: RequestHeader) = taggedRequest(rh, cachedHandlerTags)
      }
      case ws: WebSocket =>
        WebSocket(rh => ws(taggedRequest(rh, cachedHandlerTags)))
      case other => other
    }
  }
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

  import com.fasterxml.jackson.databind.JsonNode
  import play.core.j.JavaWebSocket
  import play.mvc.{LegacyWebSocket, Result => JResult, WebSocket => JWebSocket}

  private[routing] def handlerTags(handlerDef: HandlerDef): Map[String, String] = Map(
    play.api.routing.Router.Tags.RoutePattern -> handlerDef.path,
    play.api.routing.Router.Tags.RouteVerb -> handlerDef.verb,
    play.api.routing.Router.Tags.RouteController -> handlerDef.controller,
    play.api.routing.Router.Tags.RouteActionMethod -> handlerDef.method,
    play.api.routing.Router.Tags.RouteComments -> handlerDef.comments
  )

  private[routing] def taggedRequest(rh: RequestHeader, tags: Map[String, String]): RequestHeader = {
    val newTags = if (rh.tags.isEmpty) tags else rh.tags ++ tags
    rh.copy(tags = newTags)
  }

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
   * Create a `HandlerInvokerFactory` for a Java action. Caches the
   * tags and annotations.
   */
  private abstract class JavaActionInvokerFactory[A] extends HandlerInvokerFactory[A] {
    def createInvoker(fakeCall: => A, handlerDef: HandlerDef): HandlerInvoker[A] = new HandlerInvoker[A] {
      val cachedHandlerTags = handlerTags(handlerDef)
      val cachedAnnotations = {
        val controller = loadJavaControllerClass(handlerDef)
        val method = MethodUtils.getMatchingAccessibleMethod(controller, handlerDef.method, handlerDef.parameterTypes: _*)
        new JavaActionAnnotations(controller, method)
      }
      def call(call: => A): Handler = new JavaHandler {
        def withComponents(components: JavaHandlerComponents) = new play.core.j.JavaAction(components) with RequestTaggingHandler {
          val annotations = cachedAnnotations
          val parser = {
            val javaParser = components.getBodyParser(cachedAnnotations.parser)
            javaBodyParserToScala(javaParser)
          }
          def invocation: CompletionStage[JResult] = resultCall(call)
          def tagRequest(rh: RequestHeader) = taggedRequest(rh, cachedHandlerTags)
        }
      }
    }
    def resultCall(call: => A): CompletionStage[JResult]
  }

  private[play] def javaBodyParserToScala(parser: play.mvc.BodyParser[_]): BodyParser[RequestBody] = BodyParser { request =>
    val accumulator = parser.apply(new play.core.j.RequestHeaderImpl(request)).asScala()
    import play.api.libs.iteratee.Execution.Implicits.trampoline
    accumulator.map { javaEither =>
      if (javaEither.left.isPresent) {
        Left(javaEither.left.get().asScala())
      } else {
        Right(new RequestBody(javaEither.right.get()))
      }
    }
  }

  implicit def wrapJava: HandlerInvokerFactory[JResult] = new JavaActionInvokerFactory[JResult] {
    def resultCall(call: => JResult) = CompletableFuture.completedFuture(call)
  }
  implicit def wrapJavaPromise: HandlerInvokerFactory[CompletionStage[JResult]] = new JavaActionInvokerFactory[CompletionStage[JResult]] {
    def resultCall(call: => CompletionStage[JResult]) = call
  }

  /**
   * Create a `HandlerInvokerFactory` for a Java WebSocket.
   */
  private abstract class JavaWebSocketInvokerFactory[A, B] extends HandlerInvokerFactory[A] {
    def webSocketCall(call: => A): WebSocket
    def createInvoker(fakeCall: => A, handlerDef: HandlerDef): HandlerInvoker[A] = new HandlerInvoker[A] {
      val cachedHandlerTags = handlerTags(handlerDef)
      def call(call: => A): WebSocket = webSocketCall(call)
    }
  }

  implicit def javaBytesWebSocket: HandlerInvokerFactory[LegacyWebSocket[Array[Byte]]] = new JavaWebSocketInvokerFactory[LegacyWebSocket[Array[Byte]], Array[Byte]] {
    def webSocketCall(call: => LegacyWebSocket[Array[Byte]]) = JavaWebSocket.ofBytes(call)
  }

  implicit def javaStringWebSocket: HandlerInvokerFactory[LegacyWebSocket[String]] = new JavaWebSocketInvokerFactory[LegacyWebSocket[String], String] {
    def webSocketCall(call: => LegacyWebSocket[String]) = JavaWebSocket.ofString(call)
  }

  implicit def javaJsonWebSocket: HandlerInvokerFactory[LegacyWebSocket[JsonNode]] = new JavaWebSocketInvokerFactory[LegacyWebSocket[JsonNode], JsonNode] {
    def webSocketCall(call: => LegacyWebSocket[JsonNode]) = JavaWebSocket.ofJson(call)
  }

  implicit def javaBytesPromiseWebSocket: HandlerInvokerFactory[CompletionStage[LegacyWebSocket[Array[Byte]]]] = new JavaWebSocketInvokerFactory[CompletionStage[LegacyWebSocket[Array[Byte]]], Array[Byte]] {
    def webSocketCall(call: => CompletionStage[LegacyWebSocket[Array[Byte]]]) = JavaWebSocket.promiseOfBytes(call)
  }

  implicit def javaStringPromiseWebSocket: HandlerInvokerFactory[CompletionStage[LegacyWebSocket[String]]] = new JavaWebSocketInvokerFactory[CompletionStage[LegacyWebSocket[String]], String] {
    def webSocketCall(call: => CompletionStage[LegacyWebSocket[String]]) = JavaWebSocket.promiseOfString(call)
  }

  implicit def javaJsonPromiseWebSocket: HandlerInvokerFactory[CompletionStage[LegacyWebSocket[JsonNode]]] = new JavaWebSocketInvokerFactory[CompletionStage[LegacyWebSocket[JsonNode]], JsonNode] {
    def webSocketCall(call: => CompletionStage[LegacyWebSocket[JsonNode]]) = JavaWebSocket.promiseOfJson(call)
  }

  implicit def javaWebSocket: HandlerInvokerFactory[JWebSocket] = new HandlerInvokerFactory[JWebSocket] {
    import play.api.http.websocket._
    import play.api.libs.iteratee.Execution.Implicits.trampoline
    import play.http.websocket.{Message => JMessage}

    def createInvoker(fakeCall: => JWebSocket, handlerDef: HandlerDef) = new HandlerInvoker[JWebSocket] {
      def call(call: => JWebSocket) = WebSocket.acceptOrResult[Message, Message] { request =>

        val javaContext = JavaWebSocket.createJavaContext(request)

        val callWithContext = {
          try {
            Context.current.set(javaContext)
            FutureConverters.toScala(call(new j.RequestHeaderImpl(request)))
          } finally {
            Context.current.remove()
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
