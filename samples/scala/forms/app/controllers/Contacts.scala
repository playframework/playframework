package controllers

import play.api._
import play.api.mvc._
import play.api.data.mapping._
import play.api.libs.functional.syntax._

import views._
import models._

object Contacts extends Controller {

  implicit val infoValidation = From[UrlFormEncoded] { __ =>
    import Rules._
    ((__ \ "label").read(notEmpty) ~
     (__ \ "email").read(option(email)) ~
     (__ \ "phones").read(list(notEmpty))) (ContactInformation.apply _)
  }

  implicit val contactValidation = From[UrlFormEncoded] { __ =>
    import Rules._
    ((__ \ "firstname").read(notEmpty) ~
     (__ \ "lastname").read(notEmpty) ~
     (__ \ "company").read[Option[String]] ~
     (__ \ "informations").read(seq(infoValidation))) (Contact.apply _)
  }

  implicit val contactInformationW = To[UrlFormEncoded] { __ =>
    import Writes._
    ((__ \ "label").write[String] ~
     (__ \ "email").write[Option[String]] ~
     (__ \ "phones").write[Seq[String]]) (unlift(ContactInformation.unapply _))
  }
  implicit def contactW = To[UrlFormEncoded] { __ =>
    import Writes._
    ((__ \ "firstname").write[String] ~
     (__ \ "lastname").write[String] ~
     (__ \ "company").write[Option[String]] ~
     (__ \ "informations").write[Seq[ContactInformation]]) (unlift(Contact.unapply _))
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