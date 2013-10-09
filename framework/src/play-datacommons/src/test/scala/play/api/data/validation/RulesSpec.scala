  package play.api.data.mapping

import org.specs2.mutable._
import scala.util.control.Exception._
import play.api.libs.functional._
import play.api.libs.functional.syntax._

object RulesSpec extends Specification {

  "Rules" should {

    import Rules._
    import PM._
    val valid: UrlFormEncoded = Map(
      "firstname" -> Seq("Julien"),
      "lastname" -> Seq("Tournay"),
      "age" -> Seq("27"),
      "informations.label" -> Seq("Personal"),
      "informations.email" -> Seq("fakecontact@gmail.com"),
      "informations.phones" -> Seq("01.23.45.67.89", "98.76.54.32.10"))

    val invalid = Map(
     "firstname" -> Seq("Julien"),
     "lastname" -> Seq("Tournay"),
     "age" -> Seq("27"),
     "informations.label" -> Seq(""),
     "informations.email" -> Seq("fakecontact@gmail.com"),
     "informations.phones" -> Seq("01.23.45.67.89", "98.76.54.32.10"))

    "extract data" in {
      From[UrlFormEncoded] { __ =>
        (__ \ "firstname").read[String]
      }.validate(valid) mustEqual(Success("Julien"))

      val error = Failure(Seq((Path \ "foo") -> Seq(ValidationError("validation.required"))))
      From[UrlFormEncoded] { __ =>
        (__ \ "foo").read[String]
      }.validate(invalid) mustEqual(error)
    }

    "support checked" in {
      val js = Map("issmth" -> Seq("true"))
      val p = Path \ "issmth"
      p.read[UrlFormEncoded, String, Boolean](checked).validate(js) mustEqual(Success(true))
      p.read[UrlFormEncoded, String, Boolean](checked).validate(Map.empty) mustEqual(Failure(Seq(Path \ "issmth" -> Seq(ValidationError("validation.required")))))
      p.read[UrlFormEncoded, String, Boolean](checked).validate(Map("issmth" -> Seq("false"))) mustEqual(Failure(Seq(Path \ "issmth" -> Seq(ValidationError("validation.equals", true)))))
    }

  //   // "ignore values" in {
  //   //   val r = From[UrlFormEncoded]{ __ =>
  //   //     ((__ \ "firstname").read(notEmpty) ~
  //   //      (__ \ "test").read(ignored(42))).tupled
  //   //   }
  //   //   r.validate(valid) mustEqual(Success("Julien" -> 42))
  //   // }

    "support primitives types" in {

      "Int" in {
        From[UrlFormEncoded] { __ => (__ \ "n").read[Int] }.validate(Map("n" -> Seq("4"))) mustEqual(Success(4))
        From[UrlFormEncoded] { __ => (__ \ "n").read[Int] }.validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Int")))))
        From[UrlFormEncoded] { __ => (__ \ "n").read[Int] }.validate(Map("n" -> Seq("4.8"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Int")))))
        From[UrlFormEncoded] { __ => (__ \ "n" \ "o").read[Int] }.validate(Map("n.o" -> Seq("4"))) mustEqual(Success(4))
        From[UrlFormEncoded] { __ => (__ \ "n" \ "o").read[Int] }.validate(Map("n.o" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" \ "o" -> Seq(ValidationError("validation.type-mismatch", "Int")))))

        From[UrlFormEncoded] { __ => (__ \ "n" \ "o" \ "p").read[Int] }.validate(Map("n.o.p" -> Seq("4"))) mustEqual(Success(4))
        From[UrlFormEncoded] { __ => (__ \ "n" \ "o" \ "p").read[Int] }.validate(Map("n.o.p" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" \ "o" \ "p" -> Seq(ValidationError("validation.type-mismatch", "Int")))))

        val errPath = Path \ "foo"
        val error = Failure(Seq(errPath -> Seq(ValidationError("validation.required"))))
        From[UrlFormEncoded] { __ => (__ \ "foo").read[Int] }.validate(Map("n" -> Seq("4"))) mustEqual(error)
      }

      "Short" in {
        From[UrlFormEncoded] { __ => (__ \ "n").read[Short] }.validate(Map("n" -> Seq("4"))) mustEqual(Success(4))
        From[UrlFormEncoded] { __ => (__ \ "n").read[Short] }.validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Short")))))
        From[UrlFormEncoded] { __ => (__ \ "n").read[Short] }.validate(Map("n" -> Seq("4.8"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Short")))))
      }

      "Long" in {
        From[UrlFormEncoded] { __ => (__ \ "n").read[Long] }.validate(Map("n" -> Seq("4"))) mustEqual(Success(4))
        From[UrlFormEncoded] { __ => (__ \ "n").read[Long] }.validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Long")))))
        From[UrlFormEncoded] { __ => (__ \ "n").read[Long] }.validate(Map("n" -> Seq("4.8"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Long")))))
      }

      "Float" in {
        From[UrlFormEncoded] { __ => (__ \ "n").read[Float] }.validate(Map("n" -> Seq("4"))) mustEqual(Success(4))
        From[UrlFormEncoded] { __ => (__ \ "n").read[Float] }.validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Float")))))
        From[UrlFormEncoded] { __ => (__ \ "n").read[Float] }.validate(Map("n" -> Seq("4.8"))) mustEqual(Success(4.8F))
      }

      "Double" in {
        From[UrlFormEncoded] { __ => (__ \ "n").read[Double] }.validate(Map("n" -> Seq("4"))) mustEqual(Success(4))
        From[UrlFormEncoded] { __ => (__ \ "n").read[Double] }.validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Double")))))
        From[UrlFormEncoded] { __ => (__ \ "n").read[Double] }.validate(Map("n" -> Seq("4.8"))) mustEqual(Success(4.8))
      }

      "java BigDecimal" in {
        import java.math.{ BigDecimal => jBigDecimal }
        From[UrlFormEncoded] { __ => (__ \ "n").read[jBigDecimal] }.validate(Map("n" -> Seq("4"))) mustEqual(Success(new jBigDecimal("4")))
        From[UrlFormEncoded] { __ => (__ \ "n").read[jBigDecimal] }.validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "BigDecimal")))))
        From[UrlFormEncoded] { __ => (__ \ "n").read[jBigDecimal] }.validate(Map("n" -> Seq("4.8"))) mustEqual(Success(new jBigDecimal("4.8")))
      }

      "scala BigDecimal" in {
        From[UrlFormEncoded] { __ => (__ \ "n").read[BigDecimal] }.validate(Map("n" -> Seq("4"))) mustEqual(Success(BigDecimal(4)))
        From[UrlFormEncoded] { __ => (__ \ "n").read[BigDecimal] }.validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "BigDecimal")))))
        From[UrlFormEncoded] { __ => (__ \ "n").read[BigDecimal] }.validate(Map("n" -> Seq("4.8"))) mustEqual(Success(BigDecimal(4.8)))
      }

      "date" in {
        import java.util.Date
        val f = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.FRANCE)
        From[UrlFormEncoded] { __ =>
          (__ \ "n").read(date)
        }.validate(Map("n" -> Seq("1985-09-10"))) mustEqual(Success(f.parse("1985-09-10")))

        From[UrlFormEncoded] { __ =>
          (__ \ "n").read(date)
        }.validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.date", "yyyy-MM-dd")))))
      }

      "iso date" in {
        import java.util.Date
        val f = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.FRANCE)
        From[UrlFormEncoded] { __ =>
          (__ \ "n").read(isoDate)
        }.validate(Map("n" -> Seq("1985-09-10T00:00:00+02:00"))) mustEqual(Success(f.parse("1985-09-10")))

        From[UrlFormEncoded] { __ =>
          (__ \ "n").read(isoDate)
        }.validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.iso8601")))))
      }

      "joda" in {
        import org.joda.time.DateTime
        val f = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.FRANCE)
        val dd = f.parse("1985-09-10")
        val jd = new DateTime(dd)

        "date" in {
          From[UrlFormEncoded] { __ =>
            (__ \ "n").read(jodaDate)
          }.validate(Map("n" -> Seq("1985-09-10"))) mustEqual(Success(jd))

          From[UrlFormEncoded] { __ =>
            (__ \ "n").read(jodaDate)
          }.validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.expected.jodadate.format", "yyyy-MM-dd")))))
        }

        "time" in {
          From[UrlFormEncoded] { __ =>
            (__ \ "n").read(jodaTime)
          }.validate(Map("n" -> Seq(dd.getTime.toString))) mustEqual(Success(jd))

          From[UrlFormEncoded] { __ =>
            (__ \ "n").read(jodaDate)
          }.validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.expected.jodadate.format", "yyyy-MM-dd")))))
        }

        "local date" in {
          import org.joda.time.LocalDate
          val ld = new LocalDate()

          From[UrlFormEncoded] { __ =>
            (__ \ "n").read(jodaLocalDate)
          }.validate(Map("n" -> Seq(ld.toString()))) mustEqual(Success(ld))

          From[UrlFormEncoded] { __ =>
            (__ \ "n").read(jodaLocalDate)
          }.validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.expected.jodadate.format", "")))))
        }
      }

      "sql date" in {
        import java.util.Date
        val f = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.FRANCE)
        val dd = f.parse("1985-09-10")
        val ds = new java.sql.Date(dd.getTime())

        From[UrlFormEncoded] { __ =>
          (__ \ "n").read(sqlDate)
        }.validate(Map("n" -> Seq("1985-09-10"))) mustEqual(Success(ds))
      }

      "Boolean" in {
        From[UrlFormEncoded] { __ => (__ \ "n").read[Boolean] }.validate(Map("n" -> Seq("true"))) mustEqual(Success(true))
        From[UrlFormEncoded] { __ => (__ \ "n").read[Boolean] }.validate(Map("n" -> Seq("TRUE"))) mustEqual(Success(true))
        From[UrlFormEncoded] { __ => (__ \ "n").read[Boolean] }.validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Boolean")))))
      }

      "String" in {
        From[UrlFormEncoded] { __ => (__ \ "n").read[String] }.validate(Map("n" -> Seq("foo"))) mustEqual(Success("foo"))
        From[UrlFormEncoded] { __ => (__ \ "o").read[String] }.validate(Map("o.n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "o" -> Seq(ValidationError("validation.required")))))
      }

      "Option" in {
        From[UrlFormEncoded] { __ => (__ \ "n").read[Option[Boolean]] }.validate(Map("n" -> Seq("true"))) mustEqual(Success(Some(true)))
        From[UrlFormEncoded] { __ => (__ \ "n").read[Option[Boolean]] }.validate(Map("foo" -> Seq("bar"))) mustEqual(Success(None))
        From[UrlFormEncoded] { __ => (__ \ "n").read[Option[Boolean]] }.validate(Map("n" -> Seq("bar"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Boolean")))))
      }

      "Map[String, Seq[V]]" in {
        import Rules.{ map => mm }
        From[UrlFormEncoded] { __ => (__ \ "n").read[Map[String, Seq[String]]] }.validate(Map("n.foo" -> Seq("bar"))) mustEqual(Success(Map("foo" -> Seq("bar"))))
        From[UrlFormEncoded] { __ => (__ \ "n").read[Map[String, Seq[Int]]] }.validate(Map("n.foo" -> Seq("4"), "n.bar" -> Seq("5"))) mustEqual(Success(Map("foo" -> Seq(4), "bar" -> Seq(5))))
        From[UrlFormEncoded] { __ => (__ \ "x").read[Map[String, Int]] }.validate(Map("n.foo" -> Seq("4"), "n.bar" -> Seq("frack"))) mustEqual(Failure(Seq(Path \ "x" -> Seq(ValidationError("validation.required")))))
        From[UrlFormEncoded] { __ => (__ \ "n").read[Map[String, Seq[Int]]] }.validate(Map("n.foo" -> Seq("4"), "n.bar" -> Seq("frack"))) mustEqual(Failure(Seq(Path \ "n" \ "bar" \ 0 -> Seq(ValidationError("validation.type-mismatch", "Int")))))
      }

      "Traversable" in {
        import Rules.{ traversable => tr } // avoid shadowing caused by specs
        From[UrlFormEncoded] { __ => (__ \ "n").read[Traversable[String]] }.validate(Map("n" -> Seq("foo"))).get.toSeq must haveTheSameElementsAs(Seq("foo"))
        From[UrlFormEncoded] { __ => (__ \ "n").read[Traversable[Int]] }.validate(Map("n" -> Seq("1", "2", "3"))).get.toSeq must haveTheSameElementsAs(Seq(1, 2, 3))
        From[UrlFormEncoded] { __ => (__ \ "n").read[Traversable[Int]] }.validate(Map("n" -> Seq("1", "paf"))) mustEqual(Failure(Seq(Path \ "n" \ 1 -> Seq(ValidationError("validation.type-mismatch", "Int")))))
      }

      "Array" in {
        From[UrlFormEncoded] { __ => (__ \ "n").read[Array[String]] }.validate(Map("n" -> Seq("foo"))).get.toSeq must haveTheSameElementsAs(Seq("foo"))
        From[UrlFormEncoded] { __ => (__ \ "n").read[Array[Int]] }.validate(Map("n" -> Seq("1", "2", "3"))).get.toSeq must haveTheSameElementsAs(Seq(1, 2, 3))
        From[UrlFormEncoded] { __ => (__ \ "n").read[Array[Int]] }.validate(Map("n" -> Seq("1", "paf"))) mustEqual(Failure(Seq(Path \ "n" \ 1 -> Seq(ValidationError("validation.type-mismatch", "Int")))))
      }

      "Seq" in {
        From[UrlFormEncoded] { __ => (__ \ "n").read[Seq[String]] }.validate(Map("n" -> Seq("foo"))).get must haveTheSameElementsAs(Seq("foo"))
        From[UrlFormEncoded] { __ => (__ \ "n").read[Seq[Int]] }.validate(Map("n" -> Seq("1", "2", "3"))).get must haveTheSameElementsAs(Seq(1, 2, 3))
        From[UrlFormEncoded] { __ => (__ \ "n").read[Seq[Int]] }.validate(Map(
          "n[0]" -> Seq("1"),
          "n[1]" -> Seq("2"),
          "n[3]" -> Seq("3")
        )).get must haveTheSameElementsAs(Seq(1, 2, 3))
        From[UrlFormEncoded] { __ => (__ \ "n").read[Seq[Int]] }.validate(Map("n" -> Seq("1", "paf"))) mustEqual(Failure(Seq(Path \ "n" \ 1 -> Seq(ValidationError("validation.type-mismatch", "Int")))))
      }
    }

    "validate data" in {
      From[UrlFormEncoded] { __ => (__  \ "firstname").read(notEmpty) }.validate(valid) mustEqual(Success("Julien"))

      val p = (Path \ "informations" \ "label")
      From[UrlFormEncoded] { __ => (__  \ "informations" \ "label").read(notEmpty) }.validate(valid) mustEqual(Success("Personal"))
      From[UrlFormEncoded] { __ => (__  \ "informations" \ "label").read(notEmpty) }.validate(invalid) mustEqual(Failure(Seq(p -> Seq(ValidationError("validation.nonemptytext")))))
    }

    "validate seq" in {
      def isNotEmpty[T <: Traversable[_]] = validateWith[T]("validation.notEmpty"){ !_.isEmpty }

      From[UrlFormEncoded] { __ => (__ \ "firstname").read[Seq[String]] }.validate(valid) mustEqual(Success(Seq("Julien")))
      From[UrlFormEncoded] { __ => (__ \ "foobar").read[Seq[String]] }.validate(valid) mustEqual(Success(Seq()))
      From[UrlFormEncoded] { __ => (__ \ "foobar").read(isNotEmpty[Seq[Int]]) }.validate(valid) mustEqual(Failure(Seq(Path \ "foobar" -> Seq(ValidationError("validation.notEmpty")))))
    }

    "validate optional" in {
      From[UrlFormEncoded] { __ => (__ \ "firstname").read[Option[String]] }.validate(valid) mustEqual(Success(Some("Julien")))
      From[UrlFormEncoded] { __ => (__ \ "firstname").read[Option[Int]] }.validate(valid) mustEqual(Failure(Seq(Path \ "firstname" -> Seq(ValidationError("validation.type-mismatch", "Int")))))
      From[UrlFormEncoded] { __ => (__ \ "foobar").read[Option[String]] }.validate(valid) mustEqual(Success(None))
    }

    "validate deep" in {
      val p = (Path \ "informations" \ "label")

      From[UrlFormEncoded] { __ =>
         (__ \ "informations").read(
          (__ \ "label").read(notEmpty))
      }.validate(valid) mustEqual(Success("Personal"))

       From[UrlFormEncoded] { __ =>
         (__ \ "informations").read(
          (__ \ "label").read(notEmpty))
      }.validate(invalid) mustEqual(Failure(Seq(p -> Seq(ValidationError("validation.nonemptytext")))))
    }

    "coerce type" in {
      From[UrlFormEncoded] { __ => (__ \ "age").read[Int] }.validate(valid) mustEqual(Success(27))
      From[UrlFormEncoded] { __ => (__ \ "firstname").read[Int] }.validate(valid) mustEqual(Failure(Seq((Path \ "firstname") -> Seq(ValidationError("validation.type-mismatch", "Int")))))
    }

    "compose constraints" in {
      // TODO: create MonoidOps
      val composed = notEmpty |+| minLength(3)
      From[UrlFormEncoded] { __ => (__ \ "firstname").read(composed) }.validate(valid) mustEqual(Success("Julien"))

      val p = Path \ "informations" \ "label"
      val err = Failure(Seq(p -> Seq(ValidationError("validation.nonemptytext"), ValidationError("validation.minLength", 3))))
      From[UrlFormEncoded] { __ => (__ \ "informations" \ "label").read(composed) }.validate(invalid) mustEqual(err)
    }

    "compose validations" in {
      import play.api.libs.functional.syntax._

      From[UrlFormEncoded] { __ =>
        ((__ \ "firstname").read(notEmpty) ~
         (__ \ "lastname").read(notEmpty)){ _ -> _ }
      }.validate(valid) mustEqual Success("Julien" -> "Tournay")

      From[UrlFormEncoded] { __ =>
        ((__ \ "firstname").read(notEmpty) ~
         (__ \ "lastname").read(notEmpty) ~
         (__ \ "informations" \ "label").read(notEmpty)).tupled
      }.validate(invalid) mustEqual Failure(Seq((Path \ "informations" \ "label") -> Seq(ValidationError("validation.nonemptytext"))))
    }

    "validate dependent fields" in {
      val v = Map(
        "login" -> Seq("Alice"),
        "password" -> Seq("s3cr3t"),
        "verify" -> Seq("s3cr3t"))

      val i1 = Map(
        "login" -> Seq("Alice"),
        "password" -> Seq("s3cr3t"),
        "verify" -> Seq(""))

      val i2 = Map(
        "login" -> Seq("Alice"),
        "password" -> Seq("s3cr3t"),
        "verify" -> Seq("bam"))

      val passRule = From[UrlFormEncoded] { __ =>
        ((__ \ "password").read(notEmpty) ~ (__ \ "verify").read(notEmpty))
          .tupled.compose(Rule.uncurry(Rules.equalTo[String]).repath(_ => (Path \ "verify")))
      }

      val rule = From[UrlFormEncoded] { __ =>
        ((__ \ "login").read(notEmpty) ~ passRule).tupled
      }

      rule.validate(v).mustEqual(Success("Alice" -> "s3cr3t"))
      rule.validate(i1).mustEqual(Failure(Seq(Path \ "verify" -> Seq(ValidationError("validation.nonemptytext")))))
      rule.validate(i2).mustEqual(Failure(Seq(Path \ "verify" -> Seq(ValidationError("validation.equals", "s3cr3t")))))
    }

    "validate subclasses (and parse the concrete class)" in {

      trait A { val name: String }
      case class B(name: String, foo: Int) extends A
      case class C(name: String, bar: Int) extends A

      val b = Map("name" -> Seq("B"), "foo" -> Seq("4"))
      val c = Map("name" -> Seq("C"), "bar" -> Seq("6"))
      val e = Map("name" -> Seq("E"), "eee" -> Seq("6"))

      val typeFailure = Failure(Seq(Path -> Seq(ValidationError("validation.unknownType"))))

      "by trying all possible Rules" in {
        val rb: Rule[UrlFormEncoded, A] = From[UrlFormEncoded]{ __ =>
          ((__ \ "name").read[String] ~ (__ \ "foo").read[Int])(B.apply _)
        }

        val rc: Rule[UrlFormEncoded, A] = From[UrlFormEncoded]{ __ =>
          ((__ \ "name").read[String] ~ (__ \ "bar").read[Int])(C.apply _)
        }

        val rule = rb orElse rc orElse Rule(_ => typeFailure)

        rule.validate(b) mustEqual(Success(B("B", 4)))
        rule.validate(c) mustEqual(Success(C("C", 6)))
        rule.validate(e) mustEqual(Failure(Seq(Path -> Seq(ValidationError("validation.unknownType")))))
      }

      "by dicriminating on fields" in {

        val rule = From[UrlFormEncoded] { __ =>
          (__ \ "name").read[String].flatMap[A] {
            case "B" => ((__ \ "name").read[String] ~ (__ \ "foo").read[Int])(B.apply _)
            case "C" => ((__ \ "name").read[String] ~ (__ \ "bar").read[Int])(C.apply _)
            case _ => Rule(_ => typeFailure)
          }
        }

        rule.validate(b) mustEqual(Success(B("B", 4)))
        rule.validate(c) mustEqual(Success(C("C", 6)))
        rule.validate(e) mustEqual(Failure(Seq(Path -> Seq(ValidationError("validation.unknownType")))))
      }

    }

    "perform complex validation" in {
      import play.api.libs.functional.syntax._

      case class Contact(
        firstname: String,
        lastname: String,
        company: Option[String],
        informations: Seq[ContactInformation])

      case class ContactInformation(
        label: String,
        email: Option[String],
        phones: Seq[String])

      val validM = Map(
        "firstname" -> Seq("Julien"),
        "lastname" -> Seq("Tournay"),
        "age" -> Seq("27"),
        "informations[0].label" -> Seq("Personal"),
        "informations[0].email" -> Seq("fakecontact@gmail.com"),
        "informations[0].phones" -> Seq("01.23.45.67.89", "98.76.54.32.10"))

      val validWithPhones = Map(
        "firstname" -> Seq("Julien"),
        "lastname" -> Seq("Tournay"),
        "age" -> Seq("27"),
        "informations[0].label" -> Seq("Personal"),
        "informations[0].email" -> Seq("fakecontact@gmail.com"),
        "informations[0].phones[0]" -> Seq("01.23.45.67.89"),
        "informations[0].phones[1]" -> Seq("98.76.54.32.10"))

      val invalidM = Map(
        "firstname" -> Seq("Julien"),
        "lastname" -> Seq("Tournay"),
        "age" -> Seq("27"),
        "informations[0].label" -> Seq(""),
        "informations[0].email" -> Seq("fakecontact@gmail.com"),
        "informations[0].phones" -> Seq("01.23.45.67.89", "98.76.54.32.10"))

      val infoValidation = From[UrlFormEncoded]{ __ =>
       ((__ \ "label").read(notEmpty) ~
        (__ \ "email").read(option(email)) ~
        (__ \ "phones").read(seq(notEmpty))) (ContactInformation.apply _)
      }

      val contactValidation = From[UrlFormEncoded]{ __ =>
       ((__ \ "firstname").read(notEmpty) ~
        (__ \ "lastname").read(notEmpty) ~
        (__ \ "company").read[Option[String]] ~
        (__ \ "informations").read(seq(infoValidation))) (Contact.apply _)
      }

      val expected =
        Contact("Julien", "Tournay", None, Seq(
          ContactInformation("Personal", Some("fakecontact@gmail.com"), List("01.23.45.67.89", "98.76.54.32.10"))))

      contactValidation.validate(validM) mustEqual(Success(expected))
      contactValidation.validate(validWithPhones) mustEqual(Success(expected))
      contactValidation.validate(invalidM) mustEqual(Failure(Seq(
        (Path \ "informations" \ 0 \ "label") -> Seq(ValidationError("validation.nonemptytext")))))
    }

    "read recursive" in {
      case class RecUser(name: String, friends: Seq[RecUser] = Nil)
      val u = RecUser(
        "bob",
        Seq(RecUser("tom")))

      val m = Map(
        "name" -> Seq("bob"),
        "friends[0].name" -> Seq("tom"),
        "friends[0].friends" -> Seq())

      case class User1(name: String, friend: Option[User1] = None)
      val u1 = User1("bob", Some(User1("tom")))
      val m1 = Map(
        "name" -> Seq("bob"),
        "friend.name" -> Seq("tom"))

      "using explicit notation" in {
        lazy val w: Rule[UrlFormEncoded, RecUser] = From[UrlFormEncoded]{ __ =>
          ((__ \ "name").read[String] ~
           (__ \ "friends").read(seq(w)))(RecUser.apply _)
        }
        w.validate(m) mustEqual Success(u)

        lazy val w2: Rule[UrlFormEncoded, RecUser] =
          ((Path \ "name").read[UrlFormEncoded, String] ~
           (Path \ "friends").read(seq(w2)))(RecUser.apply _)
        w2.validate(m) mustEqual Success(u)

        lazy val w3: Rule[UrlFormEncoded, User1] = From[UrlFormEncoded]{ __ =>
          ((__ \ "name").read[String] ~
           (__ \ "friend").read(option(w3)))(User1.apply _)
        }
        w3.validate(m1) mustEqual Success(u1)
      }

      "using implicit notation" in {
        implicit lazy val w: Rule[UrlFormEncoded, RecUser] = From[UrlFormEncoded]{ __ =>
          ((__ \ "name").read[String] ~
           (__ \ "friends").read[Seq[RecUser]])(RecUser.apply _)
        }
        w.validate(m) mustEqual Success(u)

        implicit lazy val w3: Rule[UrlFormEncoded, User1] = From[UrlFormEncoded]{ __ =>
          ((__ \ "name").read[String] ~
           (__ \ "friend").read[Option[User1]])(User1.apply _)
        }
        w3.validate(m1) mustEqual Success(u1)
      }

    }
  }
}
