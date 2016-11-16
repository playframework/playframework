/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */

import java.util.concurrent.{CompletionStage, CompletableFuture}

import play.api.mvc.{Action, Request}
import play.core.j._
import play.http.DefaultActionCreator
import play.mvc.{Controller, Http, Result}
import play.api.http.HttpConfiguration
import play.api.test.Helpers
import play.i18n.{Langs => JLangs, MessagesApi => JMessagesApi}
import java.lang.reflect.Method

import akka.stream.Materializer

package javaguide.testhelpers {

abstract class MockJavaAction extends play.MockJavaAction

object MockJavaActionHelper {

  import Helpers.defaultAwaitTimeout

  def call(action: Action[Http.RequestBody], requestBuilder: play.mvc.Http.RequestBuilder)(implicit mat: Materializer): Result = {
    Helpers.await(requestBuilder.body() match {
      case null =>
        action.apply(requestBuilder.build()._underlyingRequest)
      case other =>
        Helpers.call(action, requestBuilder.build()._underlyingRequest, other.asBytes())
    }).asJava
  }

  def callWithStringBody(action: Action[Http.RequestBody], requestBuilder: play.mvc.Http.RequestBuilder, body: String)(implicit mat: Materializer): Result = {
    Helpers.await(Helpers.call(action, requestBuilder.build()._underlyingRequest, body)).asJava
  }

  def setContext(request: play.mvc.Http.RequestBuilder, cc: JavaContextComponents): Unit = {
    Http.Context.current.set(JavaHelpers.createJavaContext(request.build()._underlyingRequest, cc))
  }

  def removeContext(): Unit = Http.Context.current.remove()
}

}

/**
 * HandlerInvokerFactory is private[play]
 */
package play {

trait MockJavaAction extends Controller with Action[Http.RequestBody] {
  self =>

  private lazy val app = play.api.Play.current

  private lazy val contextComponents = new DefaultJavaContextComponents(
    app.injector.instanceOf(classOf[JMessagesApi]),
    app.injector.instanceOf(classOf[JLangs]),
    HttpConfiguration()
  )

  private lazy val handlerComponents = new DefaultJavaHandlerComponents(
    app.injector,
    new DefaultActionCreator,
    HttpConfiguration(),
    this.executionContext,
    contextComponents
  )

  private lazy val action = new JavaAction(handlerComponents) {
    val annotations = new JavaActionAnnotations(controller, method, handlerComponents.httpConfiguration.actionComposition)

    def parser = {
      play.core.routing.HandlerInvokerFactory.javaBodyParserToScala(
        handlerComponents.getBodyParser(annotations.parser)
      )
    }

    def invocation = self.invocation
  }

  def parser = action.parser

  def apply(request: Request[Http.RequestBody]) = action.apply(request)

  private val controller = this.getClass
  private val method = MockJavaActionJavaMocker.findActionMethod(this)

  def executionContext = play.api.libs.concurrent.Execution.defaultContext

  import java.util.function.Function
  def invocation: Function[Http.Context, CompletionStage[Result]] = {
    method.invoke(this) match {
      case r: Result => new Function[Http.Context, CompletionStage[Result]] {
        def apply(ctx: Http.Context) = CompletableFuture.completedFuture(r)
      }
      case f: CompletionStage[_] => new Function[Http.Context, CompletionStage[Result]] {
        def apply(ctx: Http.Context) = f.asInstanceOf[CompletionStage[Result]]
      }
      case f: Function[_, _] => new Function[Http.Context, CompletionStage[Result]] {
        def apply(ctx: Http.Context) = f.asInstanceOf[Function[Http.Context, Any]].apply(ctx) match {
          case r: Result => CompletableFuture.completedFuture(r)
          case c: CompletionStage[_] => c.asInstanceOf[CompletionStage[Result]]
        }
      }
    }
  }
}

/**
 * Java should be mocked.
 *
 * This object exists because if you put its implementation in the MockJavaAction, then when other things go
 *
 * import static MockJavaAction.*;
 *
 * They get a compile error from javac, and it seems to be because javac is trying ot import a synthetic method
 * that it shouldn't.  Hence, this object mocks java.
 */
private object MockJavaActionJavaMocker {
  def findActionMethod(obj: AnyRef): Method = {
    val maybeMethod = obj.getClass.getDeclaredMethods.find(!_.isSynthetic)
    val theMethod = maybeMethod.getOrElse(
      throw new RuntimeException("MockJavaAction must declare at least one non synthetic method")
    )
    theMethod.setAccessible(true)
    theMethod
  }
}

}
