/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.testhelpers

import play.core.j.{JavaHelpers, JavaActionAnnotations, JavaAction}
import play.mvc.{Http, Controller, Result}
import play.test.FakeRequest
import play.api.test.Helpers
import play.libs.F
import java.lang.reflect.Method

abstract class MockJavaAction extends Controller with JavaAction {

  val controller = this.getClass
  val method = MockJavaActionJavaMocker.findActionMethod(this)
  val annotations = new JavaActionAnnotations(controller, method)

  def parser = annotations.parser

  def invocation = {
    method.invoke(this) match {
      case r: Result => F.Promise.pure(r)
      case f: F.Promise[_] => f.asInstanceOf[F.Promise[Result]]
    }
  }
}

object MockJavaAction {
  import Helpers.defaultAwaitTimeout

  def call(action: JavaAction, request: FakeRequest) = {
    val result = Helpers.await(action.apply(request.getWrappedRequest))
    new Result {
      def toScala = result
    }
  }

  def callWithStringBody(action: JavaAction, request: FakeRequest, body: String) = {
    val result = Helpers.await(Helpers.call(action, request.getWrappedRequest, body))
    new Result {
      def toScala = result
    }
  }

  def setContext(request: FakeRequest) = {
    Http.Context.current.set(JavaHelpers.createJavaContext(request.getWrappedRequest))
  }

  def removeContext = Http.Context.current.remove()
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