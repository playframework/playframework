/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.http.scalabodyparsers {

import akka.stream.ActorMaterializer
import play.api.http.Writeable
import play.api.libs.json.{Json, JsValue}
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import org.specs2.mutable.Specification
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import scala.concurrent.Future
import java.io.File
import org.specs2.execute.AsResult

  @RunWith(classOf[JUnitRunner])
  class ScalaBodyParsersSpec extends Specification with Controller {

    def helloRequest = FakeRequest("POST", "/").withJsonBody(Json.obj("name" -> "foo"))

    "A scala body parser" should {

      "parse request as json" in {
        //#access-json-body
        def save = Action { request =>
          val body: AnyContent = request.body
          val jsonBody: Option[JsValue] = body.asJson

          // Expecting json body
          jsonBody.map { json =>
            Ok("Got: " + (json \ "name").as[String])
          }.getOrElse {
            BadRequest("Expecting application/json request body")
          }
        }
        //#access-json-body
        testAction(save, helloRequest)
      }

      "body parser json" in {
        //#body-parser-json
        def save = Action(parse.json) { request =>
          Ok("Got: " + (request.body \ "name").as[String])
        }
        //#body-parser-json
        testAction(save, helloRequest)
      }

      "body parser tolerantJson" in {
        //#body-parser-tolerantJson
        def save = Action(parse.tolerantJson) { request =>
          Ok("Got: " + (request.body \ "name").as[String])
        }
        //#body-parser-tolerantJson
        testAction(save, helloRequest)
      }

      "body parser file" in {
        //#body-parser-file
        def save = Action(parse.file(to = new File("/tmp/upload"))) { request =>
          Ok("Saved the request content to " + request.body)
        }
        //#body-parser-file
        testAction(save, helloRequest.withSession("username" -> "player"))
      }

      "body parser combining" in {
        val save = scalaguide.http.scalabodyparsers.full.Application.save
        testAction(save, helloRequest.withSession("username" -> "player"))
      }

      "body parser limit text" in {
        val text = "hello"
        //#body-parser-limit-text
        // Accept only 10KB of data.
        def save = Action(parse.text(maxLength = 1024 * 10)) { request =>
          Ok("Got: " + text)
        }
        //#body-parser-limit-text
        testAction(save, FakeRequest("POST", "/").withTextBody("foo"))
      }

      "body parser limit file" in {
        running() { app =>
          implicit val mat = ActorMaterializer()(app.actorSystem)
          val storeInUserFile = scalaguide.http.scalabodyparsers.full.Application.storeInUserFile
          //#body-parser-limit-file
          // Accept only 10KB of data.
          def save = Action(parse.maxLength(1024 * 10, storeInUserFile)) { request =>
            Ok("Saved the request content to " + request.body)
          }
          //#body-parser-limit-file
          val result = call(save, helloRequest.withSession("username" -> "player"))
          status(result) must_== OK
        }
      }

      "forward the body" in new WithApplication() {

        //#forward-body
        import javax.inject._
        import play.api.mvc._
        import play.api.libs.streams._
        import play.api.libs.ws._
        import scala.concurrent.ExecutionContext
        import akka.util.ByteString

        class MyController @Inject() (ws: WSClient)(implicit ec: ExecutionContext) {

          def forward(request: WSRequest): BodyParser[WSResponse] = BodyParser { req =>
            Accumulator.source[ByteString].mapFuture { source =>
              request
                // TODO: stream body when support is implemented
                // .withBody(source)
                .execute()
                .map(Right.apply)
            }
          }

          def myAction = Action(forward(ws.url("https://example.com"))) { req =>
            Ok("Uploaded")
          }
        }
        //#forward-body

        ok
      }

      "parse the body as csv" in new WithApplication() {

        //#csv
        import play.api.mvc._
        import play.api.libs.streams._
        import play.api.libs.concurrent.Execution.Implicits.defaultContext
        import akka.util.ByteString
        import akka.stream.scaladsl._

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
        //#csv

        testAction(Action(csv)(req => Ok(req.body(1)(2))), FakeRequest("POST", "/").withTextBody("1,2\n3,4,foo\n5,6"))
      }

    }

    def testAction[A: Writeable](action: EssentialAction, request: => FakeRequest[A], expectedResponse: Int = OK) = {
      assertAction(action, request, expectedResponse) { result => success }
    }

    def assertAction[A: Writeable, T: AsResult](action: EssentialAction, request: => FakeRequest[A], expectedResponse: Int = OK)(assertions: Future[Result] => T) = {
      running() { app =>
        implicit val mat = ActorMaterializer()(app.actorSystem)
        val result = call(action, request)
        status(result) must_== expectedResponse
        assertions(result)
      }
    }

  }

  package scalaguide.http.scalabodyparsers.full {

  import akka.util.ByteString
  import play.api.libs.streams.Accumulator
  import play.api.mvc._

    object Application extends Controller {
      def file(to: File) = parse.file(to)
      //#body-parser-combining
      val storeInUserFile = parse.using { request =>
        request.session.get("username").map { user =>
          file(to = new File("/tmp/" + user + ".upload"))
        }.getOrElse {
          sys.error("You don't have the right to upload here")
        }
      }

      def save = Action(storeInUserFile) { request =>
        Ok("Saved the request content to " + request.body)
      }

      //#body-parser-combining
    }

    object CodeShow {
      //#action
      trait Action[A] extends (Request[A] => Result) {
        def parser: BodyParser[A]
      }
      //#action

      //#request
      trait Request[+A] extends RequestHeader {
        def body: A
      }
      //#request

      //#body-parser
      trait BodyParser[+A] extends (RequestHeader => Accumulator[ByteString, Either[Result, A]])
      //#body-parser
    }
  }
}
 
