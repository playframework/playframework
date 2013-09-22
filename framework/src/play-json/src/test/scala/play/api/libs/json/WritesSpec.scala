package play.api.libs.json

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

  val contactJson =Json.obj(
    "firstname" -> "Julien",
    "lastname" -> "Tournay",
    "age" -> 27,
    "informations" -> Json.obj(
      "label" -> "Personal",
      "email" -> "fakecontact@gmail.com",
      "phones" -> Seq("01.23.45.67.89", "98.76.54.32.10")))

  import play.api.data.mapping._
  import play.api.data.mapping.json._
  import play.api.data.mapping.json.Writes._

  "Writes" should {

    "write string" in {
      val w = (Path \ "label").write[String, JsValue]
      w.writes("Hello World") mustEqual Json.obj("label" -> "Hello World")
    }

    "write option" in {
      val w = (Path \ "email").write[Option[String], JsValue]
      w.writes(Some("Hello World")) mustEqual Json.obj("email" -> "Hello World")
      w.writes(None) mustEqual Json.obj()
    }

    "write seq" in {
      val w = (Path \ "phones").write[Seq[String], JsValue]
      w.writes(Seq("01.23.45.67.89", "98.76.54.32.10")) mustEqual Json.obj("phones" -> Seq("01.23.45.67.89", "98.76.54.32.10"))
      w.writes(Nil) mustEqual Json.obj("phones" -> Seq[String]())
    }

    "support primitives types" in {

      "Int" in {
        (Path \ "n").write[Int, JsValue].writes(4) mustEqual(Json.obj("n" -> 4))
        (Path \ "n" \ "o").write[Int, JsValue].writes(4) mustEqual(Json.obj("n" -> Json.obj("o"-> 4)))
        (Path \ "n" \ "o" \ "p").write[Int, JsValue].writes(4) mustEqual(Json.obj("n" -> Json.obj("o"-> Json.obj("p"-> 4))))
      }

      "Short" in {
        (Path \ "n").write[Short, JsValue].writes(4) mustEqual(Json.obj("n" -> 4))
        (Path \ "n" \ "o").write[Short, JsValue].writes(4) mustEqual(Json.obj("n" -> Json.obj("o"-> 4)))
        (Path \ "n" \ "o" \ "p").write[Short, JsValue].writes(4) mustEqual(Json.obj("n" -> Json.obj("o"-> Json.obj("p"-> 4))))
      }

      "Long" in {
        (Path \ "n").write[Long, JsValue].writes(4) mustEqual(Json.obj("n" -> 4))
        (Path \ "n" \ "o").write[Long, JsValue].writes(4) mustEqual(Json.obj("n" -> Json.obj("o"-> 4)))
        (Path \ "n" \ "o" \ "p").write[Long, JsValue].writes(4) mustEqual(Json.obj("n" -> Json.obj("o"-> Json.obj("p"-> 4))))
      }

      "Float" in {
        (Path \ "n").write[Float, JsValue].writes(4.8f) mustEqual(Json.obj("n" -> 4.8))
        (Path \ "n" \ "o").write[Float, JsValue].writes(4.8f) mustEqual(Json.obj("n" -> Json.obj("o"-> 4.8)))
        (Path \ "n" \ "o" \ "p").write[Float, JsValue].writes(4.8f) mustEqual(Json.obj("n" -> Json.obj("o"-> Json.obj("p"-> 4.8))))
      }

      "Double" in {
        (Path \ "n").write[Double, JsValue].writes(4d) mustEqual(Json.obj("n" -> 4.0))
        (Path \ "n" \ "o").write[Double, JsValue].writes(4.8d) mustEqual(Json.obj("n" -> Json.obj("o"-> 4.8)))
        (Path \ "n" \ "o" \ "p").write[Double, JsValue].writes(4.8d) mustEqual(Json.obj("n" -> Json.obj("o"-> Json.obj("p"-> 4.8))))
      }

      "java BigDecimal" in {
        import java.math.{ BigDecimal => jBigDecimal }
        (Path \ "n").write[jBigDecimal, JsValue].writes(new jBigDecimal("4.0")) mustEqual(Json.obj("n" -> 4.0))
        (Path \ "n" \ "o").write[jBigDecimal, JsValue].writes(new jBigDecimal("4.8")) mustEqual(Json.obj("n" -> Json.obj("o"-> 4.8)))
        (Path \ "n" \ "o" \ "p").write[jBigDecimal, JsValue].writes(new jBigDecimal("4.8")) mustEqual(Json.obj("n" -> Json.obj("o"-> Json.obj("p"-> 4.8))))
      }

      "scala BigDecimal" in {
        (Path \ "n").write[BigDecimal, JsValue].writes(BigDecimal("4.0")) mustEqual(Json.obj("n" -> 4.0))
        (Path \ "n" \ "o").write[BigDecimal, JsValue].writes(BigDecimal("4.8")) mustEqual(Json.obj("n" -> Json.obj("o"-> 4.8)))
        (Path \ "n" \ "o" \ "p").write[BigDecimal, JsValue].writes(BigDecimal("4.8")) mustEqual(Json.obj("n" -> Json.obj("o"-> Json.obj("p"-> 4.8))))
      }

    //   "date" in { skipped }
    //   "joda date" in { skipped }
    //   "joda local data" in { skipped }
    //   "sql date" in { skipped }

    //   "Boolean" in {
    //     To[M] { __ => (__ \ "n").write[Boolean] }.writes(true) mustEqual(Map("n" -> Seq("true")))
    //     To[M] { __ => (__ \ "n" \ "o").write[Boolean] }.writes(false) mustEqual(Map("n.o" -> Seq("false")))
    //     To[M] { __ => (__ \ "n" \ "o" \ "p").write[Boolean] }.writes(true) mustEqual(Map("n.o.p" -> Seq("true")))
    //   }

      "String" in {
        (Path \ "n").write[String, JsValue].writes("foo") mustEqual(Json.obj("n" -> "foo"))
        (Path \ "n" \ "o").write[String, JsValue].writes("foo") mustEqual(Json.obj("n" -> Json.obj("o"-> "foo")))
        (Path \ "n" \ "o" \ "p").write[String, JsValue].writes("foo") mustEqual(Json.obj("n" -> Json.obj("o"-> Json.obj("p"-> "foo"))))
      }

    //   "Option" in {
    //     To[M] { __ => (__ \ "n").write[Option[String]] }.writes(Some("foo")) mustEqual(Map("n" -> Seq("foo")))
    //     To[M] { __ => (__ \ "n" \ "o").write[Option[String]] }.writes(Some("foo")) mustEqual(Map("n.o" -> Seq("foo")))
    //     To[M] { __ => (__ \ "n" \ "o" \ "p").write[Option[String]] }.writes(Some("foo")) mustEqual(Map("n.o.p" -> Seq("foo")))

    //     To[M] { __ => (__ \ "n").write[Option[String]] }.writes(None) mustEqual(Map.empty)
    //     To[M] { __ => (__ \ "n" \ "o").write[Option[String]] }.writes(None) mustEqual(Map.empty)
    //     To[M] { __ => (__ \ "n" \ "o" \ "p").write[Option[String]] }.writes(None) mustEqual(Map.empty)
    //   }

    //   "Map[String, Seq[V]]" in {
    //     import Rules.{ map => mm }

    //     To[M] { __ => (__ \ "n").write[Map[String, Seq[String]]] }.writes(Map("foo" -> Seq("bar"))) mustEqual((Map("n.foo" -> Seq("bar"))))
    //     To[M] { __ => (__ \ "n").write[Map[String, Seq[Int]]] }.writes(Map("foo" -> Seq(4))) mustEqual((Map("n.foo" -> Seq("4"))))
    //     To[M] { __ => (__ \ "n" \ "o").write[Map[String, Seq[Int]]] }.writes(Map("foo" -> Seq(4))) mustEqual((Map("n.o.foo" -> Seq("4"))))
    //     To[M] { __ => (__ \ "n" \ "o").write[Map[String, Int]] }.writes(Map("foo" -> 4)) mustEqual((Map("n.o.foo" -> Seq("4"))))
    //     To[M] { __ => (__ \ "n" \ "o").write[Map[String, Int]] }.writes(Map.empty) mustEqual(Map.empty)
    //   }

    //   "Traversable" in {
    //     import Rules.{ traversable => tr } // avoid shadowing caused by specs
    //     To[M] { __ => (__ \ "n").write[Traversable[String]] }.writes(Array("foo", "bar").toTraversable) mustEqual((Map("n[0]" -> Seq("foo"), "n[1]" -> Seq("bar"))))
    //     To[M] { __ => (__ \ "n" \ "o").write[Traversable[String]] }.writes(Array("foo", "bar").toTraversable) mustEqual((Map("n.o[0]" -> Seq("foo"), "n.o[1]" -> Seq("bar"))))
    //     To[M] { __ => (__ \ "n" \ "o" \ "p").write[Traversable[String]] }.writes(Array("foo", "bar").toTraversable) mustEqual((Map("n.o.p[0]" -> Seq("foo"), "n.o.p[1]" -> Seq("bar"))))

    //     To[M] { __ => (__ \ "n").write[Traversable[String]] }.writes(Array().toTraversable) mustEqual(Map.empty)
    //     To[M] { __ => (__ \ "n" \ "o").write[Traversable[String]] }.writes(Array().toTraversable) mustEqual(Map.empty)
    //     To[M] { __ => (__ \ "n" \ "o" \ "p").write[Traversable[String]] }.writes(Array().toTraversable) mustEqual(Map.empty)
    //   }

    //   "Array" in {
    //     To[M] { __ => (__ \ "n").write[Array[String]] }.writes(Array("foo", "bar")) mustEqual((Map("n[0]" -> Seq("foo"), "n[1]" -> Seq("bar"))))
    //     To[M] { __ => (__ \ "n" \ "o").write[Array[String]] }.writes(Array("foo", "bar")) mustEqual((Map("n.o[0]" -> Seq("foo"), "n.o[1]" -> Seq("bar"))))
    //     To[M] { __ => (__ \ "n" \ "o" \ "p").write[Array[String]] }.writes(Array("foo", "bar")) mustEqual((Map("n.o.p[0]" -> Seq("foo"), "n.o.p[1]" -> Seq("bar"))))

    //     To[M] { __ => (__ \ "n").write[Array[String]] }.writes(Array()) mustEqual(Map.empty)
    //     To[M] { __ => (__ \ "n" \ "o").write[Array[String]] }.writes(Array()) mustEqual(Map.empty)
    //     To[M] { __ => (__ \ "n" \ "o" \ "p").write[Array[String]] }.writes(Array()) mustEqual(Map.empty)
    //   }

    //   "Seq" in {
    //     To[M] { __ => (__ \ "n").write[Seq[String]] }.writes(Seq("foo", "bar")) mustEqual((Map("n[0]" -> Seq("foo"), "n[1]" -> Seq("bar"))))
    //     To[M] { __ => (__ \ "n" \ "o").write[Seq[String]] }.writes(Seq("foo", "bar")) mustEqual((Map("n.o[0]" -> Seq("foo"), "n.o[1]" -> Seq("bar"))))
    //     To[M] { __ => (__ \ "n" \ "o" \ "p").write[Seq[String]] }.writes(Seq("foo", "bar")) mustEqual((Map("n.o.p[0]" -> Seq("foo"), "n.o.p[1]" -> Seq("bar"))))

    //     To[M] { __ => (__ \ "n").write[Seq[String]] }.writes(Nil) mustEqual(Map.empty)
    //     To[M] { __ => (__ \ "n" \ "o").write[Seq[String]] }.writes(Nil) mustEqual(Map.empty)
    //     To[M] { __ => (__ \ "n" \ "o" \ "p").write[Seq[String]] }.writes(Nil) mustEqual(Map.empty)
    //   }
    }

    // "format data" in {
    //   val formatter = Write[Double, String]{ money =>
    //     import java.text.NumberFormat
    //     import java.util.Locale
    //     val f = NumberFormat.getCurrencyInstance(Locale.FRANCE)
    //     f.format(money)
    //   }
    //   val w = (Path \ "foo").write(formatter)
    //   w.writes(500d) mustEqual(Map("foo" -> List("500,00 €")))

    //   val w2 = To[M] { __ => (__ \ "foo").write(formatter) }
    //   w2.writes(500d) mustEqual(Map("foo" -> List("500,00 €")))
    // }

    // "compose" in {
    //   val w = To[M] { __ =>
    //     ((__ \ "email").write[Option[String]] ~
    //      (__ \ "phones").write[Seq[String]]).tupled
    //   }

    //   val v =  Some("jto@foobar.com") -> Seq("01.23.45.67.89", "98.76.54.32.10")

    //   w.writes(v) mustEqual Map("email" -> Seq("jto@foobar.com"), "phones[0]" -> Seq("01.23.45.67.89"), "phones[1]" -> Seq("98.76.54.32.10"))
    //   w.writes(Some("jto@foobar.com") -> Nil) mustEqual Map("email" -> Seq("jto@foobar.com"))
    //   w.writes(None -> Nil) mustEqual Map.empty
    // }

    // "write Map" in {
    //   def contactWrite = {
    //     import play.api.libs.functional.syntax.unlift
    //     implicit val contactInformation = To[M] { __ =>
    //       ((__ \ "label").write[String] ~
    //        (__ \ "email").write[Option[String]] ~
    //        (__ \ "phones").write[Seq[String]]) (unlift(ContactInformation.unapply _))
    //     }

    //     To[M] { __ =>
    //       ((__ \ "firstname").write[String] ~
    //        (__ \ "lastname").write[String] ~
    //        (__ \ "company").write[Option[String]] ~
    //        (__ \ "informations").write[Seq[ContactInformation]]) (unlift(Contact.unapply _))
    //     }
    //   }

    //   contactWrite.writes(contact) mustEqual contactMap
    // }

  }

}