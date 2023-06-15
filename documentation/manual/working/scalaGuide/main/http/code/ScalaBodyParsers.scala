/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.http.scalabodyparsers {
  import java.io.File

  import scala.concurrent.Future

  import org.apache.pekko.stream.Materializer
  import org.junit.runner.RunWith
  import org.specs2.execute
  import org.specs2.execute.AsResult
  import org.specs2.mutable.Around
  import org.specs2.mutable.SpecificationLike
  import org.specs2.runner.JUnitRunner
  import org.specs2.specification.Scope
  import play.api.http.Writeable
  import play.api.inject.guice.GuiceApplicationBuilder
  import play.api.libs.json.JsValue
  import play.api.libs.json.Json
  import play.api.mvc._
  import play.api.test._
  import play.api.test.Helpers._
  import play.api.Application

  @RunWith(classOf[JUnitRunner])
  class ScalaBodyParsersSpec extends SpecificationLike with ControllerHelpers {
    abstract class WithController(val app: play.api.Application = GuiceApplicationBuilder().build())
        extends Around
        with Scope
        with BaseController {
      protected def controllerComponents: ControllerComponents = app.injector.instanceOf[ControllerComponents]

      def this(builder: GuiceApplicationBuilder => GuiceApplicationBuilder) = {
        this(builder(GuiceApplicationBuilder()).build())
      }

      implicit def implicitApp: Application           = app
      implicit def implicitMaterializer: Materializer = app.materializer
      override def around[T: AsResult](t: => T): execute.Result = {
        Helpers.running(app)(AsResult.effectively(t))
      }
    }

    def helloRequest = FakeRequest("POST", "/").withJsonBody(Json.obj("name" -> "foo"))

    "A scala body parser" should {
      "parse request as json" in new WithController() {
        // #access-json-body
        def save: Action[AnyContent] = Action { (request: Request[AnyContent]) =>
          val body: AnyContent          = request.body
          val jsonBody: Option[JsValue] = body.asJson

          // Expecting json body
          jsonBody
            .map { json => Ok("Got: " + (json \ "name").as[String]) }
            .getOrElse {
              BadRequest("Expecting application/json request body")
            }
        }
        // #access-json-body
        testAction(save, helloRequest)
      }

      "body parser json" in new WithController() {
        // #body-parser-json
        def save: Action[JsValue] = Action(parse.json) { (request: Request[JsValue]) =>
          Ok("Got: " + (request.body \ "name").as[String])
        }
        // #body-parser-json
        testAction(save, helloRequest)
      }

      "body parser tolerantJson" in new WithController() {
        // #body-parser-tolerantJson
        def save: Action[JsValue] = Action(parse.tolerantJson) { (request: Request[JsValue]) =>
          Ok("Got: " + (request.body \ "name").as[String])
        }
        // #body-parser-tolerantJson
        testAction(save, helloRequest)
      }

      "body parser file" in new WithController() {
        // #body-parser-file
        def save: Action[File] = Action(parse.file(to = new File("/tmp/upload"))) { (request: Request[File]) =>
          Ok("Saved the request content to " + request.body)
        }
        // #body-parser-file
        testAction(save, helloRequest.withSession("username" -> "player"))
      }

      "body parser combining" in new WithController() {
        val save = new scalaguide.http.scalabodyparsers.full.Application(
          Helpers.stubControllerComponents(playBodyParsers = stubPlayBodyParsers(app.materializer))
        ).save
        testAction(save, helloRequest.withSession("username" -> "player"))
      }

      "body parser limit text" in new WithController() {
        val text = "hello"
        // #body-parser-limit-text
        // Accept only 10KB of data.
        def save: Action[String] = Action(parse.text(maxLength = 1024 * 10)) { (request: Request[String]) =>
          Ok("Got: " + text)
        }
        // #body-parser-limit-text
        testAction(save, FakeRequest("POST", "/").withTextBody("foo"))
      }

      "body parser limit file" in new WithController() {
        implicit val mat: Materializer = app.materializer
        val storeInUserFile =
          new scalaguide.http.scalabodyparsers.full.Application(controllerComponents).storeInUserFile
        // #body-parser-limit-file
        // Accept only 10KB of data.
        def save: Action[Either[MaxSizeExceeded, File]] = Action(parse.maxLength(1024 * 10, storeInUserFile)) {
          request =>
            Ok("Saved the request content to " + request.body)
        }
        // #body-parser-limit-file
        val result = call(save, helloRequest.withSession("username" -> "player"))
        status(result) must_== OK
      }

      "forward the body" in new WithApplication() {
        override def running() = {
          // #forward-body
          import javax.inject._

          import scala.concurrent.ExecutionContext

          import org.apache.pekko.util.ByteString
          import play.api.libs.streams._
          import play.api.libs.ws._
          import play.api.mvc._

          class MyController @Inject() (ws: WSClient, val controllerComponents: ControllerComponents)(
              implicit ec: ExecutionContext
          ) extends BaseController {
            def forward(request: WSRequest): BodyParser[WSResponse] = BodyParser { req =>
              Accumulator.source[ByteString].mapFuture { source =>
                request
                  .withBody(source)
                  .execute("POST")
                  .map(Right.apply)
              }
            }

            def myAction: Action[WSResponse] = Action(forward(ws.url("https://example.com"))) { req => Ok("Uploaded") }
          }
          // #forward-body

          ok
        }
      }

      "parse the body as csv" in new WithApplication() with Injecting {
        override def running() = {
          import scala.concurrent.ExecutionContext.Implicits.global

          // #csv
          import org.apache.pekko.stream.scaladsl._
          import org.apache.pekko.util.ByteString
          import play.api.libs.streams._
          import play.api.mvc.BodyParser

          val Action = inject[DefaultActionBuilder]

          val csv: BodyParser[Seq[Seq[String]]] = BodyParser { req =>
            // A flow that splits the stream into CSV lines
            val sink: Sink[ByteString, Future[Seq[Seq[String]]]] = Flow[ByteString]
              // We split by the new line character, allowing a maximum of 1000 characters per line
              .via(Framing.delimiter(ByteString("\n"), 1000, allowTruncation = true))
              // Turn each line to a String and split it by commas
              .map(_.utf8String.trim.split(",").toSeq)
              // Now we fold it into a list
              .toMat(Sink.fold(Seq.empty[Seq[String]])(_ :+ _))(Keep.right)

            // Convert the body to a Right either
            Accumulator(sink).map(Right.apply)
          }
          // #csv

          testAction(Action(csv)(req => Ok(req.body(1)(2))), FakeRequest("POST", "/").withTextBody("1,2\n3,4,foo\n5,6"))
        }
      }
    }

    def testAction[A: Writeable](action: EssentialAction, request: => FakeRequest[A], expectedResponse: Int = OK) = {
      assertAction(action, request, expectedResponse) { result => success }
    }

    def assertAction[A: Writeable, T: AsResult](
        action: EssentialAction,
        request: => FakeRequest[A],
        expectedResponse: Int = OK
    )(assertions: Future[Result] => T) = {
      running() { app =>
        implicit val mat: Materializer = app.materializer
        val result                     = call(action, request)
        status(result) must_== expectedResponse
        assertions(result)
      }
    }
  }

  package scalaguide.http.scalabodyparsers.full {
    import javax.inject.Inject

    import org.apache.pekko.util.ByteString
    import play.api.libs.streams.Accumulator
    import play.api.mvc._

    class Application @Inject() (cc: ControllerComponents) extends AbstractController(cc) {
      // #body-parser-combining
      val storeInUserFile = parse.using { request =>
        request.session
          .get("username")
          .map { user => parse.file(to = new File("/tmp/" + user + ".upload")) }
          .getOrElse {
            sys.error("You don't have the right to upload here")
          }
      }

      def save: Action[File] = Action(storeInUserFile) { request => Ok("Saved the request content to " + request.body) }

      // #body-parser-combining
    }

    object CodeShow {
      // #action
      trait Action[A] extends (Request[A] => Result) {
        def parser: BodyParser[A]
      }
      // #action

      // #request
      trait Request[+A] extends RequestHeader {
        def body: A
      }
      // #request

      // #body-parser
      trait BodyParser[+A] extends (RequestHeader => Accumulator[ByteString, Either[Result, A]])
      // #body-parser
    }
  }
}
