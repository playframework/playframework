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

  val contactJson = Json.obj(
    "firstname" -> "Julien",
    "lastname" -> "Tournay",
    "informations" -> Seq(Json.obj(
      "label" -> "Personal",
      "email" -> "fakecontact@gmail.com",
      "phones" -> Seq("01.23.45.67.89", "98.76.54.32.10"))))

  import play.api.data.mapping._
  import play.api.data.mapping.json._
  import play.api.data.mapping.json.Writes._

  "Writes" should {

    "write string" in {
      val w = (Path \ "label").write[String, JsObject]
      w.writes("Hello World") mustEqual Json.obj("label" -> "Hello World")
    }

    "ignore values" in {
      (Path \ "n").write(ignored("foo")).writes("test") mustEqual Json.obj("n" -> "foo")
      (Path \ "n").write(ignored(42)).writes(0) mustEqual Json.obj("n" -> 42)
    }

    "write option" in {
      val w = (Path \ "email").write[Option[String], JsObject]
      w.writes(Some("Hello World")) mustEqual Json.obj("email" -> "Hello World")
      w.writes(None) mustEqual Json.obj()

      (Path \ "n").write(option(anyval[Int])).writes(Some(5)) mustEqual Json.obj("n" -> 5)
      (Path \ "n").write(option(anyval[Int])).writes(None) mustEqual Json.obj()
    }

    "write seq" in {
      val w = (Path \ "phones").write[Seq[String], JsObject]
      w.writes(Seq("01.23.45.67.89", "98.76.54.32.10")) mustEqual Json.obj("phones" -> Seq("01.23.45.67.89", "98.76.54.32.10"))
      w.writes(Nil) mustEqual Json.obj("phones" -> Seq[String]())
    }

    "support primitives types" in {

      "Int" in {
        (Path \ "n").write[Int, JsObject].writes(4) mustEqual(Json.obj("n" -> 4))
        (Path \ "n" \ "o").write[Int, JsObject].writes(4) mustEqual(Json.obj("n" -> Json.obj("o"-> 4)))
        (Path \ "n" \ "o" \ "p").write[Int, JsObject].writes(4) mustEqual(Json.obj("n" -> Json.obj("o"-> Json.obj("p"-> 4))))
      }

      "Short" in {
        (Path \ "n").write[Short, JsObject].writes(4) mustEqual(Json.obj("n" -> 4))
        (Path \ "n" \ "o").write[Short, JsObject].writes(4) mustEqual(Json.obj("n" -> Json.obj("o"-> 4)))
        (Path \ "n" \ "o" \ "p").write[Short, JsObject].writes(4) mustEqual(Json.obj("n" -> Json.obj("o"-> Json.obj("p"-> 4))))
      }

      "Long" in {
        (Path \ "n").write[Long, JsObject].writes(4) mustEqual(Json.obj("n" -> 4))
        (Path \ "n" \ "o").write[Long, JsObject].writes(4) mustEqual(Json.obj("n" -> Json.obj("o"-> 4)))
        (Path \ "n" \ "o" \ "p").write[Long, JsObject].writes(4) mustEqual(Json.obj("n" -> Json.obj("o"-> Json.obj("p"-> 4))))
      }

      "Float" in {
        (Path \ "n").write[Float, JsObject].writes(4.8f) mustEqual(Json.obj("n" -> 4.8))
        (Path \ "n" \ "o").write[Float, JsObject].writes(4.8f) mustEqual(Json.obj("n" -> Json.obj("o"-> 4.8)))
        (Path \ "n" \ "o" \ "p").write[Float, JsObject].writes(4.8f) mustEqual(Json.obj("n" -> Json.obj("o"-> Json.obj("p"-> 4.8))))
      }

      "Double" in {
        (Path \ "n").write[Double, JsObject].writes(4d) mustEqual(Json.obj("n" -> 4.0))
        (Path \ "n" \ "o").write[Double, JsObject].writes(4.8d) mustEqual(Json.obj("n" -> Json.obj("o"-> 4.8)))
        (Path \ "n" \ "o" \ "p").write[Double, JsObject].writes(4.8d) mustEqual(Json.obj("n" -> Json.obj("o"-> Json.obj("p"-> 4.8))))
      }

      "java BigDecimal" in {
        import java.math.{ BigDecimal => jBigDecimal }
        (Path \ "n").write[jBigDecimal, JsObject].writes(new jBigDecimal("4.0")) mustEqual(Json.obj("n" -> 4.0))
        (Path \ "n" \ "o").write[jBigDecimal, JsObject].writes(new jBigDecimal("4.8")) mustEqual(Json.obj("n" -> Json.obj("o"-> 4.8)))
        (Path \ "n" \ "o" \ "p").write[jBigDecimal, JsObject].writes(new jBigDecimal("4.8")) mustEqual(Json.obj("n" -> Json.obj("o"-> Json.obj("p"-> 4.8))))
      }

      "scala BigDecimal" in {
        (Path \ "n").write[BigDecimal, JsObject].writes(BigDecimal("4.0")) mustEqual(Json.obj("n" -> 4.0))
        (Path \ "n" \ "o").write[BigDecimal, JsObject].writes(BigDecimal("4.8")) mustEqual(Json.obj("n" -> Json.obj("o"-> 4.8)))
        (Path \ "n" \ "o" \ "p").write[BigDecimal, JsObject].writes(BigDecimal("4.8")) mustEqual(Json.obj("n" -> Json.obj("o"-> Json.obj("p"-> 4.8))))
      }

      "date" in {
        import java.util.Date
        val f = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.FRANCE)
        val d = f.parse("1985-09-10")
        (Path \ "n").write(date).writes(d) mustEqual(Json.obj("n" -> "1985-09-10"))
      }

      "iso date" in {
        import java.util.Date
        val f = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.FRANCE)
        val d = f.parse("1985-09-10")
        (Path \ "n").write(isoDate).writes(d) mustEqual(Json.obj("n" -> "1985-09-10T00:00:00+02:00"))
      }

      "joda" in {
        import org.joda.time.DateTime
        val f = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.FRANCE)
        val dd = f.parse("1985-09-10")
        val jd = new DateTime(dd)

        "date" in {
          (Path \ "n").write(jodaDate).writes(jd) mustEqual(Json.obj("n" -> "1985-09-10"))
        }

        "time" in {
          (Path \ "n").write(jodaTime).writes(jd) mustEqual(Json.obj("n" -> dd.getTime))
        }

        "local date" in {
          import org.joda.time.LocalDate
          val ld = new LocalDate()
          (Path \ "n").write(jodaLocalDate).writes(ld) mustEqual(Json.obj("n" -> ld.toString))
        }
      }

      "sql date" in {
        import java.util.Date
        val f = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.FRANCE)
        val dd = f.parse("1985-09-10")
        val ds = new java.sql.Date(dd.getTime())
        (Path \ "n").write(sqlDate).writes(ds) mustEqual(Json.obj("n" -> "1985-09-10"))
      }

      "Boolean" in {
        (Path \ "n").write[Boolean, JsObject].writes(true) mustEqual(Json.obj("n" -> true))
        (Path \ "n" \ "o").write[Boolean, JsObject].writes(false) mustEqual(Json.obj("n" -> Json.obj("o"-> false)))
        (Path \ "n" \ "o" \ "p").write[Boolean, JsObject].writes(true) mustEqual(Json.obj("n" -> Json.obj("o"-> Json.obj("p"-> true))))
      }

      "String" in {
        (Path \ "n").write[String, JsObject].writes("foo") mustEqual(Json.obj("n" -> "foo"))
        (Path \ "n" \ "o").write[String, JsObject].writes("foo") mustEqual(Json.obj("n" -> Json.obj("o"-> "foo")))
        (Path \ "n" \ "o" \ "p").write[String, JsObject].writes("foo") mustEqual(Json.obj("n" -> Json.obj("o"-> Json.obj("p"-> "foo"))))
      }

      "Option" in {
        (Path \ "n").write[Option[String], JsObject].writes(Some("foo")) mustEqual(Json.obj("n" -> "foo"))
        (Path \ "n" \ "o").write[Option[String], JsObject].writes(Some("foo")) mustEqual(Json.obj("n" -> Json.obj("o"-> "foo")))
        (Path \ "n" \ "o" \ "p").write[Option[String], JsObject].writes(Some("foo")) mustEqual(Json.obj("n" -> Json.obj("o"-> Json.obj("p"-> "foo"))))

        (Path \ "n").write[Option[String], JsObject].writes(None) mustEqual(Json.obj())
        (Path \ "n" \ "o").write[Option[String], JsObject].writes(None) mustEqual(Json.obj())
        (Path \ "n" \ "o" \ "p").write[Option[String], JsObject].writes(None) mustEqual(Json.obj())
      }

      "Map[String, Seq[V]]" in {
        import play.api.data.mapping.json.Writes.{ map => mm }
        (Path \ "n").write[Map[String, Seq[String]], JsObject].writes(Map("foo" -> Seq("bar"))) mustEqual(Json.obj("n" -> Json.obj("foo" -> Seq("bar"))))
        (Path \ "n").write[Map[String, Seq[Int]], JsObject].writes(Map("foo" -> Seq(4))) mustEqual(Json.obj("n" -> Json.obj("foo" -> Seq(4))))
        (Path \ "n" \ "o").write[Map[String, Seq[Int]], JsObject].writes(Map("foo" -> Seq(4))) mustEqual(Json.obj("n" -> Json.obj("o" -> Json.obj("foo" -> Seq(4)))))
        (Path \ "n" \ "o").write[Map[String, Int], JsObject].writes(Map("foo" -> 4)) mustEqual(Json.obj("n" -> Json.obj("o" -> Json.obj("foo" -> 4))))
        (Path \ "n" \ "o").write[Map[String, Int], JsObject].writes(Map.empty) mustEqual(Json.obj("n" -> Json.obj("o" -> Json.obj())))
      }

      "Traversable" in {
        import play.api.data.mapping.json.Writes.{ traversable => tr }
        (Path \ "n").write[Traversable[String], JsObject].writes(Array("foo", "bar")) mustEqual(Json.obj("n" -> Seq("foo", "bar")))
        (Path \ "n" \ "o").write[Traversable[String], JsObject].writes(Array("foo", "bar")) mustEqual(Json.obj("n" -> Json.obj("o"-> Seq("foo", "bar"))))
        (Path \ "n" \ "o" \ "p").write[Traversable[String], JsObject].writes(Array("foo", "bar")) mustEqual(Json.obj("n" -> Json.obj("o"-> Json.obj("p"-> Seq("foo", "bar")))))

        (Path \ "n").write[Traversable[String], JsObject].writes(Array[String]()) mustEqual(Json.obj("n" -> Seq[String]()))
        (Path \ "n" \ "o").write[Traversable[String], JsObject].writes(Array[String]()) mustEqual(Json.obj("n" -> Json.obj("o"-> Seq[String]())))
        (Path \ "n" \ "o" \ "p").write[Traversable[String], JsObject].writes(Array[String]()) mustEqual(Json.obj("n" -> Json.obj("o"-> Json.obj("p"-> Seq[String]()))))
      }

      "Array" in {
        (Path \ "n").write[Array[String], JsObject].writes(Array("foo", "bar")) mustEqual(Json.obj("n" -> Seq("foo", "bar")))
        (Path \ "n" \ "o").write[Array[String], JsObject].writes(Array("foo", "bar")) mustEqual(Json.obj("n" -> Json.obj("o"-> Seq("foo", "bar"))))
        (Path \ "n" \ "o" \ "p").write[Array[String], JsObject].writes(Array("foo", "bar")) mustEqual(Json.obj("n" -> Json.obj("o"-> Json.obj("p"-> Seq("foo", "bar")))))

        (Path \ "n").write[Array[String], JsObject].writes(Array()) mustEqual(Json.obj("n" -> Seq[String]()))
        (Path \ "n" \ "o").write[Array[String], JsObject].writes(Array()) mustEqual(Json.obj("n" -> Json.obj("o"-> Seq[String]())))
        (Path \ "n" \ "o" \ "p").write[Array[String], JsObject].writes(Array()) mustEqual(Json.obj("n" -> Json.obj("o"-> Json.obj("p"-> Seq[String]()))))
      }

      "Seq" in {
        (Path \ "n").write[Seq[String], JsObject].writes(Seq("foo", "bar")) mustEqual(Json.obj("n" -> Seq("foo", "bar")))
        (Path \ "n" \ "o").write[Seq[String], JsObject].writes(Seq("foo", "bar")) mustEqual(Json.obj("n" -> Json.obj("o"-> Seq("foo", "bar"))))
        (Path \ "n" \ "o" \ "p").write[Seq[String], JsObject].writes(Seq("foo", "bar")) mustEqual(Json.obj("n" -> Json.obj("o"-> Json.obj("p"-> Seq("foo", "bar")))))

        (Path \ "n").write[Seq[String], JsObject].writes(Nil) mustEqual(Json.obj("n" -> Seq[String]()))
        (Path \ "n" \ "o").write[Seq[String], JsObject].writes(Nil) mustEqual(Json.obj("n" -> Json.obj("o"-> Seq[String]())))
        (Path \ "n" \ "o" \ "p").write[Seq[String], JsObject].writes(Nil) mustEqual(Json.obj("n" -> Json.obj("o"-> Json.obj("p"-> Seq[String]()))))
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
      w.writes(500d) mustEqual(Json.obj("foo" -> "500,00 €"))

      val w2 = To[JsValue] { __ => (__ \ "foo").write(formatter) }
      w2.writes(500d) mustEqual(Json.obj("foo" -> "500,00 €"))
    }

    "compose" in {
      import play.api.libs.functional._

      val w = To[JsObject] { __ =>
        ((__ \ "email").write[Option[String]] ~
         (__ \ "phones").write[Seq[String]]).tupled
      }

      val v =  Some("jto@foobar.com") -> Seq("01.23.45.67.89", "98.76.54.32.10")

      w.writes(v) mustEqual Json.obj("email" -> "jto@foobar.com", "phones" -> Seq("01.23.45.67.89", "98.76.54.32.10"))
      w.writes(Some("jto@foobar.com") -> Nil) mustEqual Json.obj("email" -> "jto@foobar.com", "phones" -> Seq[String]())
      w.writes(None -> Nil) mustEqual Json.obj("phones" -> Seq[String]())
    }

    "write Failure" in {
      import play.api.data.mapping.json.Writes.{ failure => ff }
      val f = Failure[(Path, Seq[ValidationError]), String](Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Int"))))

      implicitly[Write[(Path, Seq[ValidationError]), JsObject]]
      implicitly[Write[Failure[(Path, Seq[ValidationError]), String], JsObject]]

      (Path \ "errors").write[Failure[(Path, Seq[ValidationError]), String], JsObject]
        .writes(f) mustEqual(Json.parse("""{"errors":{"/n":[{"msg":"validation.type-mismatch","args":["Int"]}]}}"""))
    }

    "write Map" in {
      import play.api.libs.functional.syntax.unlift

      implicit val contactInformation = To[JsObject] { __ =>
        ((__ \ "label").write[String] ~
          (__ \ "email").write[Option[String]] ~
          (__ \ "phones").write[Seq[String]]) (unlift(ContactInformation.unapply _))
      }

      implicit val contactWrite = To[JsObject] { __ =>
        ((__ \ "firstname").write[String] ~
         (__ \ "lastname").write[String] ~
         (__ \ "company").write[Option[String]] ~
         (__ \ "informations").write[Seq[ContactInformation]]) (unlift(Contact.unapply _))
      }

      contactWrite.writes(contact) mustEqual contactJson
    }

  }

}