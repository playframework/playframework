/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.forms.scalaforms {

import play.api.{Environment, Configuration}
import play.api.i18n.{DefaultLangs, DefaultMessagesApi, Messages}

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

  val conf = Configuration.reference
  implicit val messages: Messages = new DefaultMessagesApi(Environment.simple(), conf, new DefaultLangs(conf)).preferred(Seq.empty)

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

      controllers.Application.userFormTupleName === "bob"
    }

    "handling binding failure" in {
      val userForm = controllers.Application.userFormConstraints

      implicit val request = FakeRequest().withFormUrlEncodedBody("name" -> "", "age" -> "25")

      val boundForm = userForm.bindFromRequest
      boundForm.hasErrors must beTrue
    }
    
    "display global errors user template" in {
      val userForm = controllers.Application.userFormConstraintsAdHoc
      
      implicit val request = FakeRequest().withFormUrlEncodedBody("name" -> "Johnny Utah", "age" -> "25")
      
      val boundForm = userForm.bindFromRequest
      boundForm.hasGlobalErrors must beTrue
      
      val html = views.html.user(boundForm)
      html.body must contain("Failed form constraints!")
    }
    
  }
}

package models {

case class User(name: String, age: Int)

object User {
  def create(user: User): Int = 42
}

}

// We are sneaky and want these classes available without exposing our test package structure.
package views.html {

//#userData-define
case class UserData(name: String, age: Int)
//#userData-define

// #userData-nested
case class AddressData(street: String, city: String)

case class UserAddressData(name: String, address: AddressData)
// #userData-nested

// #userListData
case class UserListData(name: String, emails: List[String])
// #userListData

// #userData-optional
case class UserOptionalData(name: String, email: Option[String])
// #userData-optional

}

package views.html.contact {

// #contact-define
case class Contact(firstname: String,
                   lastname: String,
                   company: Option[String],
                   informations: Seq[ContactInformation])

object Contact {
  def save(contact: Contact): Int = 99
}

case class ContactInformation(label: String,
                              email: Option[String],
                              phones: List[String])
// #contact-define

}

package controllers {

import views.html._
import views.html.contact._
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

class Application extends Controller {

  //#userForm-define
  val userForm = Form(
    mapping(
      "name" -> text,
      "age" -> number
    )(UserData.apply)(UserData.unapply)
  )
  //#userForm-define

  def home(id: Int = 0) = Action {
    Ok("Welcome!")
  }

  // #form-render
  def index = Action {
    Ok(views.html.user(userForm))
  }
  // #form-render

  def userPostHandlingFailure() = Action { implicit request =>
    val userForm = userFormConstraints

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

  // #form-bodyparser
  val userPost = Action(parse.form(userForm)) { implicit request =>
    val userData = request.body
    val newUser = models.User(userData.name, userData.age)
    val id = models.User.create(newUser)
    Redirect(routes.Application.home(id))
  }
  // #form-bodyparser

  // #form-bodyparser-errors
  val userPostWithErrors = Action(parse.form(userForm, onErrors = (formWithErrors: Form[UserData]) => BadRequest(views.html.user(formWithErrors)))) { implicit request =>
    val userData = request.body
    val newUser = models.User(userData.name, userData.age)
    val id = models.User.create(newUser)
    Redirect(routes.Application.home(id))
  }
  // #form-bodyparser-errors

  def submit = Action { implicit request =>
    BadRequest("Not used")
  }

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

  // #userForm-tuple
  val userFormTuple = Form(
    tuple(
      "name" -> text,
      "age" -> number
    ) // tuples come with built-in apply/unapply
  )
  // #userForm-tuple

  val userFormTupleName = {
    // #userForm-tuple-example
    val anyData = Map("name" -> "bob", "age" -> "25")
    val (name, age) = userFormTuple.bind(anyData).get
    // #userForm-tuple-example
    name
  }

  // #contact-form
  val contactForm: Form[Contact] = Form(

    // Defines a mapping that will handle Contact values
    mapping(
      "firstname" -> nonEmptyText,
      "lastname" -> nonEmptyText,
      "company" -> optional(text),

      // Defines a repeated mapping
      "informations" -> seq(
        mapping(
          "label" -> nonEmptyText,
          "email" -> optional(email),
          "phones" -> list(
            text verifying pattern("""[0-9.+]+""".r, error="A valid phone number is required")
          )
        )(ContactInformation.apply)(ContactInformation.unapply)
      )
    )(Contact.apply)(Contact.unapply)
  )
  // #contact-form

  // #contact-edit
  def editContact = Action {
    val existingContact = Contact(
      "Fake", "Contact", Some("Fake company"), informations = List(
        ContactInformation(
          "Personal", Some("fakecontact@gmail.com"), List("01.23.45.67.89", "98.76.54.32.10")
        ),
        ContactInformation(
          "Professional", Some("fakecontact@company.com"), List("01.23.45.67.89")
        ),
        ContactInformation(
          "Previous", Some("fakecontact@oldcompany.com"), List()
        )
      )
    )
    Ok(views.html.contact.form(contactForm.fill(existingContact)))
  }
  // #contact-edit
  
  // #contact-save
  def saveContact = Action { implicit request =>
    contactForm.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(views.html.contact.form(formWithErrors))
      },
      contact => {
        val contactId = Contact.save(contact)
        Redirect(routes.Application.showContact(contactId)).flashing("success" -> "Contact saved!")
      }
    )
  }
  // #contact-save
  
  def showContact(id: Int) = Action {
    Ok("Contact id: " + id)
  }

}

object Application extends Application
}

}
