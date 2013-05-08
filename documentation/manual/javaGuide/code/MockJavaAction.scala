package javaguide.testhelpers

import play.core.j.{JavaActionAnnotations, JavaAction}
import play.mvc.{Controller, Result}
import play.test.{FakeRequest, Helpers}
import play.api.mvc.HandlerRef

abstract class MockJavaAction extends Controller with JavaAction {

  def invocation = method.invoke(null).asInstanceOf[Result]

  val controller = this.getClass

  val method = this.getClass.getDeclaredMethods()(0)

  val annotations = new JavaActionAnnotations(controller, method)

  val parser = annotations.parser
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
