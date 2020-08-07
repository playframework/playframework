/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.routing

import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

import akka.stream.scaladsl.Flow
import play.api.http.ActionCompositionConfiguration
import play.api.mvc._
import play.api.routing.HandlerDef

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
  import play.mvc.{ Result => JResult }
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
}
