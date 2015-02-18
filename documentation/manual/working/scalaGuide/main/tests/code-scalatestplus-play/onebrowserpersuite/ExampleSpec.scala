/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.tests.scalatest.onebrowserpersuite

import play.api.test._
import org.scalatest._
import org.scalatestplus.play._
import play.api.test.Helpers._
import play.api.libs.ws._
import play.api.mvc._
import Results._

// #scalafunctionaltest-onebrowserpersuite
class ExampleSpec extends PlaySpec with OneServerPerSuite with OneBrowserPerSuite with HtmlUnitFactory {

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

  "The OneBrowserPerTest trait" must {
    "provide a web driver" in {
      go to (s"http://localhost:$port/testing")
      pageTitle mustBe "Test Page"
      click on find(name("b")).value
      eventually { pageTitle mustBe "scalatest" }
    }
  }
}
// #scalafunctionaltest-onebrowserpersuite
