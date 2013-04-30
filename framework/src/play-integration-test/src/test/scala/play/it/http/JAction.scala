package play.it.http

import play.api.mvc.EssentialAction
import play.core.j.{JavaActionAnnotations, JavaAction}
import play.mvc.{ Http, Result}
import play.libs.F.Promise

/**
 * Use this to mock Java actions, eg:
 *
 * {{{
 *   new FakeApplication(
 *     withRouter = {
 *       case _ => JAction(new MockController() {
 *         @Security.Authenticated
 *         def action = ok
 *       })
 *     }
 *   }
 * }}}
 */
object JAction {
  def apply(c: AbstractMockController): EssentialAction = {
    new JavaAction {
      lazy val annotations = {
        val controller = c.getClass
        val method = c.getClass.getMethod("action")
        new JavaActionAnnotations(controller, method)
      }
      def invocation = c.invocation
      def parser = annotations.parser
    }
  }
}

trait AbstractMockController {
  def invocation: Promise[Result]

  def ctx = Http.Context.current()
  def response = ctx.response()
  def request = ctx.request()
  def session = ctx.session()
  def flash = ctx.flash()
}

abstract class MockController extends AbstractMockController {
  def action: Result
  def invocation: Promise[Result] = Promise.pure(action)
}

abstract class AsyncMockController extends AbstractMockController {
  def action: Promise[Result]
  def invocation: Promise[Result] = action
}
