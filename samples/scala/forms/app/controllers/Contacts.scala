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

  implicit def contactWrite = {
    import play.api.data.mapping.Write._
    import play.api.libs.functional.syntax.unlift
    val contactInformation =
      ((Path \ "label").write(string) ~
       (Path \ "email").write(option(string)) ~
       (Path \ "phones").write(seq(string))) (unlift(ContactInformation.unapply _))

      ((Path \ "firstname").write(string) ~
       (Path \ "lastname").write(string) ~
       (Path \ "company").write(option(string)) ~
       (Path \ "informations").write(seq(contactInformation))) (unlift(Contact.unapply _))
    }

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
    Ok(html.contact.form(Form.fill(existingContact)))
  }

  /**
   * Handle form submission.
   */
  def submit = Action(parse.urlFormEncoded) { implicit request =>
    val r = contactValidation.validate(request.body)
    r match {
      case Failure(_) => BadRequest(html.contact.form(Form(request.body, r)))
      case Success(contact) => Ok(html.contact.summary(contact))
    }
  }

}