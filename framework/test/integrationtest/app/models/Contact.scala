package models
import play.api.data._
import play.api.data.Forms._

case class Contact(name: String, gender: String)

object Contacts {
    val form: Form[Contact] = Form(
      mapping(
        "name" -> text,
        "gender" -> text
      )(Contact.apply)(Contact.unapply)
    )
}
