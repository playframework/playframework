package play.it.http

import play.api.mvc.EssentialAction
import play.core.j.{JavaActionAnnotations, JavaAction}
import play.mvc.{Results, Http, Controller, Result}
import play.libs.F.Promise
import org.apache.commons.lang3.reflect.MethodUtils

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
      lazy val annotations = {
        val controller = c.getClass
        val method = c.getClass.getMethod("action")
        new JavaActionAnnotations(controller, method)
      }
      def invocation = Promise.pure(c.action)
      def parser = annotations.parser
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
