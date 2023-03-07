/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.mvc

import java.util.concurrent.CompletionStage
import java.util.function.{ Function => JFunction }

import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.{ global => ec }

import akka.stream.Materializer
import akka.util.ByteString
import org.specs2.mutable.Specification
import play.api._
import play.api.http.DefaultHttpErrorHandler
import play.api.http.HttpErrorHandler
import play.api.libs.streams.Accumulator
import play.api.libs.ws._
import play.api.mvc._
import play.api.routing.Router
import play.api.test._
import play.core.server.Server
import play.filters.HttpFiltersComponents
import play.it._
import play.libs.streams

class NettyDefaultFiltersSpec    extends DefaultFiltersSpec with NettyIntegrationSpecification
class AkkaDefaultHttpFiltersSpec extends DefaultFiltersSpec with AkkaHttpIntegrationSpecification

trait DefaultFiltersSpec extends FiltersSpec {
  // Easy to use `withServer` method
  def withServer[T](settings: Map[String, String] = Map.empty, errorHandler: Option[HttpErrorHandler] = None)(
      filters: EssentialFilter*
  )(block: WSClient => T) = {
    withFlexibleServer(settings, errorHandler, (_: Materializer) => filters)(block)
  }

  // `withServer` method that allows filters to be constructed with a Materializer
  def withFlexibleServer[T](
      settings: Map[String, String],
      errorHandler: Option[HttpErrorHandler],
      makeFilters: Materializer => Seq[EssentialFilter]
  )(block: WSClient => T) = {
    val app = new BuiltInComponentsFromContext(
      ApplicationLoader.Context.create(
        environment = Environment.simple(),
        initialSettings = settings
      )
    ) with HttpFiltersComponents {
      lazy val router                                     = testRouter(this)
      override lazy val httpFilters: Seq[EssentialFilter] = makeFilters(materializer)
      override lazy val httpErrorHandler = errorHandler.getOrElse(
        new DefaultHttpErrorHandler(environment, configuration, devContext.map(_.sourceMapper), Some(router))
      )
    }.application

    Server.withApplication(app) { implicit port => WsTestClient.withClient(block) }
  }

  // Only run this test for injected filters; we can't use it for GlobalSettings
  // filters because we can't get the Materializer that we need
  "Java filters" should {
    "work with a simple nop filter" in withFlexibleServer(
      Map.empty,
      None,
      (mat: Materializer) => Seq(new JavaSimpleFilter(mat))
    ) { ws =>
      val response = Await.result(ws.url("/ok").get(), Duration.Inf)
      response.status must_== 200
      response.body[String] must_== expectedOkText
    }
  }

  // A Java filter that extends Filter, not EssentialFilter
  class JavaSimpleFilter(mat: Materializer) extends play.mvc.Filter(mat) {
    println("Creating JavaSimpleFilter")
    import play.mvc._

    override def apply(
        next: JFunction[Http.RequestHeader, CompletionStage[Result]],
        rh: Http.RequestHeader
    ): CompletionStage[Result] = {
      println("Calling JavaSimpleFilter.apply")
      next(rh)
    }
  }
}

trait FiltersSpec extends Specification with ServerIntegrationSpecification {
  sequential

