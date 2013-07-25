package scalaguide.http.scalabodyparsers {

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

    "A scala body parser" should {

      "parse requst as text" in {
        //#request-parse-as-text
        def save = Action { request =>
          val body: AnyContent = request.body
          val textBody: Option[String] = body.asText

          // Expecting text body
          textBody.map { text =>
            Ok("Got: " + text)
          }.getOrElse {
            BadRequest("Expecting text/plain request body")
          }
        }
        //#request-parse-as-text
        val request = FakeRequest().withTextBody("hello").withHeaders(CONTENT_TYPE -> "text/plain")
        testAction(save, request,BAD_REQUEST)
      }

      "body parser text" in {
        //#body-parser-text
        def save = Action(parse.text) { request =>
          Ok("Got: " + request.body)
        }
        //#body-parser-text
        val request = FakeRequest().withTextBody("hello").withHeaders(CONTENT_TYPE -> "text/plain")
        //testAction(save, request)
        testAction(save, request)

      }

      "body parser tolerantText" in {
        //#body-parser-tolerantText
        def save = Action(parse.tolerantText) { request =>
          Ok("Got: " + request.body)
        }
        //#body-parser-tolerantText
        val request = FakeRequest().withTextBody("hello")
        //testAction(save, request)
        testAction(save, request)
      }

      "body parser file" in {
        //#body-parser-file
        def save = Action(parse.file(to = new File("/tmp/upload"))) { request =>
          Ok("Saved the request content to " + request.body)
        }
        //#body-parser-file
        def request = FakeRequest().withTextBody("hello").withSession("username" -> "player")
        //testAction(save, request)
        testAction(save, request)
      }

      "body parser combining" in {

        val save = scalaguide.http.scalabodyparsers.full.Application.save
        def request = FakeRequest().withTextBody("hello").withSession("username" -> "player")
        testAction(save, request)
      }

      "body parser limit text" in {
        val text = "hello"
        //#body-parser-limit-text
        // Accept only 10KB of data.
        def save = Action(parse.text(maxLength = 1024 * 10)) { request =>
          Ok("Got: " + text)
        }
        //#body-parser-limit-text
        val request = FakeRequest().withTextBody("hello")
        //testAction(save, request)
        testAction(save, request,BAD_REQUEST)
      }

      "body parser limit file" in {
        val storeInUserFile = scalaguide.http.scalabodyparsers.full.Application.storeInUserFile
        //#body-parser-limit-file
        // Accept only 10KB of data.
        def save = Action(parse.maxLength(1024 * 10, storeInUserFile)) { request =>
          Ok("Saved the request content to " + request.body)
        }
        //#body-parser-limit-file
        def request = FakeRequest().withTextBody("hello").withSession("username" -> "player")
        testAction(save, request)
      }

    }

    def testAction[A](action: EssentialAction, request: => Request[A] = FakeRequest(), expectedResponse: Int = OK) = {
      assertAction(action, request, expectedResponse) { result => success }
    }

    def assertAction[A, T: AsResult](action: EssentialAction, request:  => Request[A] = FakeRequest(), expectedResponse: Int = OK)(assertions: Future[SimpleResult] => T) = {
      running(FakeApplication(additionalConfiguration = Map("application.secret" -> "pass"))) {        
        val result = action(request).run
        status(result) must_== expectedResponse
        assertions(result)
      }
    }

  }

  package scalaguide.http.scalabodyparsers.full {

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
      //#Source-Code-Action
      trait Action[A] extends (Request[A] => Result) {
        def parser: BodyParser[A]
      }
      //#Source-Code-Action

      //#Source-Code-Request
      trait Request[+A] extends RequestHeader {
        def body: A
      }
      //#Source-Code-Request

    }
  }
}
 