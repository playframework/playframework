package play.it.http.parsing

import play.api.data.Form
import play.api.data.Forms.{ mapping, nonEmptyText, number }
import play.api.http.Writeable
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.Json
import play.api.mvc.{ BodyParser, BodyParsers, Result, Results }
import play.api.test.{ FakeRequest, PlaySpecification, WithApplication }

import scala.concurrent.Future

class FormBodyParserSpec extends PlaySpecification {

  "The form body parser" should {

    def parse[A, B](body: B, bodyParser: BodyParser[A])(implicit W: Writeable[B]): Either[Result, A] = {
      await(Enumerator(W.transform(body)) |>>>
        bodyParser(FakeRequest().withHeaders(W.contentType.map(CONTENT_TYPE -> _).toSeq: _*)))
    }

    case class User(name: String, age: Int)

    val userForm = Form(mapping("name" -> nonEmptyText, "age" -> number)(User.apply)(User.unapply))

    "bind JSON requests" in new WithApplication {
      parse(Json.obj("name" -> "Alice", "age" -> 42), BodyParsers.parse.form(userForm)) must beRight(User("Alice", 42))
    }

    "bind form-urlencoded requests" in new WithApplication() {
      parse(Map("name" -> Seq("Alice"), "age" -> Seq("42")), BodyParsers.parse.form(userForm)) must beRight(User("Alice", 42))
    }

    "not bind erroneous body" in new WithApplication() {
      parse(Json.obj("age" -> "Alice"), BodyParsers.parse.form(userForm)) must beLeft(Results.BadRequest)
    }

    "allow users to override the error reporting behaviour" in new WithApplication() {
      import play.api.i18n.Messages.Implicits.applicationMessages
      parse(Json.obj("age" -> "Alice"), BodyParsers.parse.form(userForm, onErrors = (form: Form[User]) => Results.BadRequest(form.errorsAsJson))) must beLeft.which { result =>
        result.header.status must equalTo(BAD_REQUEST)
        val json = contentAsJson(Future.successful(result))
        (json \ "age")(0).asOpt[String] must beSome("Numeric value expected")
        (json \ "name")(0).asOpt[String] must beSome("This field is required")
      }
    }

  }

}
