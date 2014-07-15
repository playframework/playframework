/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.test

import play.api.mvc._

object FakesSpec extends PlaySpecification {

  sequential

  "FakeApplication" should {

    "allow adding routes inline" in {
      val app = new FakeApplication(
        withRoutes = {
          case ("GET", "/inline") => Action { Results.Ok("inline route") }
        }
      )
      running(app) {
        val result = route(app, FakeRequest("GET", "/inline"))
        result must beSome
        contentAsString(result.get) must_== "inline route"
        route(app, FakeRequest("GET", "/foo")) must beNone
      }
    }
  }

  "FakeRequest" should {
    val app = new FakeApplication(
      withRoutes = {
        case (PUT, "/process") => Action { req =>
          Results.Ok(req.headers.get(CONTENT_TYPE) getOrElse "")
        }
      }
    )

    "Define Content-Type header based on body" in new WithApplication(app) {
      val xml =
        <foo>
          <bar>
            baz
          </bar>
        </foo>
      val bytes = xml.toString.getBytes("utf-16le")
      val req = FakeRequest(PUT, "/process")
        .withRawBody(bytes)
      route(req) aka "response" must beSome.which { resp =>
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
      val bytes = xml.toString.getBytes("utf-16le")
      val req = FakeRequest(PUT, "/process")
        .withRawBody(bytes)
        .withHeaders(
          CONTENT_TYPE -> "text/xml;charset=utf-16le"
        )
      route(req) aka "response" must beSome.which { resp =>
        contentAsString(resp) aka "content" must_== "text/xml;charset=utf-16le"
      }
    }
  }

}
