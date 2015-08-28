/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.http

import play.api._
import play.api.mvc.EssentialAction
import play.core.j.{ JavaHandlerComponents, JavaActionAnnotations, JavaAction }
import play.core.routing.HandlerInvokerFactory
import play.http.DefaultHttpRequestHandler
import play.mvc.{ Http, Result }
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
  def apply(app: Application, c: AbstractMockController): EssentialAction = {
    val components = app.injector.instanceOf[JavaHandlerComponents]
    new JavaAction(components) {
      val annotations = new JavaActionAnnotations(c.getClass, c.getClass.getMethod("action"))
      val parser = HandlerInvokerFactory.javaBodyParserToScala(components.injector.instanceOf(annotations.parser))
      def invocation = c.invocation
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
