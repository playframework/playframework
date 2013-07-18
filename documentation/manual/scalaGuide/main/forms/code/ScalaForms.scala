package scalaguide.forms.scalaforms {

import play.api.mvc._
import play.api.test._

import org.specs2.mutable.Specification
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner


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

    "get user infor from form" in {

      controllers.Application.userFormGet === "bob"

      controllers.Application.userFormVerify === "bob"

      controllers.Application.userFormConstraints === "bob"

      controllers.Application.userFormConstraints2 === "bob"

      controllers.Application.userFormConstraintsAdhoc === "bob@gmail.com"

      controllers.Application.userFormNested === "Shanghai"

      controllers.Application.userFormRepeated === List("benewu@gmail.com","bob@gmail.com")

      controllers.Application.userFormOptional === None

      controllers.Application.userFormStaticValue === 23
    }

    "handling binding failure" in {

      import play.api.libs.json.Json
      import scalaguide.forms.scalaformhelper.views
      import scalaguide.forms.scalaforms.controllers.routes

      val loginForm = controllers.Application.loginForm

      val anyData = Json.parse( """{"email":"bob@gmail.com","password":"secret"}""")

      //#loginForm-handling-failure
      implicit val request = FakeRequest().withBody(anyData)
      loginForm.bindFromRequest.fold(
        formWithErrors => // binding failure, you retrieve the form containing errors,
          BadRequest(views.html.login(formWithErrors)),
        value => // binding success, you get the actual value
          Redirect(routes.Application.home).flashing(("message" ,"Welcome!" + value._1))
      )
      //#loginForm-handling-failure


      val (user, password) = loginForm.bindFromRequest.get
      user === "bob@gmail.com"
    }


  }
}

package controllers {

object Application extends Controller {

  def home = Action{
    Ok("Welcome!")
  }

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

    //#userForm-filled
    val filledForm = userForm.fill(User("Bob", 18))
    //#userForm-filled

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

    val anyData = Map("name" -> "bob", "age" -> "18", "accept" -> "true")
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

    val anyData = Map("name" -> "bob", "age" -> "18", "accept" -> "true")
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
        "age" -> number(min = 0, max = 100)
      )
        //#userForm-constraints-2
        (User.apply)(User.unapply)
    )


    val anyData = Map("name" -> "bob", "age" -> "18", "accept" -> "true")
    val user: User = userForm.bind(anyData).get
    user.name
  }


  val userFormConstraintsAdhoc = {

    import play.api.data._
    import play.api.data.Forms._

    object User {
      def authenticate(email: String, password: String) = Some((email, password))
    }
    //#userForm-constraints-ad-hoc
    val loginForm = Form(
      tuple(
        "email" -> email,
        "password" -> text
      ) verifying("Invalid user name or password", fields => fields match {
        case (e, p) => User.authenticate(e, p).isDefined
      })
    )
    //#userForm-constraints-ad-hoc


    val anyData = Map("email" -> "bob@gmail.com", "password" -> "password")
    val (user, password) = loginForm.bind(anyData).get
    user
  }

  val userFormNested = {

    import play.api.data._
    import play.api.data.Forms._

    //#userForm-nested
    case class User(name: String, address: Address)
    case class Address(street: String, city: String)

    val userForm = Form(
      mapping(
        "name" -> text,
        "address" -> mapping(
          "street" -> text,
          "city" -> text
        )(Address.apply)(Address.unapply)
      )(User.apply)(User.unapply)
    )
    //#userForm-nested

    val anyData = Map("name" -> "bob@gmail.com", "address.street" -> "Century Road.","address.city" -> "Shanghai")
    val user = userForm.bind(anyData).get
    user.address.city
  }

  val userFormRepeated = {
    import play.api.data._
    import play.api.data.Forms._

    //#userForm-repeated
    case class User(name: String, emails: List[String])

    val userForm = Form(
      mapping(
        "name" -> text,
        "emails" -> list(email)
      )(User.apply)(User.unapply)
    )
    //#userForm-repeated

    val anyData = Map("name" -> "bob", "emails[0]" -> "benewu@gmail.com","emails[1]" -> "bob@gmail.com")
    val user: User = userForm.bind(anyData).get

    user.emails
  }

  val userFormOptional = {
    import play.api.data._
    import play.api.data.Forms._

    //#userForm-optional
    case class User(name: String, email: Option[String])

    val userForm = Form(
      mapping(
        "name" -> text,
        "email" -> optional(email)
      )(User.apply)(User.unapply)
    )
    //#userForm-optional

    val anyData = Map("name" -> "bob")
    val user: User = userForm.bind(anyData).get

    user.email
  }

  val userFormStaticValue = {
    import play.api.data._
    import play.api.data.Forms._

    //#userForm-static-value
    case class User(id:Long, name: String, email: Option[String])

    val userForm = Form(
      mapping(
        "id" -> ignored(23L),
        "name" -> text,
        "email" -> optional(email)
      )(User.apply)(User.unapply)
    )
    //#userForm-static-value

    val anyData = Map("id" -> "1", "name" -> "bob")
    val user: User = userForm.bind(anyData).get

    user.id
  }



}

}

}
