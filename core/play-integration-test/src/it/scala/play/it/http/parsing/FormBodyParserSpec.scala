/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http.parsing

import scala.concurrent.Future
import scala.jdk.CollectionConverters._

import akka.stream.scaladsl.Source
import akka.stream.Materializer
import akka.util.ByteString
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.nonEmptyText
import play.api.data.Forms.number
import play.api.http.MimeTypes
import play.api.http.Writeable
import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Injecting
import play.api.test.PlaySpecification
import play.api.test.WithApplication
import play.api.Application

class FormBodyParserSpec extends PlaySpecification {
  sequential

  "The form body parser" should {
    def parse[A, B](
        body: B,
        bodyParser: BodyParser[A]
    )(implicit writeable: Writeable[B], mat: Materializer): Either[Result, A] = {
      await(
        bodyParser(FakeRequest().withHeaders(writeable.contentType.map(CONTENT_TYPE -> _).toSeq: _*))
          .run(Source.single(writeable.transform(body)))
      )
    }

    case class User(name: String, age: Int)
    object User {
      def unapply(u: User): Option[(String, Int)] = Some(u.name, u.age)
    }

    val userForm = Form(mapping("name" -> nonEmptyText, "age" -> number)(User.apply)(User.unapply))

    "bind JSON requests" in new WithApplication() with Injecting {
      val parsers = inject[PlayBodyParsers]
      parse(Json.obj("name" -> "Alice", "age" -> 42), parsers.form(userForm)) must beRight(User("Alice", 42))
    }

    "bind form-urlencoded requests" in new WithApplication() with Injecting {
      val parsers = inject[PlayBodyParsers]
      parse(Map("name" -> Seq("Alice"), "age" -> Seq("42")), parsers.form(userForm)) must beRight(User("Alice", 42))
    }

    "not bind erroneous body" in new WithApplication() with Injecting {
      val parsers = inject[PlayBodyParsers]
      parse(Json.obj("age" -> "Alice"), parsers.form(userForm)) must beLeft(Results.BadRequest)
    }

    "allow users to override the error reporting behaviour" in new WithApplication() with Injecting {
      val parsers                     = inject[PlayBodyParsers]
      val messagesApi                 = app.injector.instanceOf[MessagesApi]
      implicit val messages: Messages = messagesApi.preferred(Seq.empty)
      parse(
        Json.obj("age" -> "Alice"),
        parsers.form(userForm, onErrors = (form: Form[User]) => Results.BadRequest(form.errorsAsJson))
      ) must beLeft.which { result =>
        result.header.status must equalTo(BAD_REQUEST)
        val json = contentAsJson(Future.successful(result))
        (json \ "age")(0).asOpt[String] must beSome("Numeric value expected")
        (json \ "name")(0).asOpt[String] must beSome("This field is required")
      }
    }
  }

  "The Java form body parser" should {
    def javaParserTest(bodyString: String, bodyData: Map[String, Seq[String]], bodyCharset: Option[String] = None)(
        implicit app: Application
    ): Unit = {
      val parser      = app.injector.instanceOf[play.mvc.BodyParser.FormUrlEncoded]
      val mat         = app.injector.instanceOf[Materializer]
      val bs          = akka.stream.javadsl.Source.single(ByteString.fromString(bodyString, bodyCharset.getOrElse("UTF-8")))
      val contentType = bodyCharset.fold(MimeTypes.FORM)(charset => s"${MimeTypes.FORM};charset=$charset")
      val req         = new play.mvc.Http.RequestBuilder().header(CONTENT_TYPE, contentType).build()
      val result      = parser(req).run(bs, mat).toCompletableFuture.get
      result.right.get.asScala.view.mapValues(_.toSeq).toMap must_== bodyData
    }

    "parse bodies in UTF-8" in new WithApplication() {
      val bodyString = "name=%C3%96sten&age=42"
      val bodyData   = Map("name" -> Seq("Östen"), "age" -> Seq("42"))
      javaParserTest(bodyString, bodyData)
    }

    "parse bodies in ISO-8859-1" in new WithApplication() {
      val bodyString = "name=%D6sten&age=42"
      val bodyData   = Map("name" -> Seq("Östen"), "age" -> Seq("42"))
      javaParserTest(bodyString, bodyData, Some("ISO-8859-1"))
    }
  }
}
