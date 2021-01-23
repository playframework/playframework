/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.csp

import java.util.concurrent.CompletableFuture

import play.api.Application
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsArray
import play.api.libs.json.JsNumber
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.Json
import play.api.test._
import play.core.j._
import play.core.routing.HandlerInvokerFactory
import play.mvc._

import scala.reflect.ClassTag

/**
 * Test body parser for different styles of CSP.
 *
 * See https://www.tollmanz.com/content-security-policy-report-samples/ for the gory details.
 */
class JavaCSPReportSpec extends PlaySpecification {
  sequential

  private def inject[T: ClassTag](implicit app: Application) = app.injector.instanceOf[T]

  private def javaHandlerComponents(implicit app: Application) = inject[JavaHandlerComponents]
  private def myAction(implicit app: Application)              = inject[JavaCSPReportSpec.MyAction]

  def javaAction[T: ClassTag](method: String, inv: Http.Request => Result)(implicit app: Application): JavaAction =
    new JavaAction(javaHandlerComponents) {
      val clazz: Class[_] = implicitly[ClassTag[T]].runtimeClass
      def parser: play.api.mvc.BodyParser[Http.RequestBody] =
        HandlerInvokerFactory.javaBodyParserToScala(javaHandlerComponents.getBodyParser(annotations.parser))
      def invocation(req: Http.Request): CompletableFuture[Result] = CompletableFuture.completedFuture(inv(req))
      val annotations =
        new JavaActionAnnotations(
          clazz,
          clazz.getMethod(method, classOf[Http.Request]),
          handlerComponents.httpConfiguration.actionComposition
        )
    }

  def withActionServer[T](config: (String, String)*)(block: Application => T): T = {
    val app = GuiceApplicationBuilder()
      .configure(
        Map(config: _*) ++ Map(
          "play.http.secret.key"   -> "ad31779d4ee49d5ad5162bf1429c32e2e9933f3b",
          "play.http.errorHandler" -> "play.http.JsonHttpErrorHandler"
        )
      )
      .appRoutes(implicit app => { case _ => javaAction[JavaCSPReportSpec.MyAction]("cspReport", myAction.cspReport) })
      .build()
    block(app)
  }

  "Java CSP report" should {
    "work with a chrome style csp-report" in withActionServer() { implicit app =>
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
        """.stripMargin
      )

      val request      = FakeRequest("POST", "/report-to").withJsonBody(chromeJson)
      val Some(result) = route(app, request)

      status(result) must_=== Status.OK
      contentType(result) must beSome("application/json")
      contentAsJson(result) must be_==(Json.obj("violation" -> "child-src https://45.55.25.245:8123/"))
    }

    "work with a firefox style csp-report" in withActionServer() { implicit app =>
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
        """.stripMargin
      )

      val request      = FakeRequest("POST", "/report-to").withJsonBody(firefoxJson)
      val Some(result) = route(app, request)

      status(result) must_=== Status.OK
      contentType(result) must beSome("application/json")
      contentAsJson(result) must be_==(Json.obj("violation" -> "img-src https://45.55.25.245:8123/"))
    }

    "work with a webkit style csp-report" in withActionServer() { implicit app =>
      val webkitJson = Json.parse(
        """{
          |"csp-report": {
          |    "document-uri": "http://45.55.25.245:8123/csp?os=OS%20X&device=&browser_version=23.0&browser=chrome&os_version=Lion",
          |    "violated-directive": "default-src https://45.55.25.245:8123/",
          |    "original-policy": "default-src  https://45.55.25.245:8123/; child-src  https://45.55.25.245:8123/; connect-src  https://45.55.25.245:8123/; font-src  https://45.55.25.245:8123/; img-src  https://45.55.25.245:8123/; media-src  https://45.55.25.245:8123/; object-src  https://45.55.25.245:8123/; script-src  https://45.55.25.245:8123/; style-src  https://45.55.25.245:8123/; form-action  https://45.55.25.245:8123/; frame-ancestors 'none'; plugin-types 'none'; report-uri http://45.55.25.245:8123/csp-report?os=OS%20X&device=&browser_version=23.0&browser=chrome&os_version=Lion",
          |    "blocked-uri": "http://google.com"
          |  }
          |}
        """.stripMargin
      )

      val request      = FakeRequest("POST", "/report-to").withJsonBody(webkitJson)
      val Some(result) = route(app, request)

      status(result) must_=== Status.OK
      contentType(result) must beSome("application/json")
      contentAsJson(result) must be_==(Json.obj("violation" -> "default-src https://45.55.25.245:8123/"))
    }

