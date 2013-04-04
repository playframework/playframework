package javaguide.http

import play.core.j.JavaAction
import play.mvc.{Controller, Result}
import play.test.{FakeRequest, Helpers}
import play.api.mvc.HandlerRef

abstract class MockJavaAction extends Controller with JavaAction {

  def invocation = method.invoke(null).asInstanceOf[Result]

  def controller = this.getClass

  def method = this.getClass.getDeclaredMethods()(0)
}

object MockJavaAction {
  def call(action: JavaAction, request: FakeRequest) = {
    val result = action.apply(request.getWrappedRequest)
    new Result {
      def getWrappedResult = result
      override def toString = result.toString
    }
  }
}
