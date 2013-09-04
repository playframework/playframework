package play.api.data.mapping

import org.specs2.mutable._
import play.api.libs.functional.syntax._

class WritesSpec extends Specification {

  case class Contact(
    firstname: String,
    lastname: String,
    company: Option[String],
    informations: Seq[ContactInformation])

  case class ContactInformation(
    label: String,
    email: Option[String],
    phones: Seq[String])

  val contact = Contact("Julien", "Tournay", None, Seq(
    ContactInformation("Personal", Some("fakecontact@gmail.com"), Seq("01.23.45.67.89", "98.76.54.32.10"))))

  val contactMap = Map(
    "firstname" -> Seq("Julien"),
    "lastname" -> Seq("Tournay"),
    "company" -> Seq(),
    "informations[0].label" -> Seq("Personal"),
    "informations[0].email" -> Seq("fakecontact@gmail.com"),
    "informations[0].phones[0]" -> Seq("01.23.45.67.89"),
    "informations[0].phones[1]" -> Seq("98.76.54.32.10"))

  import Write._

  type M = Map[String, Seq[String]]

  "Writes" should {

    "write string" in {
      val w = (Path \ "label").write(string)
      w.writes("Hello World") mustEqual Map("label" -> Seq("Hello World"))
    }

    "write option" in {
      val w = (Path \ "email").write(option(string))
      w.writes(Some("Hello World")) mustEqual Map("email" -> Seq("Hello World"))
      w.writes(None) mustEqual Map.empty
    }

    "write seq" in {
      val w = (Path \ "phones").write(seq(string))
      w.writes(Seq("01.23.45.67.89", "98.76.54.32.10")) mustEqual Map("phones[0]" -> Seq("01.23.45.67.89"), "phones[1]" -> Seq("98.76.54.32.10"))
      w.writes(Nil) mustEqual Map.empty
    }

    // "write Map" in {
    //   implicit def contactWrite = {

    //     import play.api.libs.functional.syntax.unlift
    //     implicit val contactInformation =
    //       ((Path \ "label").write(string) ~
    //        (Path \ "email").write(option(string)) ~
    //        (Path \ "phones").write(seq(string))) (unlift(ContactInformation.unapply _))

    //     ((Path \ "firstname").write(string) ~
    //      (Path \ "lastname").write(string) ~
    //      (Path \ "company").write(option(string)) ~
    //      (Path \ "informations").write(seq(contactInformation))) (unlift(Contact.unapply _))
    //   }

    //   Writes[Contact, M](contactWrite).writes(contact) mustEqual contactMap
    // }

  }

}