/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

import play.api._
import play.api.mvc.EssentialAction
import play.core.j.JavaAction
import play.core.j.JavaActionAnnotations
import play.core.j.JavaHandlerComponents
import play.core.routing.HandlerInvokerFactory
import play.mvc.Http
import play.mvc.Result

/**
 * Use this to mock Java actions, eg:
 *
 * {{{
 *   new GuiceApplicationBuilder().withRouter {
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
    val handlerComponents = app.injector.instanceOf[JavaHandlerComponents]
    apply(app, c, handlerComponents)
  }
  def apply(app: Application, c: AbstractMockController, handlerComponents: JavaHandlerComponents): EssentialAction = {
    new JavaAction(handlerComponents) {
      val annotations = new JavaActionAnnotations(
        c.getClass,
        c.getClass.getMethod("action", classOf[Http.Request]),
        handlerComponents.httpConfiguration.actionComposition
      )
      val parser                        = HandlerInvokerFactory.javaBodyParserToScala(handlerComponents.getBodyParser(annotations.parser))
      def invocation(req: Http.Request) = c.invocation(req)
    }
  }
}

trait AbstractMockController {
  def invocation(request: Http.Request): CompletionStage[Result]
}

abstract class MockController extends AbstractMockController {
  def action(request: Http.Request): Result
  def invocation(request: Http.Request): CompletionStage[Result] = CompletableFuture.completedFuture(action(request))
}

abstract class AsyncMockController extends AbstractMockController {
  def action(request: Http.Request): CompletionStage[Result]
  def invocation(request: Http.Request): CompletionStage[Result] = action(request)
}