  "filters" should {
    "handle errors" in {
      "ErrorHandlingFilter has no effect on a GET that returns a 200 OK" in withServer()(ErrorHandlingFilter) { ws =>
        val response = Await.result(ws.url("/ok").get(), Duration.Inf)
        response.status must_== 200
        response.body[String] must_== expectedOkText
      }

      "ErrorHandlingFilter has no effect on a POST that returns a 200 OK" in withServer()(ErrorHandlingFilter) { ws =>
        val response = Await.result(ws.url("/ok").post(expectedOkText), Duration.Inf)
        response.status must_== 200
        response.body[String] must_== expectedOkText
      }

      "ErrorHandlingFilter recovers from a GET that throws a synchronous exception" in withServer()(
        ErrorHandlingFilter
      ) { ws =>
        val response = Await.result(ws.url("/error").get(), Duration.Inf)
        response.status must_== 500
        response.body[String] must_== expectedErrorText
      }

      "ErrorHandlingFilter recovers from a GET that throws an asynchronous exception" in withServer()(
        ErrorHandlingFilter
      ) { ws =>
        val response = Await.result(ws.url("/error-async").get(), Duration.Inf)
        response.status must_== 500
        response.body[String] must_== expectedErrorText
      }

      "ErrorHandlingFilter recovers from a POST that throws a synchronous exception" in withServer()(
        ErrorHandlingFilter
      ) { ws =>
        val response = Await.result(ws.url("/error").post(expectedOkText), Duration.Inf)
        response.status must_== 500
        response.body[String] must_== expectedOkText
      }

      "ErrorHandlingFilter recovers from a POST that throws an asynchronous exception" in withServer()(
        ErrorHandlingFilter
      ) { ws =>
        val response = Await.result(ws.url("/error-async").post(expectedOkText), Duration.Inf)
        response.status must_== 500
        response.body[String] must_== expectedOkText
      }
    }

    "handle errors in Java" in {
      "ErrorHandlingFilter has no effect on a GET that returns a 200 OK" in withServer()(JavaErrorHandlingFilter) {
        ws =>
          val response = Await.result(ws.url("/ok").get(), Duration.Inf)
          response.status must_== 200
          response.body[String] must_== expectedOkText
      }

      "ErrorHandlingFilter has no effect on a POST that returns a 200 OK" in withServer()(JavaErrorHandlingFilter) {
        ws =>
          val response = Await.result(ws.url("/ok").post(expectedOkText), Duration.Inf)
          response.status must_== 200
          response.body[String] must_== expectedOkText
      }

      "ErrorHandlingFilter recovers from a GET that throws a synchronous exception" in withServer()(
        JavaErrorHandlingFilter
      ) { ws =>
        val response = Await.result(ws.url("/error").get(), Duration.Inf)
        response.status must_== 500
        response.body[String] must_== expectedErrorText
      }

      "ErrorHandlingFilter recovers from a GET that throws an asynchronous exception" in withServer()(
        JavaErrorHandlingFilter
      ) { ws =>
        val response = Await.result(ws.url("/error-async").get(), Duration.Inf)
        response.status must_== 500
        response.body[String] must_== expectedErrorText
      }

      "ErrorHandlingFilter recovers from a POST that throws a synchronous exception" in withServer()(
        JavaErrorHandlingFilter
      ) { ws =>
        val response = Await.result(ws.url("/error").post(expectedOkText), Duration.Inf)
        response.status must_== 500
        response.body[String] must_== expectedOkText
      }

      "ErrorHandlingFilter recovers from a POST that throws an asynchronous exception" in withServer()(
        JavaErrorHandlingFilter
      ) { ws =>
        val response = Await.result(ws.url("/error-async").post(expectedOkText), Duration.Inf)
        response.status must_== 500
        response.body[String] must_== expectedOkText
      }
    }

    "Filters are not applied when the request is outside play.http.context" in withServer(
      Map("play.http.context" -> "/foo")
    )(ErrorHandlingFilter, ThrowExceptionFilter) { ws =>
      val response = Await.result(ws.url("/ok").post(expectedOkText), Duration.Inf)
      response.status must_== 200
      response.body[String] must_== expectedOkText
    }

    "Filters are applied on the root of the application context" in withServer(Map("play.http.context" -> "/foo"))(
      SkipNextFilter
    ) { ws =>
      val response = Await.result(ws.url("/foo").post(expectedOkText), Duration.Inf)
      response.status must_== 200
      response.body[String] must_== SkipNextFilter.expectedText
    }

    "Filters work even if one of them does not call next" in withServer()(ErrorHandlingFilter, SkipNextFilter) { ws =>
      val response = Await.result(ws.url("/ok").get(), Duration.Inf)
      response.status must_== 200
      response.body[String] must_== SkipNextFilter.expectedText
    }

    "ErrorHandlingFilter can recover from an exception throw by another filter in the filter chain, even if that Filter does not call next" in withServer()(
      ErrorHandlingFilter,
      SkipNextWithErrorFilter
    ) { ws =>
      val response = Await.result(ws.url("/ok").get(), Duration.Inf)
      response.status must_== 500
      response.body[String] must_== SkipNextWithErrorFilter.expectedText
    }

    "ErrorHandlingFilter can recover from an exception throw by another filter in the filter chain when that filter calls next and asynchronously throws an exception" in withServer()(
      ErrorHandlingFilter,
      ThrowExceptionFilter
    ) { ws =>
      val response = Await.result(ws.url("/ok").get(), Duration.Inf)
      response.status must_== 500
      response.body[String] must_== ThrowExceptionFilter.expectedText
    }

    object ThreadNameFilter extends EssentialFilter {
      def apply(next: EssentialAction): EssentialAction = EssentialAction { req =>
        Accumulator.done(Results.Ok(Thread.currentThread().getName))
      }
    }

    "Filters should use the Akka ExecutionContext" in withServer()(ThreadNameFilter) { ws =>
      val result     = Await.result(ws.url("/ok").get(), Duration.Inf)
      val threadName = result.body
      threadName must startWith("application-akka.actor.default-dispatcher-")
    }

    "Scala EssentialFilter should work when converting from Scala to Java" in withServer()(
      ScalaEssentialFilter.asJava
    ) { ws =>
      val result = Await.result(ws.url("/ok").get(), Duration.Inf)
      result.header(ScalaEssentialFilter.header) must beSome(ScalaEssentialFilter.expectedValue)
    }

    "Java EssentialFilter should work when converting from Java to Scala" in withServer()(JavaEssentialFilter.asScala) {
      ws =>
        val result = Await.result(ws.url("/ok").get(), Duration.Inf)
        result.header(JavaEssentialFilter.header) must beSome(JavaEssentialFilter.expectedValue)
    }

    "Scala EssentialFilter should preserve the same type when converting from Scala to Java then back to Scala" in {
      ScalaEssentialFilter.asJava.asScala.getClass.isAssignableFrom(ScalaEssentialFilter.getClass) must_== true
    }

    "Java EssentialFilter should preserve the same type when converting from Java to Scala then back Java" in {
      JavaEssentialFilter.asScala.asJava.getClass.isAssignableFrom(JavaEssentialFilter.getClass) must_== true
    }

    val filterAddedHeaderKey = "CUSTOM_HEADER"
    val filterAddedHeaderVal = "custom header val"

    object CustomHeaderFilter extends EssentialFilter {
      def apply(next: EssentialAction) = EssentialAction { request =>
        next(request.withHeaders(addCustomHeader(request.headers)))
      }
      def addCustomHeader(originalHeaders: Headers): Headers = {
        FakeHeaders(originalHeaders.headers :+ (filterAddedHeaderKey -> filterAddedHeaderVal))
      }
    }

    object CustomErrorHandler extends HttpErrorHandler {
      def onClientError(request: RequestHeader, statusCode: Int, message: String) = {
        Future.successful(Results.NotFound(request.headers.get(filterAddedHeaderKey).getOrElse("undefined header")))
      }
      def onServerError(request: RequestHeader, exception: Throwable) = Future.successful(Results.InternalServerError)
    }

    "requests not matching a route should receive a RequestHeader modified by upstream filters" in withServer(
      errorHandler = Some(CustomErrorHandler)
    )(CustomHeaderFilter) { ws =>
      val response = Await.result(ws.url("/not-a-real-route").get(), Duration.Inf)
      response.status must_== 404
      response.body[String] must_== filterAddedHeaderVal
    }
  }

