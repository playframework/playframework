/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.mvc

import org.specs2.mutable.Specification
import play.api.mvc._
import play.api.test._
import play.it._
import scala.concurrent.duration.Duration
import scala.concurrent._
import play.api.libs.concurrent.Execution.{ defaultContext => ec }

object NettyFiltersSpec extends FiltersSpec with NettyIntegrationSpecification
object AkkaHttpFiltersSpec extends FiltersSpec with AkkaHttpIntegrationSpecification

trait FiltersSpec extends PlaySpecification with WsTestClient with ServerIntegrationSpecification {

  "filters" should {
    "handle errors" in {

      object ErrorHandlingFilter extends Filter {
        def apply(next: RequestHeader => Future[Result])(request: RequestHeader): Future[Result] = {
          try {
            next(request).recover {
              case t: Throwable =>
                Results.InternalServerError(t.getMessage)
            }(play.api.libs.concurrent.Execution.Implicits.defaultContext)
          } catch {
            case t: Throwable => Future.successful(Results.InternalServerError(t.getMessage))
          }
        }
      }

      object SkipNextFilter extends Filter {
        val expectedText = "This filter does not call next"

        def apply(next: RequestHeader => Future[Result])(request: RequestHeader): Future[Result] = {
          Future.successful(Results.Ok(expectedText))
        }
      }

      object SkipNextWithErrorFilter extends Filter {
        val expectedText = "This filter does not call next and throws an exception"

        def apply(next: RequestHeader => Future[Result])(request: RequestHeader): Future[Result] = {
          Future.failed(new RuntimeException(expectedText))
        }
      }

      object ThrowExceptionFilter extends Filter {
        val expectedText = "This filter calls next and throws an exception afterwords"

        override def apply(next: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
          next(rh).map { _ =>
            throw new RuntimeException(expectedText)
          }(ec)
        }
      }

      object ErrorHandlingGlobal extends WithFilters(ErrorHandlingFilter)
      object SkipNextGlobal extends WithFilters(ErrorHandlingFilter, SkipNextFilter)
      object SkipNextWithErrorGlobal extends WithFilters(ErrorHandlingFilter, SkipNextWithErrorFilter)
      object GlobalWithThrowExceptionFilter extends WithFilters(ErrorHandlingFilter, ThrowExceptionFilter)

      val expectedOkText = "Hello World"
      val expectedErrorText = "Error"

      val routerForTest: PartialFunction[(String, String), Handler] = {
        case ("GET", "/ok") => Action { request => Results.Ok(expectedOkText) }
        case ("POST", "/ok") => Action { request => Results.Ok(request.body.asText.getOrElse("")) }
        case ("GET", "/error") => Action { request => throw new RuntimeException(expectedErrorText) }
        case ("POST", "/error") => Action { request => throw new RuntimeException(request.body.asText.getOrElse("")) }
        case ("GET", "/error-async") => Action.async { request => Future { throw new RuntimeException(expectedErrorText) }(ec) }
        case ("POST", "/error-async") => Action.async { request => Future { throw new RuntimeException(request.body.asText.getOrElse("")) }(ec) }
      }

      "ErrorHandlingFilter has no effect on a GET that returns a 200 OK" in new WithServer(FakeApplication(withGlobal = Some(ErrorHandlingGlobal), withRoutes = routerForTest)) {
        val response = Await.result(wsUrl("/ok").get(), Duration.Inf)
        response.status must_== 200
        response.body must_== expectedOkText
      }

      "ErrorHandlingFilter has no effect on a POST that returns a 200 OK" in new WithServer(FakeApplication(withGlobal = Some(ErrorHandlingGlobal), withRoutes = routerForTest)) {
        val response = Await.result(wsUrl("/ok").post(expectedOkText), Duration.Inf)
        response.status must_== 200
        response.body must_== expectedOkText
      }

      "ErrorHandlingFilter recovers from a GET that throws a synchronous exception" in new WithServer(FakeApplication(withGlobal = Some(ErrorHandlingGlobal), withRoutes = routerForTest)) {
        val response = Await.result(wsUrl("/error").get(), Duration.Inf)
        response.status must_== 500
        response.body must_== expectedErrorText
      }

      "ErrorHandlingFilter recovers from a GET that throws an asynchronous exception" in new WithServer(FakeApplication(withGlobal = Some(ErrorHandlingGlobal), withRoutes = routerForTest)) {
        val response = Await.result(wsUrl("/error-async").get(), Duration.Inf)
        response.status must_== 500
        response.body must_== expectedErrorText
      }

      "ErrorHandlingFilter recovers from a POST that throws a synchronous exception" in new WithServer(FakeApplication(withGlobal = Some(ErrorHandlingGlobal), withRoutes = routerForTest)) {
        val response = Await.result(wsUrl("/error").post(expectedOkText), Duration.Inf)
        response.status must_== 500
        response.body must_== expectedOkText
      }

      "ErrorHandlingFilter recovers from a POST that throws an asynchronous exception" in new WithServer(FakeApplication(withGlobal = Some(ErrorHandlingGlobal), withRoutes = routerForTest)) {
        val response = Await.result(wsUrl("/error-async").post(expectedOkText), Duration.Inf)
        response.status must_== 500
        response.body must_== expectedOkText
      }

      "Filters are not applied when the request is outside the application.context" in new WithServer(
        FakeApplication(
          withGlobal = Some(GlobalWithThrowExceptionFilter),
          withRoutes = routerForTest,
          additionalConfiguration = Map("application.context" -> "/foo"))
      ) {
        val response = Await.result(wsUrl("/ok").post(expectedOkText), Duration.Inf)
        response.status must_== 200
        response.body must_== expectedOkText
      }

      "Filters work even if one of them does not call next" in new WithServer(FakeApplication(withGlobal = Some(SkipNextGlobal), withRoutes = routerForTest)) {
        val response = Await.result(wsUrl("/ok").get(), Duration.Inf)
        response.status must_== 200
        response.body must_== SkipNextFilter.expectedText
      }

      "ErrorHandlingFilter can recover from an exception throw by another filter in the filter chain, even if that Filter does not call next" in new WithServer(FakeApplication(withGlobal = Some(SkipNextWithErrorGlobal), withRoutes = routerForTest)) {
        val response = Await.result(wsUrl("/ok").get(), Duration.Inf)
        response.status must_== 500
        response.body must_== SkipNextWithErrorFilter.expectedText
      }

      "ErrorHandlingFilter can recover from an exception throw by another filter in the filter chain when that filter calls next and asynchronously throws an exception" in new WithServer(FakeApplication(withGlobal = Some(GlobalWithThrowExceptionFilter), withRoutes = routerForTest)) {
        val response = Await.result(wsUrl("/ok").get(), Duration.Inf)
        response.status must_== 500
        response.body must_== ThrowExceptionFilter.expectedText
      }

      val filterAddedHeaderKey = "CUSTOM_HEADER"
      val filterAddedHeaderVal = "custom header val"

      object MockGlobal3 extends WithFilters(new Filter {
        def apply(next: RequestHeader => Future[Result])(request: RequestHeader): Future[Result] = {
          next(request.copy(headers = addCustomHeader(request.headers)))
        }
        def addCustomHeader(originalHeaders: Headers): Headers = {
          FakeHeaders(originalHeaders.headers :+ (filterAddedHeaderKey -> filterAddedHeaderVal))
        }
      }) {
        override def onHandlerNotFound(request: RequestHeader) = {
          Future.successful(Results.NotFound(request.headers.get(filterAddedHeaderKey).getOrElse("undefined header")))
        }
      }

      "requests not matching a route should receive a RequestHeader modified by upstream filters" in new WithServer(FakeApplication(withGlobal = Some(MockGlobal3))) {
        val response = Await.result(wsUrl("/not-a-real-route").get(), Duration.Inf)
        response.status must_== 404
        response.body must_== filterAddedHeaderVal
      }
    }
  }
}
