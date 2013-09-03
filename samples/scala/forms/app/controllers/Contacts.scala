package controllers

import play.api._
import play.api.mvc._
import play.api.data.mapping._
import play.api.data.mapping.Rules._
import play.api.libs.functional.syntax._

import views._
import models._

object Contacts extends Controller {

  val nonEmptyText = string compose notEmpty
  val infoValidation =
   ((Path \ "label").read(nonEmptyText) ~
    (Path \ "email").read(option(string compose email)) ~
    (Path \ "phones").read(list(nonEmptyText))) (ContactInformation.apply _)

  val contactValidation =
   ((Path \ "firstname").read(nonEmptyText) ~
    (Path \ "lastname").read(nonEmptyText) ~
    (Path \ "company").read(option(string)) ~
    (Path \ "informations").read(seq(infoValidation))) (Contact.apply _)

  // implicit def contactWrite = {
  //  import play.api.libs.functional.syntax.unlift
  //  implicit val infoWrites =
  //   ((Path \ "label").write[String] ~
  //    (Path \ "email").write[Option[String]] ~
  //    (Path \ "phones").write[Seq[String]]) (unlift(ContactInformation.unapply _))

  //   ((Path \ "firstname").write[String] ~
  //    (Path \ "lastname").write[String] ~
  //    (Path \ "company").write[Option[String]] ~
  //    (Path \ "informations").write[Seq[ContactInformation]]) (unlift(Contact.unapply _))
  // }

  /**
   * Display an empty form.
   */
  def form = Action {
    Ok(html.contact.form(Form[Contact]()));
  }

  /**
   * Display a form pre-filled with an existing Contact.
   */
  def editForm = Action {
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
    //Ok(html.contact.form(Form.fill(existingContact)))
    Ok(html.contact.form(Form[Contact]()))
  }

  /**
   * Handle form submission.
   */
  def submit = Action(parse.urlFormEncoded) { implicit request =>
    val r = contactValidation.validate(request.body)
    r match {
      case Failure(_) => BadRequest(html.contact.form(Form(request.body, r)))
      case Success(_) => Ok(html.contact.form(Form(request.body, r)))
    }
  }

}