  object ErrorHandlingFilter extends EssentialFilter {
    def apply(next: EssentialAction) = EssentialAction { request =>
      try {
        next(request).recover {
          case t: Throwable =>
            Results.InternalServerError(t.getMessage)
        }(ec)
      } catch {
        case t: Throwable => Accumulator.done(Results.InternalServerError(t.getMessage))
      }
    }
  }

  object JavaErrorHandlingFilter extends play.mvc.EssentialFilter {
    import play.libs.streams.Accumulator
    import play.mvc._

    private def getResult(t: Throwable): Result = {
      // Get the cause of the CompletionException
      Results.internalServerError(Option(t.getCause).getOrElse(t).getMessage)
    }

    def apply(next: EssentialAction): EssentialAction = new EssentialAction {
      override def apply(request: Http.RequestHeader): Accumulator[ByteString, Result] = {
        try {
          next
            .apply(request)
            .recover(t => getResult(t), ec)
        } catch {
          case t: Throwable => Accumulator.done(getResult(t))
        }
      }
    }
  }

  object SkipNextFilter extends EssentialFilter {
    val expectedText = "This filter does not call next"

    def apply(next: EssentialAction) = EssentialAction { request => Accumulator.done(Results.Ok(expectedText)) }
  }

  object SkipNextWithErrorFilter extends EssentialFilter {
    val expectedText = "This filter does not call next and throws an exception"

