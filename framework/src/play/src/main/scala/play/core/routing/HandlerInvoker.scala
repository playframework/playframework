/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.routing

import org.apache.commons.lang3.reflect.MethodUtils
import play.api.mvc._
import play.core.j.{ JavaHandlerComponents, JavaHandler, JavaActionAnnotations }

import scala.util.control.NonFatal

/**
 * An object that, when invoked with a thunk, produces a `Handler` that wraps
 * that thunk. Constructed by a `HandlerInvokerFactory`.
 */
trait HandlerInvoker[T] {
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
private class TaggingInvoker[A](underlyingInvoker: HandlerInvoker[A], handlerDef: HandlerDef) extends HandlerInvoker[A] {
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
      case ws @ WebSocket(f) =>
        WebSocket[ws.FramesIn, ws.FramesOut](rh => ws.f(taggedRequest(rh, cachedHandlerTags)))(ws.inFormatter, ws.outFormatter)
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
trait HandlerInvokerFactory[T] {
  /**
   * Create an invoker for the given thunk that is never called.
   * @param fakeCall A simulated call to the controller method. Needed to
   * so implicit resolution can use the controller method's return type,
   * but *never actually called*.
   */
  def createInvoker(fakeCall: => T, handlerDef: HandlerDef): HandlerInvoker[T]
}

object HandlerInvokerFactory {

  import play.libs.F.{ Promise => JPromise }
  import play.mvc.{ Result => JResult, WebSocket => JWebSocket }
  import play.core.j.JavaWebSocket
  import com.fasterxml.jackson.databind.JsonNode

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
          val parser = cachedAnnotations.parser
          def invocation: JPromise[JResult] = resultCall(call)
          def tagRequest(rh: RequestHeader) = taggedRequest(rh, cachedHandlerTags)
        }
      }
    }
    def resultCall(call: => A): JPromise[JResult]
  }

  implicit def wrapJava: HandlerInvokerFactory[JResult] = new JavaActionInvokerFactory[JResult] {
    def resultCall(call: => JResult) = JPromise.pure(call)
  }
  implicit def wrapJavaPromise: HandlerInvokerFactory[JPromise[JResult]] = new JavaActionInvokerFactory[JPromise[JResult]] {
    def resultCall(call: => JPromise[JResult]) = call
  }

  /**
   * Create a `HandlerInvokerFactory` for a Java WebSocket.
   */
  private abstract class JavaWebSocketInvokerFactory[A, B] extends HandlerInvokerFactory[A] {
    def webSocketCall(call: => A): WebSocket[B, B]
    def createInvoker(fakeCall: => A, handlerDef: HandlerDef): HandlerInvoker[A] = new HandlerInvoker[A] {
      val cachedHandlerTags = handlerTags(handlerDef)
      def call(call: => A): WebSocket[B, B] = webSocketCall(call)
    }
  }

  implicit def javaBytesWebSocket: HandlerInvokerFactory[JWebSocket[Array[Byte]]] = new JavaWebSocketInvokerFactory[JWebSocket[Array[Byte]], Array[Byte]] {
    def webSocketCall(call: => JWebSocket[Array[Byte]]) = JavaWebSocket.ofBytes(call)
  }

  implicit def javaStringWebSocket: HandlerInvokerFactory[JWebSocket[String]] = new JavaWebSocketInvokerFactory[JWebSocket[String], String] {
    def webSocketCall(call: => JWebSocket[String]) = JavaWebSocket.ofString(call)
  }

  implicit def javaJsonWebSocket: HandlerInvokerFactory[JWebSocket[JsonNode]] = new JavaWebSocketInvokerFactory[JWebSocket[JsonNode], JsonNode] {
    def webSocketCall(call: => JWebSocket[JsonNode]) = JavaWebSocket.ofJson(call)
  }

  implicit def javaBytesPromiseWebSocket: HandlerInvokerFactory[JPromise[JWebSocket[Array[Byte]]]] = new JavaWebSocketInvokerFactory[JPromise[JWebSocket[Array[Byte]]], Array[Byte]] {
    def webSocketCall(call: => JPromise[JWebSocket[Array[Byte]]]) = JavaWebSocket.promiseOfBytes(call)
  }

  implicit def javaStringPromiseWebSocket: HandlerInvokerFactory[JPromise[JWebSocket[String]]] = new JavaWebSocketInvokerFactory[JPromise[JWebSocket[String]], String] {
    def webSocketCall(call: => JPromise[JWebSocket[String]]) = JavaWebSocket.promiseOfString(call)
  }

  implicit def javaJsonPromiseWebSocket: HandlerInvokerFactory[JPromise[JWebSocket[JsonNode]]] = new JavaWebSocketInvokerFactory[JPromise[JWebSocket[JsonNode]], JsonNode] {
    def webSocketCall(call: => JPromise[JWebSocket[JsonNode]]) = JavaWebSocket.promiseOfJson(call)
  }
}
