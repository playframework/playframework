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
    "informations[0].label" -> Seq("Personal"),
    "informations[0].email" -> Seq("fakecontact@gmail.com"),
    "informations[0].phones[0]" -> Seq("01.23.45.67.89"),
    "informations[0].phones[1]" -> Seq("98.76.54.32.10"))

  import Writes._
  import PM._

  "Writes" should {

    "write string" in {
      val w = (Path \ "label").write[String, UrlFormEncoded]
      w.writes("Hello World") mustEqual Map("label" -> Seq("Hello World"))
    }

    "ignore values" in {
      (Path \ "n").write(ignored("foo")).writes("test") mustEqual Map("n" -> Seq("foo"))
      (Path \ "n").write(ignored(42)).writes(0) mustEqual Map("n" -> Seq("42"))
    }

    "write option" in {
      val w = (Path \ "email").write[Option[String], UrlFormEncoded]
      w.writes(Some("Hello World")) mustEqual Map("email" -> Seq("Hello World"))
      w.writes(None) mustEqual Map.empty

      (Path \ "n").write(optionW(anyval[Int])).writes(Some(5)) mustEqual Map("n" -> Seq("5"))
      (Path \ "n").write(optionW(anyval[Int])).writes(None) mustEqual Map.empty

      case class Foo(name: String)
      implicit val wf = (Path \ "name").write[String, UrlFormEncoded].contramap((_: Foo).name)
      val wmf = (Path \ "maybe").write[Option[Foo], UrlFormEncoded]
      wmf.writes(Some(Foo("bar"))) mustEqual Map("maybe.name" -> Seq("bar"))
      wmf.writes(None) mustEqual Map.empty
    }

    "write seq" in {
      val w = (Path \ "phones").write[Seq[String], UrlFormEncoded]
      w.writes(Seq("01.23.45.67.89", "98.76.54.32.10")) mustEqual Map("phones[0]" -> Seq("01.23.45.67.89"), "phones[1]" -> Seq("98.76.54.32.10"))
      w.writes(Nil) mustEqual Map.empty
    }

    "support primitives types" in {

      "Int" in {
        To[UrlFormEncoded] { __ => (__ \ "n").write[Int] }.writes(4) mustEqual(Map("n" -> Seq("4")))
        To[UrlFormEncoded] { __ => (__ \ "n" \ "o").write[Int] }.writes(4) mustEqual(Map("n.o" -> Seq("4")))
        To[UrlFormEncoded] { __ => (__ \ "n" \ "o" \ "p").write[Int] }.writes(4) mustEqual(Map("n.o.p" -> Seq("4")))
      }

      "Short" in {
        To[UrlFormEncoded] { __ => (__ \ "n").write[Short] }.writes(4) mustEqual(Map("n" -> Seq("4")))
        To[UrlFormEncoded] { __ => (__ \ "n" \ "o").write[Short] }.writes(4) mustEqual(Map("n.o" -> Seq("4")))
        To[UrlFormEncoded] { __ => (__ \ "n" \ "o" \ "p").write[Short] }.writes(4) mustEqual(Map("n.o.p" -> Seq("4")))
      }

      "Long" in {
        To[UrlFormEncoded] { __ => (__ \ "n").write[Long] }.writes(4) mustEqual(Map("n" -> Seq("4")))
        To[UrlFormEncoded] { __ => (__ \ "n" \ "o").write[Long] }.writes(4) mustEqual(Map("n.o" -> Seq("4")))
        To[UrlFormEncoded] { __ => (__ \ "n" \ "o" \ "p").write[Long] }.writes(4) mustEqual(Map("n.o.p" -> Seq("4")))
      }

      "Float" in {
        To[UrlFormEncoded] { __ => (__ \ "n").write[Float] }.writes(4) mustEqual(Map("n" -> Seq("4.0")))
        To[UrlFormEncoded] { __ => (__ \ "n" \ "o").write[Float] }.writes(4.8F) mustEqual(Map("n.o" -> Seq("4.8")))
        To[UrlFormEncoded] { __ => (__ \ "n" \ "o" \ "p").write[Float] }.writes(4.8F) mustEqual(Map("n.o.p" -> Seq("4.8")))
      }

      "Double" in {
        To[UrlFormEncoded] { __ => (__ \ "n").write[Double] }.writes(4) mustEqual(Map("n" -> Seq("4.0")))
        To[UrlFormEncoded] { __ => (__ \ "n" \ "o").write[Double] }.writes(4.8D) mustEqual(Map("n.o" -> Seq("4.8")))
        To[UrlFormEncoded] { __ => (__ \ "n" \ "o" \ "p").write[Double] }.writes(4.8D) mustEqual(Map("n.o.p" -> Seq("4.8")))
      }

      "java BigDecimal" in {
        import java.math.{ BigDecimal => jBigDecimal }
        To[UrlFormEncoded] { __ => (__ \ "n").write[jBigDecimal] }.writes(new jBigDecimal("4.0")) mustEqual(Map("n" -> Seq("4.0")))
        To[UrlFormEncoded] { __ => (__ \ "n" \ "o").write[jBigDecimal] }.writes(new jBigDecimal("4.8")) mustEqual(Map("n.o" -> Seq("4.8")))
        To[UrlFormEncoded] { __ => (__ \ "n" \ "o" \ "p").write[jBigDecimal] }.writes(new jBigDecimal("4.8")) mustEqual(Map("n.o.p" -> Seq("4.8")))
      }

      "scala BigDecimal" in {
        To[UrlFormEncoded] { __ => (__ \ "n").write[BigDecimal] }.writes(BigDecimal("4.0")) mustEqual(Map("n" -> Seq("4.0")))
        To[UrlFormEncoded] { __ => (__ \ "n" \ "o").write[BigDecimal] }.writes(BigDecimal("4.8")) mustEqual(Map("n.o" -> Seq("4.8")))
        To[UrlFormEncoded] { __ => (__ \ "n" \ "o" \ "p").write[BigDecimal] }.writes(BigDecimal("4.8")) mustEqual(Map("n.o.p" -> Seq("4.8")))
      }

      "date" in {
        import java.util.Date
        val f = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.FRANCE)
        val d = f.parse("1985-09-10")
        To[UrlFormEncoded] { __ => (__ \ "n").write(date) }.writes(d) mustEqual(Map("n" -> Seq("1985-09-10")))
      }

      "iso date" in {
        skipped("Can't test on CI")
        import java.util.Date
        val f = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.FRANCE)
        val d = f.parse("1985-09-10")
        To[UrlFormEncoded] { __ => (__ \ "n").write(isoDate) }.writes(d) mustEqual(Map("n" -> Seq("1985-09-10T00:00:00+02:00")))
      }

      "joda" in {
        import org.joda.time.DateTime
        val f = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.FRANCE)
        val dd = f.parse("1985-09-10")
        val jd = new DateTime(dd)

        "date" in {
          To[UrlFormEncoded] { __ => (__ \ "n").write(jodaDate) }.writes(jd) mustEqual(Map("n" -> Seq("1985-09-10")))
        }

        "time" in {
          To[UrlFormEncoded] { __ => (__ \ "n").write(jodaTime) }.writes(jd) mustEqual(Map("n" -> Seq(dd.getTime.toString)))
        }

        "local date" in {
          import org.joda.time.LocalDate
          val ld = new LocalDate()
          To[UrlFormEncoded] { __ => (__ \ "n").write(jodaLocalDate) }.writes(ld) mustEqual(Map("n" -> Seq(ld.toString)))
        }
      }

      "sql date" in {
        import java.util.Date
        val f = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.FRANCE)
        val dd = f.parse("1985-09-10")
        val ds = new java.sql.Date(dd.getTime())
        To[UrlFormEncoded] { __ => (__ \ "n").write(sqlDate) }.writes(ds) mustEqual(Map("n" -> Seq("1985-09-10")))
      }

      "Boolean" in {
        To[UrlFormEncoded] { __ => (__ \ "n").write[Boolean] }.writes(true) mustEqual(Map("n" -> Seq("true")))
        To[UrlFormEncoded] { __ => (__ \ "n" \ "o").write[Boolean] }.writes(false) mustEqual(Map("n.o" -> Seq("false")))
        To[UrlFormEncoded] { __ => (__ \ "n" \ "o" \ "p").write[Boolean] }.writes(true) mustEqual(Map("n.o.p" -> Seq("true")))
      }

      "String" in {
        To[UrlFormEncoded] { __ => (__ \ "n").write[String] }.writes("foo") mustEqual(Map("n" -> Seq("foo")))
        To[UrlFormEncoded] { __ => (__ \ "n" \ "o").write[String] }.writes("foo") mustEqual(Map("n.o" -> Seq("foo")))
        To[UrlFormEncoded] { __ => (__ \ "n" \ "o" \ "p").write[String] }.writes("foo") mustEqual(Map("n.o.p" -> Seq("foo")))
      }

      "Option" in {
        To[UrlFormEncoded] { __ => (__ \ "n").write[Option[String]] }.writes(Some("foo")) mustEqual(Map("n" -> Seq("foo")))
        To[UrlFormEncoded] { __ => (__ \ "n" \ "o").write[Option[String]] }.writes(Some("foo")) mustEqual(Map("n.o" -> Seq("foo")))
        To[UrlFormEncoded] { __ => (__ \ "n" \ "o" \ "p").write[Option[String]] }.writes(Some("foo")) mustEqual(Map("n.o.p" -> Seq("foo")))

        To[UrlFormEncoded] { __ => (__ \ "n").write[Option[String]] }.writes(None) mustEqual(Map.empty)
        To[UrlFormEncoded] { __ => (__ \ "n" \ "o").write[Option[String]] }.writes(None) mustEqual(Map.empty)
        To[UrlFormEncoded] { __ => (__ \ "n" \ "o" \ "p").write[Option[String]] }.writes(None) mustEqual(Map.empty)
      }

      "Map[String, Seq[V]]" in {
        To[UrlFormEncoded] { __ => (__ \ "n").write[Map[String, Seq[String]]] }.writes(Map("foo" -> Seq("bar"))) mustEqual((Map("n.foo" -> Seq("bar"))))
        To[UrlFormEncoded] { __ => (__ \ "n").write[Map[String, Seq[Int]]] }.writes(Map("foo" -> Seq(4))) mustEqual((Map("n.foo" -> Seq("4"))))
        To[UrlFormEncoded] { __ => (__ \ "n" \ "o").write[Map[String, Seq[Int]]] }.writes(Map("foo" -> Seq(4))) mustEqual((Map("n.o.foo" -> Seq("4"))))
        To[UrlFormEncoded] { __ => (__ \ "n" \ "o").write[Map[String, Int]] }.writes(Map("foo" -> 4)) mustEqual((Map("n.o.foo" -> Seq("4"))))
        To[UrlFormEncoded] { __ => (__ \ "n" \ "o").write[Map[String, Int]] }.writes(Map.empty) mustEqual(Map.empty)
      }

      "Traversable" in {
        To[UrlFormEncoded] { __ => (__ \ "n").write[Traversable[String]] }.writes(Array("foo", "bar").toTraversable) mustEqual((Map("n[0]" -> Seq("foo"), "n[1]" -> Seq("bar"))))
        To[UrlFormEncoded] { __ => (__ \ "n" \ "o").write[Traversable[String]] }.writes(Array("foo", "bar").toTraversable) mustEqual((Map("n.o[0]" -> Seq("foo"), "n.o[1]" -> Seq("bar"))))
        To[UrlFormEncoded] { __ => (__ \ "n" \ "o" \ "p").write[Traversable[String]] }.writes(Array("foo", "bar").toTraversable) mustEqual((Map("n.o.p[0]" -> Seq("foo"), "n.o.p[1]" -> Seq("bar"))))

        To[UrlFormEncoded] { __ => (__ \ "n").write[Traversable[String]] }.writes(Array().toTraversable) mustEqual(Map.empty)
        To[UrlFormEncoded] { __ => (__ \ "n" \ "o").write[Traversable[String]] }.writes(Array().toTraversable) mustEqual(Map.empty)
        To[UrlFormEncoded] { __ => (__ \ "n" \ "o" \ "p").write[Traversable[String]] }.writes(Array().toTraversable) mustEqual(Map.empty)
      }

      "Array" in {
        To[UrlFormEncoded] { __ => (__ \ "n").write[Array[String]] }.writes(Array("foo", "bar")) mustEqual((Map("n[0]" -> Seq("foo"), "n[1]" -> Seq("bar"))))
        To[UrlFormEncoded] { __ => (__ \ "n" \ "o").write[Array[String]] }.writes(Array("foo", "bar")) mustEqual((Map("n.o[0]" -> Seq("foo"), "n.o[1]" -> Seq("bar"))))
        To[UrlFormEncoded] { __ => (__ \ "n" \ "o" \ "p").write[Array[String]] }.writes(Array("foo", "bar")) mustEqual((Map("n.o.p[0]" -> Seq("foo"), "n.o.p[1]" -> Seq("bar"))))

        To[UrlFormEncoded] { __ => (__ \ "n").write[Array[String]] }.writes(Array()) mustEqual(Map.empty)
        To[UrlFormEncoded] { __ => (__ \ "n" \ "o").write[Array[String]] }.writes(Array()) mustEqual(Map.empty)
        To[UrlFormEncoded] { __ => (__ \ "n" \ "o" \ "p").write[Array[String]] }.writes(Array()) mustEqual(Map.empty)
      }

      "Seq" in {
        To[UrlFormEncoded] { __ => (__ \ "n").write[Seq[String]] }.writes(Seq("foo", "bar")) mustEqual((Map("n[0]" -> Seq("foo"), "n[1]" -> Seq("bar"))))
        To[UrlFormEncoded] { __ => (__ \ "n" \ "o").write[Seq[String]] }.writes(Seq("foo", "bar")) mustEqual((Map("n.o[0]" -> Seq("foo"), "n.o[1]" -> Seq("bar"))))
        To[UrlFormEncoded] { __ => (__ \ "n" \ "o" \ "p").write[Seq[String]] }.writes(Seq("foo", "bar")) mustEqual((Map("n.o.p[0]" -> Seq("foo"), "n.o.p[1]" -> Seq("bar"))))

        To[UrlFormEncoded] { __ => (__ \ "n").write[Seq[String]] }.writes(Nil) mustEqual(Map.empty)
        To[UrlFormEncoded] { __ => (__ \ "n" \ "o").write[Seq[String]] }.writes(Nil) mustEqual(Map.empty)
        To[UrlFormEncoded] { __ => (__ \ "n" \ "o" \ "p").write[Seq[String]] }.writes(Nil) mustEqual(Map.empty)
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

      val w2 = To[UrlFormEncoded] { __ => (__ \ "foo").write(formatter) }
      w2.writes(500d) mustEqual(Map("foo" -> List("500,00 €")))
    }

    "compose" in {
      val w = To[UrlFormEncoded] { __ =>
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
        implicit val contactInformation = To[UrlFormEncoded] { __ =>
          ((__ \ "label").write[String] ~
           (__ \ "email").write[Option[String]] ~
           (__ \ "phones").write[Seq[String]]) (unlift(ContactInformation.unapply _))
        }

        To[UrlFormEncoded] { __ =>
          ((__ \ "firstname").write[String] ~
           (__ \ "lastname").write[String] ~
           (__ \ "company").write[Option[String]] ~
           (__ \ "informations").write[Seq[ContactInformation]]) (unlift(Contact.unapply _))
        }
      }

      contactWrite.writes(contact) mustEqual contactMap
    }

    "write recursive" in {
      case class RecUser(name: String, friends: List[RecUser] = Nil)
      val u = RecUser(
        "bob",
        List(RecUser("tom")))

      val m = Map(
        "name" -> Seq("bob"),
        "friends[0].name" -> Seq("tom"))

      case class User1(name: String, friend: Option[User1] = None)
      val u1 = User1("bob", Some(User1("tom")))
      val m1 = Map(
        "name" -> Seq("bob"),
        "friend.name" -> Seq("tom"))

      "using explicit notation" in {
        lazy val w: Write[RecUser, UrlFormEncoded] = To[UrlFormEncoded]{ __ =>
          ((__ \ "name").write[String] ~
           (__ \ "friends").write(seqW(w)))(unlift(RecUser.unapply _))
        }
        w.writes(u) mustEqual m

        lazy val w2: Write[RecUser, UrlFormEncoded] =
          ((Path \ "name").write[String, UrlFormEncoded] ~
           (Path \ "friends").write(seqW(w2)))(unlift(RecUser.unapply _))
        w2.writes(u) mustEqual m

        lazy val w3: Write[User1, UrlFormEncoded] = To[UrlFormEncoded]{ __ =>
          ((__ \ "name").write[String] ~
           (__ \ "friend").write(optionW(w3)))(unlift(User1.unapply _))
        }
        w3.writes(u1) mustEqual m1
      }

      "using implicit notation" in {
        implicit lazy val w: Write[RecUser, UrlFormEncoded] = To[UrlFormEncoded]{ __ =>
          ((__ \ "name").write[String] ~
           (__ \ "friends").write[Seq[RecUser]])(unlift(RecUser.unapply _))
        }
        w.writes(u) mustEqual m

        implicit lazy val w3: Write[User1, UrlFormEncoded] = To[UrlFormEncoded]{ __ =>
          ((__ \ "name").write[String] ~
           (__ \ "friend").write[Option[User1]])(unlift(User1.unapply _))
        }
        w3.writes(u1) mustEqual m1
      }

    }

  }

}