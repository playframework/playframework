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

    "support primitives types" in {

      "Int" in {
        To[M] { __ => (__ \ "n").write[Int] }.writes(4) mustEqual(Map("n" -> Seq("4")))
        To[M] { __ => (__ \ "n" \ "o").write[Int] }.writes(4) mustEqual(Map("n.o" -> Seq("4")))
        To[M] { __ => (__ \ "n" \ "o" \ "p").write[Int] }.writes(4) mustEqual(Map("n.o.p" -> Seq("4")))
      }

      "Short" in {
        To[M] { __ => (__ \ "n").write[Short] }.writes(4) mustEqual(Map("n" -> Seq("4")))
        To[M] { __ => (__ \ "n" \ "o").write[Short] }.writes(4) mustEqual(Map("n.o" -> Seq("4")))
        To[M] { __ => (__ \ "n" \ "o" \ "p").write[Short] }.writes(4) mustEqual(Map("n.o.p" -> Seq("4")))
      }

      "Long" in {
        To[M] { __ => (__ \ "n").write[Long] }.writes(4) mustEqual(Map("n" -> Seq("4")))
        To[M] { __ => (__ \ "n" \ "o").write[Long] }.writes(4) mustEqual(Map("n.o" -> Seq("4")))
        To[M] { __ => (__ \ "n" \ "o" \ "p").write[Long] }.writes(4) mustEqual(Map("n.o.p" -> Seq("4")))
      }

      "Float" in {
        To[M] { __ => (__ \ "n").write[Float] }.writes(4) mustEqual(Map("n" -> Seq("4.0")))
        To[M] { __ => (__ \ "n" \ "o").write[Float] }.writes(4.8F) mustEqual(Map("n.o" -> Seq("4.8")))
        To[M] { __ => (__ \ "n" \ "o" \ "p").write[Float] }.writes(4.8F) mustEqual(Map("n.o.p" -> Seq("4.8")))
      }

      "Double" in {
        To[M] { __ => (__ \ "n").write[Double] }.writes(4) mustEqual(Map("n" -> Seq("4.0")))
        To[M] { __ => (__ \ "n" \ "o").write[Double] }.writes(4.8D) mustEqual(Map("n.o" -> Seq("4.8")))
        To[M] { __ => (__ \ "n" \ "o" \ "p").write[Double] }.writes(4.8D) mustEqual(Map("n.o.p" -> Seq("4.8")))
      }

      "java BigDecimal" in {
        import java.math.{ BigDecimal => jBigDecimal }
        To[M] { __ => (__ \ "n").write[jBigDecimal] }.writes(new jBigDecimal("4.0")) mustEqual(Map("n" -> Seq("4.0")))
        To[M] { __ => (__ \ "n" \ "o").write[jBigDecimal] }.writes(new jBigDecimal("4.8")) mustEqual(Map("n.o" -> Seq("4.8")))
        To[M] { __ => (__ \ "n" \ "o" \ "p").write[jBigDecimal] }.writes(new jBigDecimal("4.8")) mustEqual(Map("n.o.p" -> Seq("4.8")))
      }

      "scala BigDecimal" in {
        To[M] { __ => (__ \ "n").write[BigDecimal] }.writes(BigDecimal("4.0")) mustEqual(Map("n" -> Seq("4.0")))
        To[M] { __ => (__ \ "n" \ "o").write[BigDecimal] }.writes(BigDecimal("4.8")) mustEqual(Map("n.o" -> Seq("4.8")))
        To[M] { __ => (__ \ "n" \ "o" \ "p").write[BigDecimal] }.writes(BigDecimal("4.8")) mustEqual(Map("n.o.p" -> Seq("4.8")))
      }

      "date" in { skipped }
      "joda date" in { skipped }
      "joda local data" in { skipped }
      "sql date" in { skipped }

      "Boolean" in {
        To[M] { __ => (__ \ "n").write[Boolean] }.writes(true) mustEqual(Map("n" -> Seq("true")))
        To[M] { __ => (__ \ "n" \ "o").write[Boolean] }.writes(false) mustEqual(Map("n.o" -> Seq("false")))
        To[M] { __ => (__ \ "n" \ "o" \ "p").write[Boolean] }.writes(true) mustEqual(Map("n.o.p" -> Seq("true")))
      }

      "String" in {
        To[M] { __ => (__ \ "n").write[String] }.writes("foo") mustEqual(Map("n" -> Seq("foo")))
        To[M] { __ => (__ \ "n" \ "o").write[String] }.writes("foo") mustEqual(Map("n.o" -> Seq("foo")))
        To[M] { __ => (__ \ "n" \ "o" \ "p").write[String] }.writes("foo") mustEqual(Map("n.o.p" -> Seq("foo")))
      }

      "Option" in {
        To[M] { __ => (__ \ "n").write[Option[String]] }.writes(Some("foo")) mustEqual(Map("n" -> Seq("foo")))
        To[M] { __ => (__ \ "n" \ "o").write[Option[String]] }.writes(Some("foo")) mustEqual(Map("n.o" -> Seq("foo")))
        To[M] { __ => (__ \ "n" \ "o" \ "p").write[Option[String]] }.writes(Some("foo")) mustEqual(Map("n.o.p" -> Seq("foo")))

        To[M] { __ => (__ \ "n").write[Option[String]] }.writes(None) mustEqual(Map.empty)
        To[M] { __ => (__ \ "n" \ "o").write[Option[String]] }.writes(None) mustEqual(Map.empty)
        To[M] { __ => (__ \ "n" \ "o" \ "p").write[Option[String]] }.writes(None) mustEqual(Map.empty)
      }

      "Map[String, Seq[V]]" in {
        import Rules.{ map => mm }

        To[M] { __ => (__ \ "n").write[Map[String, Seq[String]]] }.writes(Map("foo" -> Seq("bar"))) mustEqual((Map("n.foo" -> Seq("bar"))))
        To[M] { __ => (__ \ "n").write[Map[String, Seq[Int]]] }.writes(Map("foo" -> Seq(4))) mustEqual((Map("n.foo" -> Seq("4"))))
        To[M] { __ => (__ \ "n" \ "o").write[Map[String, Seq[Int]]] }.writes(Map("foo" -> Seq(4))) mustEqual((Map("n.o.foo" -> Seq("4"))))
        To[M] { __ => (__ \ "n" \ "o").write[Map[String, Int]] }.writes(Map("foo" -> 4)) mustEqual((Map("n.o.foo" -> Seq("4"))))
        To[M] { __ => (__ \ "n" \ "o").write[Map[String, Int]] }.writes(Map.empty) mustEqual(Map.empty)
      }

      "Traversable" in {
        import Rules.{ traversable => tr } // avoid shadowing caused by specs
        To[M] { __ => (__ \ "n").write[Traversable[String]] }.writes(Array("foo", "bar").toTraversable) mustEqual((Map("n[0]" -> Seq("foo"), "n[1]" -> Seq("bar"))))
        To[M] { __ => (__ \ "n" \ "o").write[Traversable[String]] }.writes(Array("foo", "bar").toTraversable) mustEqual((Map("n.o[0]" -> Seq("foo"), "n.o[1]" -> Seq("bar"))))
        To[M] { __ => (__ \ "n" \ "o" \ "p").write[Traversable[String]] }.writes(Array("foo", "bar").toTraversable) mustEqual((Map("n.o.p[0]" -> Seq("foo"), "n.o.p[1]" -> Seq("bar"))))

        To[M] { __ => (__ \ "n").write[Traversable[String]] }.writes(Array().toTraversable) mustEqual(Map.empty)
        To[M] { __ => (__ \ "n" \ "o").write[Traversable[String]] }.writes(Array().toTraversable) mustEqual(Map.empty)
        To[M] { __ => (__ \ "n" \ "o" \ "p").write[Traversable[String]] }.writes(Array().toTraversable) mustEqual(Map.empty)
      }

      "Array" in {
        To[M] { __ => (__ \ "n").write[Array[String]] }.writes(Array("foo", "bar")) mustEqual((Map("n[0]" -> Seq("foo"), "n[1]" -> Seq("bar"))))
        To[M] { __ => (__ \ "n" \ "o").write[Array[String]] }.writes(Array("foo", "bar")) mustEqual((Map("n.o[0]" -> Seq("foo"), "n.o[1]" -> Seq("bar"))))
        To[M] { __ => (__ \ "n" \ "o" \ "p").write[Array[String]] }.writes(Array("foo", "bar")) mustEqual((Map("n.o.p[0]" -> Seq("foo"), "n.o.p[1]" -> Seq("bar"))))

        To[M] { __ => (__ \ "n").write[Array[String]] }.writes(Array()) mustEqual(Map.empty)
        To[M] { __ => (__ \ "n" \ "o").write[Array[String]] }.writes(Array()) mustEqual(Map.empty)
        To[M] { __ => (__ \ "n" \ "o" \ "p").write[Array[String]] }.writes(Array()) mustEqual(Map.empty)
      }

      "Seq" in {
        To[M] { __ => (__ \ "n").write[Seq[String]] }.writes(Seq("foo", "bar")) mustEqual((Map("n[0]" -> Seq("foo"), "n[1]" -> Seq("bar"))))
        To[M] { __ => (__ \ "n" \ "o").write[Seq[String]] }.writes(Seq("foo", "bar")) mustEqual((Map("n.o[0]" -> Seq("foo"), "n.o[1]" -> Seq("bar"))))
        To[M] { __ => (__ \ "n" \ "o" \ "p").write[Seq[String]] }.writes(Seq("foo", "bar")) mustEqual((Map("n.o.p[0]" -> Seq("foo"), "n.o.p[1]" -> Seq("bar"))))

        To[M] { __ => (__ \ "n").write[Seq[String]] }.writes(Nil) mustEqual(Map.empty)
        To[M] { __ => (__ \ "n" \ "o").write[Seq[String]] }.writes(Nil) mustEqual(Map.empty)
        To[M] { __ => (__ \ "n" \ "o" \ "p").write[Seq[String]] }.writes(Nil) mustEqual(Map.empty)
      }
    }

    "format data" in {
      val formatter = Write[Double, String]{ money =>
        import java.text.NumberFormat
        import java.util.Locale
        val f = NumberFormat.getCurrencyInstance(Locale.FRANCE)
        f.format(money)
      }
      val w = (Path \ "foo").write(formatter)
      w.writes(500d) mustEqual(Map("foo" -> List("500,00 €")))

      val w2 = To[M] { __ => (__ \ "foo").write(formatter) }
      w2.writes(500d) mustEqual(Map("foo" -> List("500,00 €")))
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