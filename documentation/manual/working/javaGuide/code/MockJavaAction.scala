/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.testhelpers {

import akka.stream.Materializer
import play.api.mvc.{Action, Request}
import play.core.j.{JavaHandlerComponents, JavaHelpers, JavaActionAnnotations, JavaAction}
import play.http.DefaultHttpRequestHandler
import play.mvc.{Controller, Http, Result}
import play.api.test.Helpers
import play.libs.F
import java.lang.reflect.Method

abstract class MockJavaAction extends Controller with Action[Http.RequestBody] {
  self =>

  private lazy val components = new JavaHandlerComponents(
    play.api.Play.current.injector, new DefaultHttpRequestHandler
  )

  private lazy val action = new JavaAction(components) {
    val annotations = new JavaActionAnnotations(controller, method)

    def parser = play.HandlerInvokerFactoryAccessor.javaBodyParserToScala(
      components.injector.instanceOf(annotations.parser)
    )

    def invocation = self.invocation
  }

  def parser = action.parser

  def apply(request: Request[Http.RequestBody]) = action.apply(request)

  private val controller = this.getClass
  private val method = MockJavaActionJavaMocker.findActionMethod(this)

  def invocation = {
    method.invoke(this) match {
      case r: Result => F.Promise.pure(r)
      case f: F.Promise[_] => f.asInstanceOf[F.Promise[Result]]
    }
  }
}

object MockJavaActionHelper {

  import Helpers.defaultAwaitTimeout

  def call(action: Action[Http.RequestBody], requestBuilder: play.mvc.Http.RequestBuilder): Result = {
    Helpers.await(action.apply(requestBuilder.build()._underlyingRequest)).asJava
  }

  def callWithStringBody(action: Action[Http.RequestBody], requestBuilder: play.mvc.Http.RequestBuilder, body: String)(implicit mat: Materializer): Result = {
    Helpers.await(Helpers.call(action, requestBuilder.build()._underlyingRequest, body)).asJava
  }

  def setContext(request: play.mvc.Http.RequestBuilder): Unit = {
    Http.Context.current.set(JavaHelpers.createJavaContext(request.build()._underlyingRequest))
  }

  def removeContext: Unit = Http.Context.current.remove()
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
object MockJavaActionJavaMocker {
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

/**
 * javaBodyParserToScala is private to play
 */
package play {

object HandlerInvokerFactoryAccessor {
  val javaBodyParserToScala = play.core.routing.HandlerInvokerFactory.javaBodyParserToScala _
}

}