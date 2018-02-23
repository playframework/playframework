/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.test

import java.util.concurrent.TimeUnit

import akka.stream.Materializer
import akka.util.ByteString
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class FakesSpec extends PlaySpecification {

  sequential

  private val Action = ActionBuilder.ignoringBody

  "FakeRequest" should {
    def app = GuiceApplicationBuilder().routes {
      case (PUT, "/process") => Action { req =>
        Results.Ok(req.headers.get(CONTENT_TYPE) getOrElse "")
      }
    }.build()

    "Define Content-Type header based on body" in new WithApplication(app) {
      val xml =
        <foo>
          <bar>
            baz
          </bar>
        </foo>
      val bytes = ByteString(xml.toString, "utf-16le")
      val req = FakeRequest(PUT, "/process")
        .withRawBody(bytes)
      route(app, req) aka "response" must beSome.which { resp =>
        contentAsString(resp) aka "content" must_== "application/octet-stream"
      }
    }

    "Not override explicit Content-Type header" in new WithApplication(app) {
      val xml =
        <foo>
          <bar>
            baz
          </bar>
        </foo>
      val bytes = ByteString(xml.toString, "utf-16le")
      val req = FakeRequest(PUT, "/process")
        .withRawBody(bytes)
        .withHeaders(
          CONTENT_TYPE -> "text/xml;charset=utf-16le"
        )
      route(app, req) aka "response" must beSome.which { resp =>
        contentAsString(resp) aka "content" must_== "text/xml;charset=utf-16le"
      }
    }

    "set a Content-Type header when one is unspecified and required" in new WithApplication() {
      val request = FakeRequest(GET, "/testCall")
        .withJsonBody(Json.obj("foo" -> "bar"))

      contentTypeForFakeRequest(request) must contain("application/json")
    }
    "not overwrite the Content-Type header when specified" in new WithApplication() {
      val request = FakeRequest(GET, "/testCall")
        .withJsonBody(Json.obj("foo" -> "bar"))
        .withHeaders(CONTENT_TYPE -> "application/test+json")

      contentTypeForFakeRequest(request) must contain("application/test+json")
    }
  }

  def contentTypeForFakeRequest[T](request: FakeRequest[AnyContentAsJson])(implicit mat: Materializer): String = {
    var testContentType: Option[String] = None
    val action = Action { request: Request[_] => testContentType = request.headers.get(CONTENT_TYPE); Ok }
    val headers = new WrappedRequest(request)
    val execution = (new TestActionCaller).call(action, headers, request.body)
    Await.result(execution, Duration(3, TimeUnit.SECONDS))
    testContentType.getOrElse("No Content-Type found")
  }

}

class TestActionCaller extends EssentialActionCaller with Writeables

