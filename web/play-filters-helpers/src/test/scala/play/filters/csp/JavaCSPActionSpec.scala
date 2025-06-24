/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.csp

import java.util.concurrent.CompletableFuture

import scala.reflect.ClassTag

import play.api.http.HeaderNames
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.BodyParser
import play.api.test._
import play.api.Application
import play.core.j._
import play.core.routing.HandlerInvokerFactory
import play.mvc.Controller
import play.mvc.Http
import play.mvc.Result
import play.mvc.Results

/**
 * Tests Java CSP action
 */
class JavaCSPActionSpec extends PlaySpecification {
  private def inject[T: ClassTag](implicit app: Application) = app.injector.instanceOf[T]

  private def javaHandlerComponents(implicit app: Application) = inject[JavaHandlerComponents]
  private def myAction(implicit app: Application)              = inject[JavaCSPActionSpec.MyAction]

  def javaAction[T: ClassTag](method: String, inv: Http.Request => Result)(implicit app: Application): JavaAction =
    new JavaAction(javaHandlerComponents) {
      val clazz: Class[_]                      = implicitly[ClassTag[T]].runtimeClass
      def parser: BodyParser[Http.RequestBody] =
        HandlerInvokerFactory.javaBodyParserToScala(javaHandlerComponents.getBodyParser(annotations.parser))
      def invocation(req: Http.Request): CompletableFuture[Result] = CompletableFuture.completedFuture(inv(req))
      val annotations                                              =
        new JavaActionAnnotations(
          clazz,
          clazz.getMethod(method, classOf[Http.Request]),
          handlerComponents.httpConfiguration.actionComposition
        )
    }

  def withActionServer[T](config: (String, String)*)(block: Application => T): T = {
    val app = GuiceApplicationBuilder()
      .configure(Map(config: _*) ++ Map("play.http.secret.key" -> "ad31779d4ee49d5ad5162bf1429c32e2e9933f3b"))
      .appRoutes(implicit app => { case _ => javaAction[JavaCSPActionSpec.MyAction]("index", myAction.index) })
      .build()
    block(app)
  }

  "CSP filter support" should {
    "work when enabled" in withActionServer("play.filters.csp.nonce.header" -> "true") { implicit app =>
      val request      = FakeRequest()
      val Some(result) = route(app, request)

      val Some(nonce) = header(HeaderNames.X_CONTENT_SECURITY_POLICY_NONCE_HEADER, result)
      val expected    = s"script-src 'nonce-$nonce' 'unsafe-inline' 'unsafe-eval' 'strict-dynamic' https: http:"
      header(HeaderNames.CONTENT_SECURITY_POLICY, result).get must contain(expected)
    }
  }
}

object JavaCSPActionSpec {
  class MyAction extends Controller {
    @CSP
    def index(req: Http.Request): Result = {
      require(req.asScala() != null) // Make sure request is set
      Results.ok("")
    }
  }
}