    def apply(next: EssentialAction) = EssentialAction { request =>
      Accumulator.done(Future.failed(new RuntimeException(expectedText)))
    }
  }

  object ThrowExceptionFilter extends EssentialFilter {
    val expectedText = "This filter calls next and throws an exception afterwards"

    def apply(next: EssentialAction) = EssentialAction { request =>
      next(request).map { _ => throw new RuntimeException(expectedText) }(ec)
    }
  }

  object ScalaEssentialFilter extends EssentialFilter {
    val header        = "Scala"
    val expectedValue = "1"

    def apply(next: EssentialAction) = EssentialAction { request =>
      next(request).map { result => result.withHeaders(header -> expectedValue) }(ec)
    }
  }

  object JavaEssentialFilter extends play.mvc.EssentialFilter {
    import play.mvc._
    val header        = "Java"
    val expectedValue = "1"

    override def apply(next: EssentialAction): EssentialAction = new EssentialAction {
      override def apply(request: Http.RequestHeader): streams.Accumulator[ByteString, Result] = {
        next
          .apply(request)
          .map(result => result.withHeader(header, expectedValue), ec)
      }
    }
  }

  val expectedOkText    = "Hello World"
  val expectedErrorText = "Error"

  import play.api.routing.sird._
  def testRouter(components: BuiltInComponents) = {
    val Action = components.defaultActionBuilder
    Router.from {
      case GET(p"/") =>
        Action { request => Results.Ok(expectedOkText) }
      case GET(p"/ok") =>
        Action { request => Results.Ok(expectedOkText) }
      case POST(p"/ok") =>
        Action { request => Results.Ok(request.body.asText.getOrElse("")) }
      case GET(p"/error") =>
        Action { request => throw new RuntimeException(expectedErrorText) }
      case POST(p"/error") =>
        Action { request => throw new RuntimeException(request.body.asText.getOrElse("")) }
      case GET(p"/error-async") =>
        Action.async { request => Future { throw new RuntimeException(expectedErrorText) }(ec) }
      case POST(p"/error-async") =>
        Action.async { request => Future { throw new RuntimeException(request.body.asText.getOrElse("")) }(ec) }
    }
  }

  def withServer[T](settings: Map[String, String] = Map.empty, errorHandler: Option[HttpErrorHandler] = None)(
      filters: EssentialFilter*
  )(block: WSClient => T): T
}
