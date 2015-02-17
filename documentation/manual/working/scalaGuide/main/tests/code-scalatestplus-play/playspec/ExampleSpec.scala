/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.tests.scalatest.playspec

import play.api.test._
import org.scalatest._
import org.scalatestplus.play._
import play.api.test.Helpers._
import play.api.libs.ws._
import play.api.mvc._
import Results._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.concurrent.IntegrationPatience

// #scalafunctionaltest-playspec
class ExampleSpec extends PlaySpec with OneServerPerSuite with  ScalaFutures with IntegrationPatience {

  // Override app if you need a FakeApplication with other than
  // default parameters.
  implicit override lazy val app: FakeApplication =
    FakeApplication(
      additionalConfiguration = Map("ehcacheplugin" -> "disabled"),
      withRoutes = {
        case ("GET", "/testing") =>
          Action(
            Results.Ok(
              "<html>" +
                "<head><title>Test Page</title></head>" +
                "<body>" +
                "<input type='button' name='b' value='Click Me' onclick='document.title=\"scalatest\"' />" +
                "</body>" +
                "</html>"
            ).as("text/html")
          )
      }
    )

  "WsScalaTestClient's" must {

    "wsUrl works correctly" in {
      val futureResult = wsUrl("/testing").get
      val body = futureResult.futureValue.body
      val expectedBody =
        "<html>" +
          "<head><title>Test Page</title></head>" +
          "<body>" +
          "<input type='button' name='b' value='Click Me' onclick='document.title=\"scalatest\"' />" +
          "</body>" +
          "</html>"
      assert(body == expectedBody)
    }

    "wsCall works correctly" in {
      val futureResult = wsCall(Call("get", "/testing")).get
      val body = futureResult.futureValue.body
      val expectedBody =
        "<html>" +
          "<head><title>Test Page</title></head>" +
          "<body>" +
          "<input type='button' name='b' value='Click Me' onclick='document.title=\"scalatest\"' />" +
          "</body>" +
          "</html>"
      assert(body == expectedBody)
    }
  }
}
// #scalafunctionaltest-playspec
