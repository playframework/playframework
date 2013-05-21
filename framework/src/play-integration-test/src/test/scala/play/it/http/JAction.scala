package play.it.http

import play.api.mvc.EssentialAction
import play.core.j.{JavaActionAnnotations, JavaAction}
import play.mvc.{Results, Http, Controller, Result}

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
  def apply(c: MockController): EssentialAction = {
    new JavaAction {
      val annotations = new JavaActionAnnotations(c.getClass, c.getClass.getMethod("action"))
      val parser = annotations.parser
      def invocation = c.action
    }
  }
}

abstract class MockController {
  def action: Result

  def ctx = Http.Context.current()
  def response = ctx.response()
  def request = ctx.request()
  def session = ctx.session()
  def flash = ctx.flash()
}
