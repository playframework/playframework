package scalaguide.scalaforms {

import play.api.mvc._
import play.api.test._

import org.specs2.mutable.Specification
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import scala.concurrent.Future


@RunWith(classOf[JUnitRunner])
class ScalaFormsSpec extends Specification with Controller {

  "A scala forms" should {

    "generate from map" in {

      val loginForm = controllers.Application.loginForm

      //#loginForm-generate-map
      val anyData = Map("email" -> "bob@gmail.com", "password" -> "secret")
      val (user, password) = loginForm.bind(anyData).get
      //#loginForm-generate-map

      user === "bob@gmail.com"
    }

    "generate from request" in {

      import play.api.libs.json.Json
      val loginForm = controllers.Application.loginForm

      val anyData = Json.parse( """{"email":"bob@gmail.com","password":"secret"}""")
      implicit val request = FakeRequest().withBody(anyData)
      //#loginForm-generate-request
      val (user, password) = loginForm.bindFromRequest.get
      //#loginForm-generate-request

      user === "bob@gmail.com"
    }

    "get Case Class User from form" in {

      controllers.Application.userFormGet === "bob"

      controllers.Application.userFormVerify === "bob"

      controllers.Application.userFormConstraints === "bob"

      controllers.Application.userFormConstraints2 === "bob"

      controllers.Application.userFormConstraintsAdhoc === "bob@gmail.com"
    }

    def testAction[A](action: EssentialAction, request: => Request[A] = FakeRequest(), expectedResponse: Int = OK) {
      assertAction(action, request, expectedResponse) {
        result =>
      }
    }

    def assertAction[A](action: EssentialAction, request: => Request[A] = FakeRequest(), expectedResponse: Int = OK)(assertions: Future[SimpleResult] => Unit) {
      import play.api.test.Helpers._

      running(FakeApplication(additionalConfiguration = Map("application.secret" -> "pass"))) {
        val result = action(request).run
        status(result) must_== expectedResponse
        assertions(result)
      }
    }
  }
}

package controllers {

object Application extends Controller {

  val loginForm = {

    //#loginForm-define
    import play.api.data._
    import play.api.data.Forms._

    val loginForm = Form(
      tuple(
        "email" -> text,
        "password" -> text
      )
    )
    //#loginForm-define
    loginForm
  }

  val userFormGet = {
    //#userForm-get
    import play.api.data._
    import play.api.data.Forms._

    case class User(name: String, age: Int)

    val userForm = Form(
      mapping(
        "name" -> text,
        "age" -> number
      )(User.apply)(User.unapply)
    )

    val anyData = Map("name" -> "bob", "age" -> "18")
    val user: User = userForm.bind(anyData).get
    //#userForm-get
    user.name
  }

  val userFormVerify = {
    import play.api.data._
    import play.api.data.Forms._

    case class User(name: String, age: Int)

    //#userForm-verify
    val userForm = Form(
      mapping(
        "name" -> text,
        "age" -> number,
        "accept" -> checked("Please accept the terms and conditions")
      )((name, age, _) => User(name, age))
        ((user: User) => Some(user.name, user.age, false))
    )
    //#userForm-verify

    val anyData = Map("name" -> "bob", "age" -> "18","accept" -> "true")
    val user: User = userForm.bind(anyData).get
    user.name
  }

  val userFormConstraints = {
    //#userForm-constraints
    import play.api.data._
    import play.api.data.Forms._
    import play.api.data.validation.Constraints._

    case class User(name: String, age: Int)

    val userForm = Form(
      mapping(
        "name" -> text.verifying(nonEmpty),
        "age" -> number.verifying(min(0), max(100))
      )(User.apply)(User.unapply)
    )
    //#userForm-constraints

    val anyData = Map("name" -> "bob", "age" -> "18","accept" -> "true")
    val user: User = userForm.bind(anyData).get
    user.name
  }

  val userFormConstraints2 = {

    import play.api.data._
    import play.api.data.Forms._

    case class User(name: String, age: Int)

    val userForm = Form(
      //#userForm-constraints-2
      mapping(
        "name" -> nonEmptyText,
        "age" -> number(min=0, max=100)
      )
       //#userForm-constraints-2
      (User.apply)(User.unapply)
    )


    val anyData = Map("name" -> "bob", "age" -> "18","accept" -> "true")
    val user: User = userForm.bind(anyData).get
    user.name
  }


  val userFormConstraintsAdhoc = {

    import play.api.data._
    import play.api.data.Forms._

    object User {
      def authenticate(email:String,password:String) = Some((email,password))
    }
      //#userForm-constraints-ad-hoc
      val loginForm = Form(
        tuple(
          "email" -> email,
          "password" -> text
        ) verifying("Invalid user name or password", fields => fields match {
          case (e, p) => User.authenticate(e,p).isDefined
        })
      )
      //#userForm-constraints-ad-hoc


    val anyData = Map("email" -> "bob@gmail.com", "password" -> "password")
    val (user,password) = loginForm.bind(anyData).get
    user
  }
}

}

}
