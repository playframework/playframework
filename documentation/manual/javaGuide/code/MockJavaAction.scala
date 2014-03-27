/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.testhelpers

import play.core.j.{JavaHelpers, JavaActionAnnotations, JavaAction}
import play.mvc.{Http, Controller, Result}
import play.test.FakeRequest
import play.api.test.Helpers
import play.libs.F

abstract class MockJavaAction extends Controller with JavaAction {

  val controller = this.getClass
  val method = {
    val m = this.getClass.getDeclaredMethods()(0)
    m.setAccessible(true)
    m
  }
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