    "work with a old webkit style csp-report" in withActionServer() { implicit app =>
      val request = FakeRequest("POST", "/report-to").withFormUrlEncodedBody(
        "document-url"       -> "http://45.55.25.245:8123/csp?os=OS%2520X&device=&browser_version=3.6&browser=firefox&os_version=Yosemite",
        "violated-directive" -> "object-src https://45.55.25.245:8123/"
      )
      val Some(result) = route(app, request)

      status(result) must_=== Status.OK
      contentType(result) must beSome("application/json")
      contentAsJson(result) must be_==(Json.obj("violation" -> "object-src https://45.55.25.245:8123/"))
    }

    "fail when receiving an unsupported media type (text/plain) in content type header" in withActionServer() {
      implicit app =>
        val request      = FakeRequest("POST", "/report-to").withTextBody("foo")
        val Some(result) = route(app, request)

        status(result) must_=== Status.UNSUPPORTED_MEDIA_TYPE
        contentType(result) must beSome("application/problem+json")
        val fullJson = contentAsJson(result).asInstanceOf[JsObject]
        // The value of "requestId" is not constant, it changes, so we just check for its existence
        fullJson.fields.count(_._1 == "requestId") must_=== 1
        // Lets remove "requestId" now
        fullJson - "requestId" must be_==(
          JsObject(
            Seq(
              "title"  -> JsString("Unsupported Media Type"),
              "status" -> JsNumber(Status.UNSUPPORTED_MEDIA_TYPE),
              "detail" -> JsString(
                "Content type must be one of application/x-www-form-urlencoded,text/json,application/json,application/csp-report but was Some(text/plain)"
              ),
            )
          )
        )
    }

    "fail when receiving invalid csp-report JSON" in withActionServer() { implicit app =>
      val invalidCspReportJson = Json.parse(
        """{
          | "csp-report": {
          |    "foo": "bar"
          |  }
          |}
        """.stripMargin
      )

      val request      = FakeRequest("POST", "/report-to").withJsonBody(invalidCspReportJson)
      val Some(result) = route(app, request)

      status(result) must_=== Status.BAD_REQUEST
      contentType(result) must beSome("application/problem+json")
      val fullJson = contentAsJson(result).asInstanceOf[JsObject]
      // The value of "requestId" is not constant, it changes, so we just check for its existence
      fullJson.fields.count(_._1 == "requestId") must_=== 1
      // Lets remove "requestId" now
      fullJson - "requestId" must be_==(
        JsObject(
          Seq(
            "title"  -> JsString("Bad Request"),
            "status" -> JsNumber(Status.BAD_REQUEST),
            "detail" -> JsString("Could not parse CSP"),
            "errors" -> JsObject(
              Seq(
                "obj.document-uri" ->
                  JsArray(
                    Seq(
                      JsObject(Seq("msg" -> JsArray(Seq(JsString("error.path.missing"))), "args" -> JsArray(Seq.empty)))
                    )
                  ),
                "obj.violated-directive" ->
                  JsArray(
                    Seq(
                      JsObject(Seq("msg" -> JsArray(Seq(JsString("error.path.missing"))), "args" -> JsArray(Seq.empty)))
                    )
                  ),
              )
            )
          )
        )
      )
    }
  }
}

object JavaCSPReportSpec {
  class MyAction extends Controller {
    @BodyParser.Of(classOf[CSPReportBodyParser])
    def cspReport(request: Http.Request): Result = {
      import scala.collection.JavaConverters._
      val cspReport: JavaCSPReport = request.body.as(classOf[JavaCSPReport])
      val json                     = play.libs.Json.toJson(Map("violation" -> cspReport.violatedDirective).asJava)
      Results.ok(json).as(play.mvc.Http.MimeTypes.JSON)
    }
  }
}
