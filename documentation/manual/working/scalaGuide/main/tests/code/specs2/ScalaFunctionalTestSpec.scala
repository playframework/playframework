/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.tests.specs2

import play.api.libs.ws._

import play.api.mvc._
import play.api.test._

import play.api.{GlobalSettings, Application}
import play.api.test.Helpers._
import scala.Some
import play.api.test.FakeApplication

/**
 *
 */
class ScalaFunctionalTestSpec extends PlaySpecification with Results {

  // lie and make this look like a DB model.
  case class Computer(name: String, introduced: Option[String])

  object Computer {
    def findById(id: Int): Option[Computer] = Some(Computer("Macintosh", Some("1984-01-24")))
  }

  "Scala Functional Test" should {

    // #scalafunctionaltest-fakeApplication
    val fakeApplicationWithGlobal = FakeApplication(withGlobal = Some(new GlobalSettings() {
      override def onStart(app: Application) { println("Hello world!") }
    }))
    // #scalafunctionaltest-fakeApplication

    val fakeApplication = FakeApplication(withRoutes = {
      case ("GET", "/Bob") =>
        Action {
          Ok("Hello Bob") as "text/html; charset=utf-8"
        }
    })

    // #scalafunctionaltest-respondtoroute
    "respond to the index Action" in new WithApplication(fakeApplication) {
      val Some(result) = route(FakeRequest(GET, "/Bob"))

      status(result) must equalTo(OK)
      contentType(result) must beSome("text/html")
      charset(result) must beSome("utf-8")
      contentAsString(result) must contain("Hello Bob")
    }
    // #scalafunctionaltest-respondtoroute

    // #scalafunctionaltest-testview
    "render index template" in new WithApplication {
      val html = views.html.index("Coco")

      contentAsString(html) must contain("Hello Coco")
    }
    // #scalafunctionaltest-testview

    // #scalafunctionaltest-testmodel
    val appWithMemoryDatabase = FakeApplication(additionalConfiguration = inMemoryDatabase("test"))
    "run an application" in new WithApplication(appWithMemoryDatabase) {

      val Some(macintosh) = Computer.findById(21)

      macintosh.name must equalTo("Macintosh")
      macintosh.introduced must beSome.which(_ must beEqualTo("1984-01-24"))
    }
    // #scalafunctionaltest-testmodel

    // #scalafunctionaltest-testwithbrowser
    val fakeApplicationWithBrowser = FakeApplication(withRoutes = {
      case ("GET", "/") =>
        Action {
          Ok(
            """
              |<html>
              |<body>
              |  <div id="title">Hello Guest</div>
              |  <a href="/login">click me</a>
              |</body>
              |</html>
            """.stripMargin) as "text/html"
        }
      case ("GET", "/login") =>
        Action {
          Ok(
            """
              |<html>
              |<body>
              |  <div id="title">Hello Coco</div>
              |</body>
              |</html>
            """.stripMargin) as "text/html"
        }
    })

    "run in a browser" in new WithBrowser(webDriver = WebDriverFactory(HTMLUNIT), app = fakeApplicationWithBrowser) {
      browser.goTo("/")

      // Check the page
      browser.$("#title").getTexts().get(0) must equalTo("Hello Guest")

      browser.$("a").click()

      browser.url must equalTo("/login")
      browser.$("#title").getTexts().get(0) must equalTo("Hello Coco")
    }
    // #scalafunctionaltest-testwithbrowser

    val testPort = 19001
    val myPublicAddress =  s"localhost:$testPort"
    val testPaymentGatewayURL = s"http://$myPublicAddress"
    // #scalafunctionaltest-testpaymentgateway
    "test server logic" in new WithServer(app = fakeApplicationWithBrowser, port = testPort) {
      // The test payment gateway requires a callback to this server before it returns a result...
      val callbackURL = s"http://$myPublicAddress/callback"

      // await is from play.api.test.FutureAwaits
      val response = await(WS.url(testPaymentGatewayURL).withQueryString("callbackURL" -> callbackURL).get())

      response.status must equalTo(OK)
    }
    // #scalafunctionaltest-testpaymentgateway

    // #scalafunctionaltest-testws
    val appWithRoutes = FakeApplication(withRoutes = {
      case ("GET", "/") =>
        Action {
          Ok("ok")
        }
    })

    "test WS logic" in new WithServer(app = appWithRoutes, port = 3333) {
      await(WS.url("http://localhost:3333").get()).status must equalTo(OK)
    }
    // #scalafunctionaltest-testws

  }

}
