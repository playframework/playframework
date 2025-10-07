/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.tests.specs2

import org.specs2.mutable.Specification
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws._
import play.api.mvc._
// #scalafunctionaltest-imports
import play.api.test._
// ###replace: import play.api.test.Helpers._
import play.api.test.Helpers.{ GET => GET_REQUEST, _ }
// #scalafunctionaltest-imports
import play.api.Application

trait ExampleSpecification extends Specification with DefaultAwaitTimeout with FutureAwaits with Results

class ScalaFunctionalTestSpec extends ExampleSpecification {
  // lie and make this look like a DB model.
  case class Computer(name: String, introduced: Option[String])

  object Computer {
    def findById(id: Int): Option[Computer] = Some(Computer("Macintosh", Some("1984-01-24")))
  }

  "Scala Functional Test" should {
    // #scalafunctionaltest-application
    val application: Application = GuiceApplicationBuilder().build()
    // #scalafunctionaltest-application

    val applicationWithRouter = GuiceApplicationBuilder()
      .appRoutes { app =>
        val Action = app.injector.instanceOf[DefaultActionBuilder]
        ({
          case ("GET", "/Bob") =>
            Action {
              Ok("Hello Bob").as("text/html; charset=utf-8")
            }
        })
      }
      .build()

    // #scalafunctionaltest-respondtoroute
    "respond to the index Action" in new WithApplication(applicationWithRouter) {
      override def running() = {
        // ###replace: val Some(result) = route(app, FakeRequest(GET, "/Bob"))
        val Some(result) = route(app, FakeRequest(GET_REQUEST, "/Bob"))

        status(result) must equalTo(OK)
        contentType(result) must beSome("text/html")
        charset(result) must beSome("utf-8")
        contentAsString(result) must contain("Hello Bob")
      }
    }
    // #scalafunctionaltest-respondtoroute

    // #scalafunctionaltest-testview
    "render index template" in new WithApplication {
      override def running() = {
        val html = views.html.index("Coco")

        contentAsString(html) must contain("Hello Coco")
      }
    }
    // #scalafunctionaltest-testview

    // #scalafunctionaltest-testmodel
    def appWithMemoryDatabase = new GuiceApplicationBuilder().configure(inMemoryDatabase("test")).build()
    "run an application" in new WithApplication(appWithMemoryDatabase) {
      override def running() = {
        val Some(macintosh) = Computer.findById(21)

        macintosh.name must equalTo("Macintosh")
        macintosh.introduced must beSome[String].which(_ must beEqualTo("1984-01-24"))
      }
    }
    // #scalafunctionaltest-testmodel

    // #scalafunctionaltest-testwithbrowser
    def applicationWithBrowser = {
      new GuiceApplicationBuilder()
        .appRoutes { app =>
          val Action = app.injector.instanceOf[DefaultActionBuilder]
          ({
            case ("GET", "/") =>
              Action {
                Ok("""
                     |<html>
                     |<body>
                     |  <div id="title">Hello Guest</div>
                     |  <a href="/login">click me</a>
                     |</body>
                     |</html>
                """.stripMargin).as("text/html")
              }
            case ("GET", "/login") =>
              Action {
                Ok("""
                     |<html>
                     |<body>
                     |  <div id="title">Hello Coco</div>
                     |</body>
                     |</html>
                """.stripMargin).as("text/html")
              }
          })
        }
        .build()
    }

    "run in a browser" in new WithBrowser(webDriver = WebDriverFactory(HTMLUNIT), app = applicationWithBrowser) {
      override def running() = {
        browser.goTo("/")

        // Check the page
        browser.el("#title").text() must equalTo("Hello Guest")

        browser.el("a").click()

        browser.url must equalTo("login")
        browser.el("#title").text() must equalTo("Hello Coco")
      }
    }
    // #scalafunctionaltest-testwithbrowser

    val testPort              = 19001
    val myPublicAddress       = s"localhost:$testPort"
    val testPaymentGatewayURL = s"http://$myPublicAddress"
    // #scalafunctionaltest-testpaymentgateway
    "test server logic" in new WithServer(app = applicationWithBrowser, port = testPort) {
      override def running() = {
        // The test payment gateway requires a callback to this server before it returns a result...
        val callbackURL = s"http://$myPublicAddress/callback"

        val ws = app.injector.instanceOf[WSClient]

        // await is from play.api.test.FutureAwaits
        val response =
          await(ws.url(testPaymentGatewayURL).withQueryStringParameters("callbackURL" -> callbackURL).get())

        response.status must equalTo(OK)
      }
    }
    // #scalafunctionaltest-testpaymentgateway

    // #scalafunctionaltest-testws
    val appWithRoutes = GuiceApplicationBuilder()
      .appRoutes { app =>
        val Action = app.injector.instanceOf[DefaultActionBuilder]
        ({
          case ("GET", "/") =>
            Action {
              Ok("ok")
            }
        })
      }
      .build()

    "test WSClient logic" in new WithServer(app = appWithRoutes, port = 3333) {
      override def running() = {
        val ws = app.injector.instanceOf[WSClient]
        await(ws.url("http://localhost:3333").get()).status must equalTo(OK)
      }
    }
    // #scalafunctionaltest-testws

    // #scalafunctionaltest-testmessages
    "messages" should {
      import play.api.i18n._

      implicit val lang = Lang("en-US")

      "provide default messages with the Java API" in new WithApplication() with Injecting {
        override def running() = {
          val javaMessagesApi = inject[play.i18n.MessagesApi]
          val msg             = javaMessagesApi.get(new play.i18n.Lang(lang), "constraint.email")
          msg must ===("Email")
        }
      }

      "provide default messages with the Scala API" in new WithApplication() with Injecting {
        override def running() = {
          val messagesApi = inject[MessagesApi]
          val msg         = messagesApi("constraint.email")
          msg must ===("Email")
        }
      }
    }
    // #scalafunctionaltest-testmessages
  }
}
