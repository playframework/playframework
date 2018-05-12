/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.csp

import com.typesafe.config.ConfigFactory
import javax.inject.Inject
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.{ AbstractController, ControllerComponents }
import play.api.test.{ FakeRequest, PlaySpecification }
import play.api.{ Application, Configuration }

import scala.reflect.ClassTag

class ScalaCSPReportSpec extends PlaySpecification {

  sequential

  def toConfiguration(rawConfig: String) = {
    val typesafeConfig = ConfigFactory.parseString(rawConfig)
    Configuration(typesafeConfig)
  }

  private def inject[T: ClassTag](implicit app: Application) = app.injector.instanceOf[T]

  private def myAction(implicit app: Application) = inject[ScalaCSPReportSpec.MyAction]

  def withApplication[T]()(block: Application => T): T = {
    val app = new GuiceApplicationBuilder()
      .appRoutes(implicit app => { case _ => myAction.cspReport })
      .build()
    running(app)(block(app))
  }

  "Scala CSP report" should {

    "work with a chrome style csp-report" in withApplication() { implicit app =>
      val chromeJson = Json.parse(
        """{
          | "csp-report": {
          |    "document-uri": "http://45.55.25.245:8123/csp?os=OS%20X&device=&browser_version=43.0&browser=chrome&os_version=Lion",
          |    "referrer": "",
          |    "violated-directive": "child-src https://45.55.25.245:8123/",
          |    "effective-directive": "frame-src",
          |    "original-policy": "default-src  https://45.55.25.245:8123/; child-src  https://45.55.25.245:8123/; connect-src  https://45.55.25.245:8123/; font-src  https://45.55.25.245:8123/; img-src  https://45.55.25.245:8123/; media-src  https://45.55.25.245:8123/; object-src  https://45.55.25.245:8123/; script-src  https://45.55.25.245:8123/; style-src  https://45.55.25.245:8123/; form-action  https://45.55.25.245:8123/; frame-ancestors 'none'; plugin-types 'none'; report-uri http://45.55.25.245:8123/csp-report?os=OS%20X&device=&browser_version=43.0&browser=chrome&os_version=Lion",
          |    "blocked-uri": "http://google.com",
          |    "status-code": 200
          |  }
          |}
        """.stripMargin)

      val request = FakeRequest("POST", "/report-to").withJsonBody(chromeJson)
      val Some(result) = route(app, request)

      contentAsJson(result) must be_==(Json.obj("violation" -> "child-src https://45.55.25.245:8123/"))
    }

    "work with a firefox style csp-report" in withApplication() { implicit app =>
      val firefoxJson = Json.parse(
        """{
          |"csp-report": {
          |    "blocked-uri": "data:image/gif;base64,R0lGODlhEAAQAMQAAORHHOVSKudfOulrSOp3WOyDZu6QdvCchPGolfO0o/XBs/fNwfjZ0frl3/zy7////wAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACH5BAkAABAALAAAAAAQABAAAAVVICSOZGlCQAosJ6mu7fiyZeKqNKToQGDsM8hBADgUXoGAiqhSvp5QAnQKGIgUhwFUYLCVDFCrKUE1lBavAViFIDlTImbKC5Gm2hB0SlBCBMQiB0UjIQA7",
          |    "document-uri": "http://45.55.25.245:8123/csp?os=OS%20X&device=&browser_version=37.0&browser=firefox&os_version=Yosemite",
          |    "original-policy": "default-src https://45.55.25.245:8123/; connect-src https://45.55.25.245:8123/; font-src https://45.55.25.245:8123/; img-src https://45.55.25.245:8123/; media-src https://45.55.25.245:8123/; object-src https://45.55.25.245:8123/; script-src https://45.55.25.245:8123/; style-src https://45.55.25.245:8123/; form-action https://45.55.25.245:8123/; frame-ancestors 'none'; report-uri http://45.55.25.245:8123/csp-report?os=OS%20X&device=&browser_version=37.0&browser=firefox&os_version=Yosemite",
          |    "referrer": "",
          |    "violated-directive": "img-src https://45.55.25.245:8123/"
          |  }
          |}
        """.stripMargin)

      val request = FakeRequest("POST", "/report-to").withJsonBody(firefoxJson)
      val Some(result) = route(app, request)

      contentAsJson(result) must be_==(Json.obj("violation" -> "img-src https://45.55.25.245:8123/"))
    }

    "work with a webkit style csp-report" in withApplication() { implicit app =>
      val webkitJson = Json.parse(
        """{
          |"csp-report": {
          |    "document-uri": "http://45.55.25.245:8123/csp?os=OS%20X&device=&browser_version=23.0&browser=chrome&os_version=Lion",
          |    "violated-directive": "default-src https://45.55.25.245:8123/",
          |    "original-policy": "default-src  https://45.55.25.245:8123/; child-src  https://45.55.25.245:8123/; connect-src  https://45.55.25.245:8123/; font-src  https://45.55.25.245:8123/; img-src  https://45.55.25.245:8123/; media-src  https://45.55.25.245:8123/; object-src  https://45.55.25.245:8123/; script-src  https://45.55.25.245:8123/; style-src  https://45.55.25.245:8123/; form-action  https://45.55.25.245:8123/; frame-ancestors 'none'; plugin-types 'none'; report-uri http://45.55.25.245:8123/csp-report?os=OS%20X&device=&browser_version=23.0&browser=chrome&os_version=Lion",
          |    "blocked-uri": "http://google.com"
          |  }
          |}
        """.stripMargin)

      val request = FakeRequest("POST", "/report-to").withJsonBody(webkitJson)
      val Some(result) = route(app, request)

      contentAsJson(result) must be_==(Json.obj("violation" -> "default-src https://45.55.25.245:8123/"))
    }

    "work with a old webkit style csp-report" in withApplication() { implicit app =>
      val request = FakeRequest("POST", "/report-to").withFormUrlEncodedBody(
        "document-url" -> "http://45.55.25.245:8123/csp?os=OS%2520X&device=&browser_version=3.6&browser=firefox&os_version=Yosemite",
        "violated-directive" -> "object-src https://45.55.25.245:8123/"
      )
      val Some(result) = route(app, request)

      contentAsJson(result) must be_==(Json.obj("violation" -> "object-src https://45.55.25.245:8123/"))
    }
  }

}

object ScalaCSPReportSpec {

  class MyAction @Inject() (cspReportAction: CSPReportActionBuilder, cc: ControllerComponents) extends AbstractController(cc) {
    def cspReport = cspReportAction { implicit request =>
      val json = Json.toJson(Map("violation" -> request.body.violatedDirective))
      Ok(json)
    }
  }

}
