package play.api.data.mapping

import org.specs2.mutable._
import play.api.libs.functional.syntax._
import scala.language.reflectiveCalls

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
    "informations[0].label" -> Seq("Personal"),
    "informations[0].email" -> Seq("fakecontact@gmail.com"),
    "informations[0].phones[0]" -> Seq("01.23.45.67.89"),
    "informations[0].phones[1]" -> Seq("98.76.54.32.10"))

  import Write._
  import PM._


  "Writes" should {

    "write string" in {
      val w = (Path \ "label").write[String, M]
      w.writes("Hello World") mustEqual Map("label" -> Seq("Hello World"))
    }

    "write option" in {
      val w = (Path \ "email").write[Option[String], M]
      w.writes(Some("Hello World")) mustEqual Map("email" -> Seq("Hello World"))
      w.writes(None) mustEqual Map.empty
    }

    "write seq" in {
      val w = (Path \ "phones").write[Seq[String], M]
      w.writes(Seq("01.23.45.67.89", "98.76.54.32.10")) mustEqual Map("phones[0]" -> Seq("01.23.45.67.89"), "phones[1]" -> Seq("98.76.54.32.10"))
      w.writes(Nil) mustEqual Map.empty
    }

    "compose" in {
      val w = To[M] { __ =>
        ((__ \ "email").write[Option[String]] ~
         (__ \ "phones").write[Seq[String]]).tupled
      }

      val v =  Some("jto@foobar.com") -> Seq("01.23.45.67.89", "98.76.54.32.10")

      w.writes(v) mustEqual Map("email" -> Seq("jto@foobar.com"), "phones[0]" -> Seq("01.23.45.67.89"), "phones[1]" -> Seq("98.76.54.32.10"))
      w.writes(Some("jto@foobar.com") -> Nil) mustEqual Map("email" -> Seq("jto@foobar.com"))
      w.writes(None -> Nil) mustEqual Map.empty
    }

    "write Map" in {
      def contactWrite = {
        import play.api.libs.functional.syntax.unlift
        implicit val contactInformation = To[M] { __ =>
          ((__ \ "label").write[String] ~
           (__ \ "email").write[Option[String]] ~
           (__ \ "phones").write[Seq[String]]) (unlift(ContactInformation.unapply _))
        }

        To[M] { __ =>
          ((__ \ "firstname").write[String] ~
           (__ \ "lastname").write[String] ~
           (__ \ "company").write[Option[String]] ~
           (__ \ "informations").write[Seq[ContactInformation]]) (unlift(Contact.unapply _))
        }
      }

      contactWrite.writes(contact) mustEqual contactMap
    }

  }

}