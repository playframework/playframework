/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.forms.scalaforms {

  import javax.inject.Inject

  import java.net.URL

  import play.api.Configuration
  import play.api.Environment
  import play.api.i18n._

  import scalaguide.forms.scalaforms.controllers.routes
  import play.api.mvc._
  import play.api.test.WithApplication
  import play.api.test._
  import play.api.test._
  import org.specs2.mutable.Specification
  import org.junit.runner.RunWith
  import org.specs2.runner.JUnitRunner

  import scala.concurrent.ExecutionContext
  import scala.concurrent.Future

// #form-imports
  import play.api.data._
  import play.api.data.Forms._
// #form-imports

// #validation-imports
  import play.api.data.validation.Constraints._
// #validation-imports

  @RunWith(classOf[JUnitRunner])
  class ScalaFormsSpec extends Specification with ControllerHelpers {

    val messagesApi                 = new DefaultMessagesApi()
    implicit val messages: Messages = messagesApi.preferred(Seq.empty)

    "A scala forms" should {

      "generate from map" in new WithApplication {
        val controller = app.injector.instanceOf[controllers.Application]
        val userForm   = controller.userForm

        //#userForm-generate-map
        val anyData  = Map("name" -> "bob", "age" -> "21")
        val userData = userForm.bind(anyData).get
        //#userForm-generate-map

        userData.name === "bob"
      }

      "generate from request" in new WithApplication {
        import play.api.libs.json.Json

        val controller = app.injector.instanceOf[controllers.Application]
        val userForm   = controller.userForm

        val anyData          = Json.parse("""{"name":"bob","age":"21"}""")
        implicit val request = FakeRequest().withBody(anyData)
        //#userForm-generate-request
        val userData = userForm.bindFromRequest.get
        //#userForm-generate-request

        userData.name === "bob"
      }

      "get user info from form" in new WithApplication {

        val controller = app.injector.instanceOf[controllers.Application]
        controller.userFormName === "bob"
        controller.userFormVerifyName === "bob"
        controller.userFormConstraintsName === "bob"
        controller.userFormConstraints2Name === "bob"
        controller.userFormConstraintsAdhocName === "bob"
        controller.userFormNestedCity === "Shanghai"
        controller.userFormRepeatedEmails === List("benewu@gmail.com", "bob@gmail.com")
        controller.userFormOptionalEmail === None
        controller.userFormStaticId === 23
        controller.userFormTupleName === "bob"
      }

      "handling form with errors" in new WithApplication {
        val controller           = app.injector.instanceOf[controllers.Application]
        val userFormConstraints2 = controller.userFormConstraints2

        implicit val request = FakeRequest().withFormUrlEncodedBody("name" -> "", "age" -> "25")

        //#userForm-constraints-2-with-errors
        val boundForm = userFormConstraints2.bind(Map("bob" -> "", "age" -> "25"))
        boundForm.hasErrors must beTrue
        //#userForm-constraints-2-with-errors
      }

      "handling binding failure" in new WithApplication {
        val controller = app.injector.instanceOf[controllers.Application]
        val userForm   = controller.userFormConstraints

        implicit val request = FakeRequest().withFormUrlEncodedBody("name" -> "", "age" -> "25")

        val boundForm = userForm.bindFromRequest
        boundForm.hasErrors must beTrue
      }

      "display global errors user template" in new WithApplication {
        val controller = app.injector.instanceOf[controllers.Application]
        val userForm   = controller.userFormConstraintsAdHoc

        implicit val request = FakeRequest().withFormUrlEncodedBody("name" -> "Johnny Utah", "age" -> "25")

        val boundForm = userForm.bindFromRequest
        boundForm.hasGlobalErrors must beTrue

        val html = views.html.user(boundForm)
        html.body must contain("Failed form constraints!")
      }

      "map single values" in new WithApplication {

        //#form-single-value
        val singleForm = Form(
          single(
            "email" -> email
          )
        )

        val emailValue = singleForm.bind(Map("email" -> "bob@example.com")).get
        //#form-single-value
        emailValue must beEqualTo("bob@example.com")
      }

      "fill selects with options and set their defaults" in new WithApplication {
        val controller = app.injector.instanceOf[controllers.Application]
        val boundForm  = controller.filledAddressSelectForm
        val html       = views.html.select(boundForm)
        html.body must contain("option value=\"London\" selected")
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

// #userData-custom-datatype
    case class UserCustomData(name: String, website: java.net.URL)
// #userData-custom-datatype

  }

  package views.html.contact {

// #contact-define
    case class Contact(
        firstname: String,
        lastname: String,
        company: Option[String],
        informations: Seq[ContactInformation]
    )

    object Contact {
      def save(contact: Contact): Int = 99
    }

    case class ContactInformation(label: String, email: Option[String], phones: List[String])
// #contact-define

  }

  package controllers {

    import views.html._
    import views.html.contact._

    class Application @Inject()(components: ControllerComponents)
        extends AbstractController(components)
        with I18nSupport {

      //#userForm-define
      val userForm = Form(
        mapping(
          "name" -> text,
          "age"  -> number
        )(UserData.apply)(UserData.unapply)
      )
      //#userForm-define

      def home(id: Int = 0) = Action {
        Ok("Welcome!")
      }

      // #form-render
      def index = Action { implicit request =>
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
            val id      = models.User.create(newUser)
            Redirect(routes.Application.home(id))
          }
        )
      //#userForm-handling-failure

      }

      // #form-bodyparser
      val userPost = Action(parse.form(userForm)) { implicit request =>
        val userData = request.body
        val newUser  = models.User(userData.name, userData.age)
        val id       = models.User.create(newUser)
        Redirect(routes.Application.home(id))
      }
      // #form-bodyparser

      // #form-bodyparser-errors
      val userPostWithErrors = Action(
        parse.form(
          userForm,
          onErrors = (formWithErrors: Form[UserData]) => {
            implicit val messages = messagesApi.preferred(Seq(Lang.defaultLang))
            BadRequest(views.html.user(formWithErrors))
          }
        )
      ) { implicit request =>
        val userData = request.body
        val newUser  = models.User(userData.name, userData.age)
        val id       = models.User.create(newUser)
        Redirect(routes.Application.home(id))
      }
      // #form-bodyparser-errors

      def submit = Action { implicit request =>
        BadRequest("Not used")
      }

      val userFormName = {
        //#userForm-get
        val anyData        = Map("name" -> "bob", "age" -> "18")
        val user: UserData = userForm.bind(anyData).get
        //#userForm-get

        //#userForm-filled
        val filledForm = userForm.fill(UserData("Bob", 18))
        //#userForm-filled

        user.name
      }

      //#addressSelectForm-constraint
      val addressSelectForm: Form[AddressData] = Form(
        mapping(
          "street" -> text,
          "city"   -> text
        )(AddressData.apply)(AddressData.unapply)
      )
      //#addressSelectForm-constraint

      val filledAddressSelectForm = {
        //#addressSelectForm-filled
        val selectedFormValues = AddressData(street = "Main St", city = "London")
        val filledForm         = addressSelectForm.fill(selectedFormValues)
        //#addressSelectForm-filled
        filledForm
      }

      //#userForm-verify
      val userFormVerify = Form(
        mapping(
          "name"   -> text,
          "age"    -> number,
          "accept" -> checked("Please accept the terms and conditions")
        )((name, age, _) => UserData(name, age))((user: UserData) => Some(user.name, user.age, false))
      )
      //#userForm-verify

      val userFormVerifyName = {
        val anyData        = Map("name" -> "bob", "age" -> "18", "accept" -> "true")
        val user: UserData = userFormVerify.bind(anyData).get
        user.name
      }

      //#userForm-constraints
      val userFormConstraints = Form(
        mapping(
          "name" -> text.verifying(nonEmpty),
          "age"  -> number.verifying(min(0), max(100))
        )(UserData.apply)(UserData.unapply)
      )
      //#userForm-constraints

      val userFormConstraintsName = {
        val anyData        = Map("name" -> "bob", "age" -> "18", "accept" -> "true")
        val user: UserData = userFormConstraints.bind(anyData).get
        user.name
      }

      //#userForm-constraints-2
      val userFormConstraints2 = Form(
        mapping(
          "name" -> nonEmptyText,
          "age"  -> number(min = 0, max = 100)
        )(UserData.apply)(UserData.unapply)
      )
      //#userForm-constraints-2

      val userFormConstraints2Name = {
        val anyData        = Map("name" -> "bob", "age" -> "18", "accept" -> "true")
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
          "age"  -> number
        )(UserData.apply)(UserData.unapply).verifying(
          "Failed form constraints!",
          fields =>
            fields match {
              case userData => validate(userData.name, userData.age).isDefined
            }
        )
      )
      //#userForm-constraints-ad-hoc

      val userFormConstraintsAdhocName = {
        val anyData  = Map("name" -> "bob", "age" -> "18")
        val formData = userFormConstraintsAdHoc.bind(anyData).get
        formData.name
      }

      //#userForm-nested
      val userFormNested: Form[UserAddressData] = Form(
        mapping(
          "name" -> text,
          "address" -> mapping(
            "street" -> text,
            "city"   -> text
          )(AddressData.apply)(AddressData.unapply)
        )(UserAddressData.apply)(UserAddressData.unapply)
      )
      //#userForm-nested

      val userFormNestedCity = {
        val anyData = Map("name" -> "bob@gmail.com", "address.street" -> "Century Road.", "address.city" -> "Shanghai")
        val user    = userFormNested.bind(anyData).get
        user.address.city
      }

      //#userForm-repeated
      val userFormRepeated = Form(
        mapping(
          "name"   -> text,
          "emails" -> list(email)
        )(UserListData.apply)(UserListData.unapply)
      )
      //#userForm-repeated

      val userFormRepeatedEmails = {
        val anyData = Map("name" -> "bob", "emails[0]" -> "benewu@gmail.com", "emails[1]" -> "bob@gmail.com")
        val user    = userFormRepeated.bind(anyData).get

        user.emails
      }

      //#userForm-optional
      val userFormOptional = Form(
        mapping(
          "name"  -> text,
          "email" -> optional(email)
        )(UserOptionalData.apply)(UserOptionalData.unapply)
      )
      //#userForm-optional

      val userFormOptionalEmail = {
        val anyData = Map("name" -> "bob")
        val user    = userFormOptional.bind(anyData).get

        user.email
      }

      //#userForm-default
      Form(
        mapping(
          "name" -> default(text, "Bob"),
          "age"  -> default(number, 18)
        )(UserData.apply)(UserData.unapply)
      )
      //#userForm-default

      case class UserStaticData(id: Long, name: String, email: Option[String])

      //#userForm-static-value
      val userFormStatic = Form(
        mapping(
          "id"    -> ignored(23L),
          "name"  -> text,
          "email" -> optional(email)
        )(UserStaticData.apply)(UserStaticData.unapply)
      )
      //#userForm-static-value

      //#userForm-custom-datatype
      val userFormCustom = Form(
        mapping(
          "name"    -> text,
          "website" -> of[URL]
        )(UserCustomData.apply)(UserCustomData.unapply)
      )
      //#userForm-custom-datatype

      //#userForm-custom-formatter
      import play.api.data.format.Formatter
      import play.api.data.format.Formats._
      implicit object UrlFormatter extends Formatter[URL] {
        override val format                                       = Some(("format.url", Nil))
        override def bind(key: String, data: Map[String, String]) = parsing(new URL(_), "error.url", Nil)(key, data)
        override def unbind(key: String, value: URL)              = Map(key -> value.toString)
      }
      //#userForm-custom-formatter

      val userFormStaticId = {
        val anyData = Map("id" -> "1", "name" -> "bob")
        val user    = userFormStatic.bind(anyData).get

        user.id
      }

      // #userForm-tuple
      val userFormTuple = Form(
        tuple(
          "name" -> text,
          "age"  -> number
        ) // tuples come with built-in apply/unapply
      )
      // #userForm-tuple

      val userFormTupleName = {
        // #userForm-tuple-example
        val anyData     = Map("name" -> "bob", "age" -> "25")
        val (name, age) = userFormTuple.bind(anyData).get
        // #userForm-tuple-example
        name
      }

      // #contact-form
      val contactForm: Form[Contact] = Form(
        // Defines a mapping that will handle Contact values
        mapping(
          "firstname" -> nonEmptyText,
          "lastname"  -> nonEmptyText,
          "company"   -> optional(text),
          // Defines a repeated mapping
          "informations" -> seq(
            mapping(
              "label" -> nonEmptyText,
              "email" -> optional(email),
              "phones" -> list(
                text.verifying(pattern("""[0-9.+]+""".r, error = "A valid phone number is required"))
              )
            )(ContactInformation.apply)(ContactInformation.unapply)
          )
        )(Contact.apply)(Contact.unapply)
      )
      // #contact-form

      // #contact-edit
      def editContact = Action { implicit request =>
        val existingContact = Contact(
          "Fake",
          "Contact",
          Some("Fake company"),
          informations = List(
            ContactInformation(
              "Personal",
              Some("fakecontact@gmail.com"),
              List("01.23.45.67.89", "98.76.54.32.10")
            ),
            ContactInformation(
              "Professional",
              Some("fakecontact@company.com"),
              List("01.23.45.67.89")
            ),
            ContactInformation(
              "Previous",
              Some("fakecontact@oldcompany.com"),
              List()
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

//#messages-controller
    class MessagesController @Inject()(cc: ControllerComponents)
        extends AbstractController(cc)
        with play.api.i18n.I18nSupport {

      import play.api.data.Form
      import play.api.data.Forms._

      val userForm = Form(
        mapping(
          "name" -> text,
          "age"  -> number
        )(views.html.UserData.apply)(views.html.UserData.unapply)
      )

      def index = Action { implicit request =>
        Ok(views.html.user(userForm))
      }
    }
//#messages-controller

//#messages-request-controller
// Example form injecting a messagesAction
    class FormController @Inject()(messagesAction: MessagesActionBuilder, components: ControllerComponents)
        extends AbstractController(components) {

      import play.api.data.Form
      import play.api.data.Forms._

      val userForm = Form(
        mapping(
          "name" -> text,
          "age"  -> number
        )(views.html.UserData.apply)(views.html.UserData.unapply)
      )

      def index = messagesAction { implicit request: MessagesRequest[AnyContent] =>
        Ok(views.html.messages(userForm))
      }

      def post() = TODO
    }
//#messages-request-controller

    //#messages-abstract-controller
    // Form with Action extending MessagesAbstractController
    class MessagesFormController @Inject()(components: MessagesControllerComponents)
        extends MessagesAbstractController(components) {

      import play.api.data.Form
      import play.api.data.Forms._

      val userForm = Form(
        mapping(
          "name" -> text,
          "age"  -> number
        )(views.html.UserData.apply)(views.html.UserData.unapply)
      )

      def index = Action { implicit request: MessagesRequest[AnyContent] =>
        Ok(views.html.messages(userForm))
      }

      def post() = TODO
    }
    //#messages-abstract-controller

  }

}
