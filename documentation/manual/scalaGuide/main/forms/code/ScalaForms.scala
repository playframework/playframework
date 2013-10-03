/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.forms.scalaforms {

import scalaguide.forms.scalaforms.controllers.routes

import play.api.mvc._
import play.api.test._

import org.specs2.mutable.Specification
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

// #form-imports
import play.api.data._
import play.api.data.Forms._
// #form-imports

// #validation-imports
import play.api.data.validation.Constraints._
// #validation-imports

@RunWith(classOf[JUnitRunner])
class ScalaFormsSpec extends Specification with Controller {

  "A scala forms" should {

    "generate from map" in {

      val userForm = controllers.Application.userForm

      //#userForm-generate-map
      val anyData = Map("name" -> "bob", "age" -> "21")
      val userData = userForm.bind(anyData).get
      //#userForm-generate-map

      userData.name === "bob"
    }

    "generate from request" in {

      import play.api.libs.json.Json
      val userForm = controllers.Application.userForm

      val anyData = Json.parse( """{"name":"bob","age":"21"}""")
      implicit val request = FakeRequest().withBody(anyData)
      //#userForm-generate-request
      val userData = userForm.bindFromRequest.get
      //#userForm-generate-request

      userData.name === "bob"
    }

    "get user info from form" in {

      controllers.Application.userFormName === "bob"

      controllers.Application.userFormVerifyName === "bob"

      controllers.Application.userFormConstraintsName === "bob"

      controllers.Application.userFormConstraints2Name === "bob"

      controllers.Application.userFormConstraintsAdhocName === "bob"

      controllers.Application.userFormNestedCity === "Shanghai"

      controllers.Application.userFormRepeatedEmails === List("benewu@gmail.com", "bob@gmail.com")

      controllers.Application.userFormOptionalEmail === None

      controllers.Application.userFormStaticId === 23
    }

    "handling binding failure" in {
      val userForm = controllers.Application.userFormConstraints

      implicit val request = FakeRequest().withBody("name" -> "", "age" -> "25")

      val boundForm = userForm.bindFromRequest
      boundForm.hasErrors must be_==(true)
    }
  }
}

package models {

case class User(name: String, age: Int)

object User {
  def create(user: User): Int = 42
}

}

package controllers {

//#userData-define
case class UserData(name: String, age: Int)

//#userData-define

object Application extends Controller {

  def home(id: Int = 0) = Action {
    Ok("Welcome!")
  }

  def userPost() = Action {
    val userForm = controllers.Application.userFormConstraints

    //#userForm-handling-failure
    userForm.bindFromRequest.fold(
      formWithErrors => {
        // binding failure, you retrieve the form containing errors:
        BadRequest(views.html.user(formWithErrors))
      },
      userData => {
        /* binding success, you get the actual value. */
        val newUser = models.User(userData.name, userData.age)
        val id = models.User.create(newUser)
        Redirect(routes.Application.home(id))
      }
    )
    //#userForm-handling-failure

  }

  //#userForm-define
  val userForm = Form(
    mapping(
      "name" -> text,
      "age" -> number
    )(UserData.apply)(UserData.unapply)
  )
  //#userForm-define

  val userFormName = {
    //#userForm-get
    val anyData = Map("name" -> "bob", "age" -> "18")
    val user: UserData = userForm.bind(anyData).get
    //#userForm-get

    //#userForm-filled
    val filledForm = userForm.fill(UserData("Bob", 18))
    //#userForm-filled

    user.name
  }

  //#userForm-verify
  val userFormVerify = Form(
    mapping(
      "name" -> text,
      "age" -> number,
      "accept" -> checked("Please accept the terms and conditions")
    )((name, age, _) => UserData(name, age))
      ((user: UserData) => Some(user.name, user.age, false))
  )
  //#userForm-verify

  val userFormVerifyName = {
    val anyData = Map("name" -> "bob", "age" -> "18", "accept" -> "true")
    val user: UserData = userFormVerify.bind(anyData).get
    user.name
  }

  //#userForm-constraints
  val userFormConstraints = Form(
    mapping(
      "name" -> text.verifying(nonEmpty),
      "age" -> number.verifying(min(0), max(100))
    )(UserData.apply)(UserData.unapply)
  )
  //#userForm-constraints

  val userFormConstraintsName = {
    val anyData = Map("name" -> "bob", "age" -> "18", "accept" -> "true")
    val user: UserData = userFormConstraints.bind(anyData).get
    user.name
  }

  //#userForm-constraints-2
  val userFormConstraints2 = Form(
    mapping(
      "name" -> nonEmptyText,
      "age" -> number(min = 0, max = 100)
    )(UserData.apply)(UserData.unapply)
  )
  //#userForm-constraints-2

  val userFormConstraints2Name = {
    val anyData = Map("name" -> "bob", "age" -> "18", "accept" -> "true")
    val user: UserData = userFormConstraints2.bind(anyData).get
    user.name
  }

  //#userForm-constraints-ad-hoc
  def validate(name: String, age: Int) = {
    name match {
      case "bob" if age >= 18 =>
        Some(UserData(name, age))
      case "admin" =>
        Some(UserData(name, age))
      case _ =>
        None
    }
  }

  val userFormConstraintsAdHoc = Form(
    mapping(
      "name" -> text,
      "age" -> number
    )(UserData.apply)(UserData.unapply) verifying("Failed form constraints!", fields => fields match {
      case userData => validate(userData.name, userData.age).isDefined
    })
  )
  //#userForm-constraints-ad-hoc

  val userFormConstraintsAdhocName = {
    val anyData = Map("name" -> "bob", "age" -> "18")
    val formData = userFormConstraintsAdHoc.bind(anyData).get
    formData.name
  }

  case class AddressData(street: String, city: String)

  case class UserAddressData(name: String, address: AddressData)

  //#userForm-nested
  val userFormNested: Form[UserAddressData] = Form(
    mapping(
      "name" -> text,
      "address" -> mapping(
        "street" -> text,
        "city" -> text
      )(AddressData.apply)(AddressData.unapply)
    )(UserAddressData.apply)(UserAddressData.unapply)
  )
  //#userForm-nested

  val userFormNestedCity = {
    val anyData = Map("name" -> "bob@gmail.com", "address.street" -> "Century Road.", "address.city" -> "Shanghai")
    val user = userFormNested.bind(anyData).get
    user.address.city
  }

  // #userListData
  case class UserListData(name: String, emails: List[String])
  // #userListData

  //#userForm-repeated
  val userFormRepeated = Form(
    mapping(
      "name" -> text,
      "emails" -> list(email)
    )(UserListData.apply)(UserListData.unapply)
  )
  //#userForm-repeated

  val userFormRepeatedEmails = {
    val anyData = Map("name" -> "bob", "emails[0]" -> "benewu@gmail.com", "emails[1]" -> "bob@gmail.com")
    val user = userFormRepeated.bind(anyData).get

    user.emails
  }

  case class UserOptionalData(name: String, email: Option[String])

  //#userForm-optional
  val userFormOptional = Form(
    mapping(
      "name" -> text,
      "email" -> optional(email)
    )(UserOptionalData.apply)(UserOptionalData.unapply)
  )
  //#userForm-optional

  val userFormOptionalEmail = {
    val anyData = Map("name" -> "bob")
    val user = userFormOptional.bind(anyData).get

    user.email
  }

  case class UserStaticData(id: Long, name: String, email: Option[String])

  //#userForm-static-value
  val userFormStatic = Form(
    mapping(
      "id" -> ignored(23L),
      "name" -> text,
      "email" -> optional(email)
    )(UserStaticData.apply)(UserStaticData.unapply)
  )
  //#userForm-static-value

  val userFormStaticId = {
    val anyData = Map("id" -> "1", "name" -> "bob")
    val user = userFormStatic.bind(anyData).get

    user.id
  }


}

}

}
