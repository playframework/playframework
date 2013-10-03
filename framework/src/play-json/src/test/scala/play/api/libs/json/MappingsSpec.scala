package play.api.libs.json

import org.specs2.mutable._
import scala.util.control.Exception._
import play.api.libs.functional._
import play.api.libs.functional.syntax._
import play.api.data.mapping._

import scala.language.reflectiveCalls

object MappingsSpec extends Specification {

  "Json Mappings" should {
    import play.api.data.mapping.json.Rules
    import Rules._

    val valid = Json.obj(
    "firstname" -> "Julien",
    "lastname" -> "Tournay",
    "age" -> 27,
    "informations" -> Json.obj(
      "label" -> "Personal",
      "email" -> "fakecontact@gmail.com",
      "phones" -> Seq("01.23.45.67.89", "98.76.54.32.10")))

    val invalid = Json.obj(
      "firstname" -> "Julien",
      "lastname" -> "Tournay",
      "age" -> 27,
      "informations" -> Json.obj(
        "label" -> "",
        "email" -> "fakecontact@gmail.com",
        "phones" -> Seq("01.23.45.67.89", "98.76.54.32.10")))

    "extract data" in {
      (Path \ "firstname").read[JsValue, String].validate(valid) mustEqual(Success("Julien"))
      val errPath = Path \ "foo"
      val error = Failure(Seq(errPath -> Seq(ValidationError("validation.required"))))
      errPath.read[JsValue, String].validate(invalid) mustEqual(error)
    }

    "support checked" in {
      val js = Json.obj("issmth" -> true)
      val p = Path \ "issmth"
      p.read(checked).validate(js) mustEqual(Success(true))
      p.read(checked).validate(Json.obj()) mustEqual(Failure(Seq(Path \ "issmth" -> Seq(ValidationError("validation.required")))))
      p.read(checked).validate(Json.obj("issmth" -> false)) mustEqual(Failure(Seq(Path \ "issmth" -> Seq(ValidationError("validation.equals", true)))))
    }

    "support all types of Json values" in {

      "null" in {
        (Path \ "n").read[JsValue, JsNull.type].validate(Json.obj("n" -> JsNull)) mustEqual(Success(JsNull))
        (Path \ "n").read[JsValue, JsNull.type].validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "null")))))
        (Path \ "n").read[JsValue, JsNull.type].validate(Json.obj("n" -> 4.8)) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "null")))))
      }

      "Int" in {
        (Path \ "n").read[JsValue, Int].validate(Json.obj("n" -> 4)) mustEqual(Success(4))
        (Path \ "n").read[JsValue, Int].validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Int")))))
        (Path \ "n").read[JsValue, Int].validate(Json.obj("n" -> 4.8)) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Int")))))
        (Path \ "n" \ "o").read[JsValue, Int].validate(Json.obj("n" -> Json.obj("o" -> 4))) mustEqual(Success(4))
        (Path \ "n" \ "o").read[JsValue, Int].validate(Json.obj("n" -> Json.obj("o" -> "foo"))) mustEqual(Failure(Seq(Path \ "n" \ "o" -> Seq(ValidationError("validation.type-mismatch", "Int")))))

        (Path \ "n" \ "o" \ "p" ).read[JsValue, Int].validate(Json.obj("n" -> Json.obj("o" -> Json.obj("p" -> 4)))) mustEqual(Success(4))
        (Path \ "n" \ "o" \ "p").read[JsValue, Int].validate(Json.obj("n" -> Json.obj("o" -> Json.obj("p" -> "foo")))) mustEqual(Failure(Seq(Path \ "n" \ "o" \ "p" -> Seq(ValidationError("validation.type-mismatch", "Int")))))

        val errPath = Path \ "foo"
        val error = Failure(Seq(errPath -> Seq(ValidationError("validation.required"))))
        errPath.read[JsValue, Int].validate(Json.obj("n" -> 4)) mustEqual(error)
      }

      "Short" in {
        (Path \ "n").read[JsValue, Short].validate(Json.obj("n" -> 4)) mustEqual(Success(4))
        (Path \ "n").read[JsValue, Short].validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Short")))))
        (Path \ "n").read[JsValue, Short].validate(Json.obj("n" -> 4.8)) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Short")))))
      }

      "Long" in {
        (Path \ "n").read[JsValue, Long].validate(Json.obj("n" -> 4)) mustEqual(Success(4))
        (Path \ "n").read[JsValue, Long].validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Long")))))
        (Path \ "n").read[JsValue, Long].validate(Json.obj("n" -> 4.8)) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Long")))))
      }

      "Float" in {
        (Path \ "n").read[JsValue, Float].validate(Json.obj("n" -> 4)) mustEqual(Success(4))
        (Path \ "n").read[JsValue, Float].validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Float")))))
        (Path \ "n").read[JsValue, Float].validate(Json.obj("n" -> 4.8)) mustEqual(Success(4.8F))
      }

      "Double" in {
        (Path \ "n").read[JsValue, Double].validate(Json.obj("n" -> 4)) mustEqual(Success(4))
        (Path \ "n").read[JsValue, Double].validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Double")))))
        (Path \ "n").read[JsValue, Double].validate(Json.obj("n" -> 4.8)) mustEqual(Success(4.8))
      }

      "java BigDecimal" in {
        import java.math.{ BigDecimal => jBigDecimal }
        (Path \ "n").read[JsValue, jBigDecimal].validate(Json.obj("n" -> 4)) mustEqual(Success(new jBigDecimal("4")))
        (Path \ "n").read[JsValue, jBigDecimal].validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "BigDecimal")))))
        (Path \ "n").read[JsValue, jBigDecimal].validate(Json.obj("n" -> 4.8)) mustEqual(Success(new jBigDecimal("4.8")))
      }

      "scala BigDecimal" in {
        (Path \ "n").read[JsValue, BigDecimal].validate(Json.obj("n" -> 4)) mustEqual(Success(BigDecimal(4)))
        (Path \ "n").read[JsValue, BigDecimal].validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "BigDecimal")))))
        (Path \ "n").read[JsValue, BigDecimal].validate(Json.obj("n" -> 4.8)) mustEqual(Success(BigDecimal(4.8)))
      }

      "date" in {
        import java.util.Date
        val f = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.FRANCE)
        (Path \ "n").read(date).validate(Json.obj("n" -> "1985-09-10")) mustEqual(Success(f.parse("1985-09-10")))
        (Path \ "n").read(date).validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.date", "yyyy-MM-dd")))))
      }

      "iso date" in {
        import java.util.Date
        val f = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.FRANCE)
        (Path \ "n").read(isoDate).validate(Json.obj("n" -> "1985-09-10T00:00:00+02:00")) mustEqual(Success(f.parse("1985-09-10")))
        (Path \ "n").read(isoDate).validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.iso8601")))))
      }

      "joda" in {
        import org.joda.time.DateTime
        val f = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.FRANCE)
        val dd = f.parse("1985-09-10")
        val jd = new DateTime(dd)

        "date" in {
          (Path \ "n").read(jodaDate).validate(Json.obj("n" -> "1985-09-10")) mustEqual(Success(jd))
          (Path \ "n").read(jodaDate).validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.expected.jodadate.format", "yyyy-MM-dd")))))
        }

        "time" in {
          (Path \ "n").read(jodaTime).validate(Json.obj("n" -> dd.getTime)) mustEqual(Success(jd))
          (Path \ "n").read(jodaDate).validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.expected.jodadate.format", "yyyy-MM-dd")))))
        }

        "local date" in {
          import org.joda.time.LocalDate
          val ld = new LocalDate()
          (Path \ "n").read(jodaLocalDate).validate(Json.obj("n" -> ld.toString())) mustEqual(Success(ld))
          (Path \ "n").read(jodaLocalDate).validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.expected.jodadate.format", "")))))
        }
      }

      "sql date" in {
        import java.util.Date
        val f = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.FRANCE)
        val dd = f.parse("1985-09-10")
        val ds = new java.sql.Date(dd.getTime())
        (Path \ "n").read(sqlDate).validate(Json.obj("n" -> "1985-09-10")) mustEqual(Success(ds))
      }

      "Boolean" in {
        (Path \ "n").read[JsValue, Boolean].validate(Json.obj("n" -> true)) mustEqual(Success(true))
        (Path \ "n").read[JsValue, Boolean].validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Boolean")))))
      }

      "String" in {
        (Path \ "n").read[JsValue, String].validate(Json.obj("n" -> "foo")) mustEqual(Success("foo"))
        (Path \ "n").read[JsValue, String].validate(Json.obj("n" -> 42)) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "String")))))
        (Path \ "n").read[JsValue, String].validate(Json.obj("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "String")))))
        (Path \ "o").read[JsValue, String].validate(Json.obj("o" -> Json.obj("n" -> "foo"))) mustEqual(Failure(Seq(Path \ "o" -> Seq(ValidationError("validation.type-mismatch", "String")))))
      }

      "JsObject" in {
        (Path \ "o").read[JsValue, JsObject].validate(Json.obj("o" -> Json.obj("n" -> "foo"))) mustEqual(Success(JsObject(Seq("n" -> JsString("foo")))))
        (Path \ "n").read[JsValue, JsObject].validate(Json.obj("n" -> 42)) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Object")))))
        (Path \ "n").read[JsValue, JsObject].validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Object")))))
        (Path \ "n").read[JsValue, JsObject].validate(Json.obj("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Object")))))
      }

      "JsString" in {
        (Path \ "n").read[JsValue, JsString].validate(Json.obj("n" -> "foo")) mustEqual(Success(JsString("foo")))
        (Path \ "n").read[JsValue, JsString].validate(Json.obj("n" -> 42)) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "String")))))
      }

      "JsNumber" in {
        (Path \ "n").read[JsValue, JsNumber].validate(Json.obj("n" -> 4)) mustEqual(Success(JsNumber(4)))
        (Path \ "n").read[JsValue, JsNumber].validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Number")))))
        (Path \ "n").read[JsValue, JsNumber].validate(Json.obj("n" -> 4.8)) mustEqual(Success(JsNumber(4.8)))
      }

      "JsBoolean" in {
        (Path \ "n").read[JsValue, JsBoolean].validate(Json.obj("n" -> true)) mustEqual(Success(JsBoolean(true)))
        (Path \ "n").read[JsValue, JsBoolean].validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Boolean")))))
      }

      "Option" in {
        (Path \ "n").read[JsValue, Option[Boolean]].validate(Json.obj("n" -> true)) mustEqual(Success(Some(true)))
        (Path \ "n").read[JsValue, Option[Boolean]].validate(Json.obj("n" -> JsNull)) mustEqual(Success(None))
        (Path \ "n").read[JsValue, Option[Boolean]].validate(Json.obj("foo" -> "bar")) mustEqual(Success(None))
        (Path \ "n").read[JsValue, Option[Boolean]].validate(Json.obj("n" -> "bar")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Boolean")))))
      }

      "Map[String, V]" in {
        import Rules.{ map => mm }
        (Path \ "n").read[JsValue, Map[String, String]].validate(Json.obj("n" -> Json.obj("foo" -> "bar"))) mustEqual(Success(Map("foo" -> "bar")))
        (Path \ "n").read[JsValue, Map[String, Int]].validate(Json.obj("n" -> Json.obj("foo" -> 4, "bar" -> 5))) mustEqual(Success(Map("foo" -> 4, "bar" -> 5)))
        (Path \ "x").read[JsValue, Map[String, Int]].validate(Json.obj("n" -> Json.obj("foo" -> 4, "bar" -> "frack"))) mustEqual(Failure(Seq(Path \ "x" -> Seq(ValidationError("validation.required")))))
        (Path \ "n").read[JsValue, Map[String, Int]].validate(Json.obj("n" -> Json.obj("foo" -> 4, "bar" -> "frack"))) mustEqual(Failure(Seq(Path \ "n" \ "bar" -> Seq(ValidationError("validation.type-mismatch", "Int")))))
      }

      "Traversable" in {
        implicitly[Rule[JsValue, Traversable[String]]]
        (Path \ "n").read[JsValue, Traversable[String]].validate(Json.obj("n" -> Seq("foo"))).get.toSeq must haveTheSameElementsAs(Seq("foo"))
        (Path \ "n").read[JsValue, Traversable[Int]].validate(Json.obj("n" -> Seq(1, 2, 3))).get.toSeq must haveTheSameElementsAs(Seq(1, 2, 3))
        (Path \ "n").read[JsValue, Traversable[String]].validate(Json.obj("n" -> "paf")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Array")))))
      }

      "Array" in {
        (Path \ "n").read[JsValue, Array[String]].validate(Json.obj("n" -> Seq("foo"))).get.toSeq must haveTheSameElementsAs(Seq("foo"))
        (Path \ "n").read[JsValue, Array[Int]].validate(Json.obj("n" -> Seq(1, 2, 3))).get.toSeq must haveTheSameElementsAs(Seq(1, 2, 3))
        (Path \ "n").read[JsValue, Array[String]].validate(Json.obj("n" -> "paf")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Array")))))
      }

      "Seq" in {
        (Path \ "n").read[JsValue, Seq[String]].validate(Json.obj("n" -> Seq("foo"))).get must haveTheSameElementsAs(Seq("foo"))
        (Path \ "n").read[JsValue, Seq[Int]].validate(Json.obj("n" -> Seq(1, 2, 3))).get must haveTheSameElementsAs(Seq(1, 2, 3))
        (Path \ "n").read[JsValue, Seq[String]].validate(Json.obj("n" -> "paf")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Array")))))
        (Path \ "n").read[JsValue, Seq[String]].validate(Json.parse("""{"n":["foo", 2]}""")) mustEqual(Failure(Seq(Path \ "n" \ 1 -> Seq(ValidationError("validation.type-mismatch", "String")))))
      }

    }

    "validate data" in {
      (Path \ "firstname").read(notEmpty).validate(valid) mustEqual(Success("Julien"))

      val p = (Path \ "informations" \ "label")
      p.read(notEmpty).validate(valid) mustEqual(Success("Personal"))
      p.read(notEmpty).validate(invalid) mustEqual(Failure(Seq(p -> Seq(ValidationError("validation.nonemptytext")))))
    }

    "validate optional" in {
      (Path \ "firstname").read[JsValue, Option[String]].validate(valid) mustEqual(Success(Some("Julien")))
      (Path \ "foobar").read[JsValue, Option[String]].validate(valid) mustEqual(Success(None))
    }

    "validate deep" in {
      val p = (Path \ "informations" \ "label")

      From[JsValue] { __ =>
        (__ \ "informations").read(
          (__ \ "label").read(notEmpty))
      }.validate(valid) mustEqual(Success("Personal"))

      From[JsValue] { __ =>
        (__ \ "informations").read(
          (__ \ "label").read(notEmpty))
      }.validate(invalid) mustEqual(Failure(Seq(p -> Seq(ValidationError("validation.nonemptytext")))))
    }

    "coerce type" in {
      (Path \ "age").read[JsValue, Int].validate(valid) mustEqual(Success(27))
      (Path \ "age").read(min(20)).validate(valid) mustEqual(Success(27))
      (Path \ "age").read(max(50)).validate(valid) mustEqual(Success(27))
      (Path \ "age").read(min(50)).validate(valid) mustEqual(Failure(Seq((Path \ "age") -> Seq(ValidationError("validation.min", 50)))))
      (Path \ "age").read(max(0)).validate(valid) mustEqual(Failure(Seq((Path \ "age") -> Seq(ValidationError("validation.max", 0)))))
      (Path \ "firstname").read[JsValue, Int].validate(valid) mustEqual(Failure(Seq((Path \ "firstname") -> Seq(ValidationError("validation.type-mismatch", "Int")))))
    }

    "compose constraints" in {
      val composed = notEmpty |+| minLength(3)
      (Path \ "firstname").read(composed).validate(valid) mustEqual(Success("Julien"))

      val p = Path \ "informations" \ "label"
      val err = Failure(Seq(p -> Seq(ValidationError("validation.nonemptytext"), ValidationError("validation.minLength", 3))))
      p.read(composed).validate(invalid) mustEqual(err)
    }

    "compose validations" in {
      import play.api.libs.functional.syntax._

      From[JsValue]{ __ =>
        ((__ \ "firstname").read(notEmpty) ~
         (__ \ "lastname").read(notEmpty)).tupled
      }.validate(valid) mustEqual Success("Julien" -> "Tournay")

      From[JsValue]{ __ =>
        ((__ \ "firstname").read(notEmpty) ~
         (__ \ "lastname").read(notEmpty) ~
         (__ \ "informations" \ "label").read(notEmpty)).tupled
      }.validate(invalid) mustEqual Failure(Seq((Path \ "informations" \ "label") -> Seq(ValidationError("validation.nonemptytext"))))
    }

    "lift validations to seq validations" in {
      (Path \ "foo").read(seq(notEmpty)).validate(Json.obj("foo" -> Seq("bar")))
        .get must haveTheSameElementsAs(Seq("bar"))

      From[JsValue]{ __ =>
        (__ \ "foo").read(
          (__ \ "foo").read(seq(notEmpty)))
      }.validate(Json.obj("foo" -> Json.obj("foo" -> Seq("bar"))))
        .get must haveTheSameElementsAs(Seq("bar"))

      (Path \ "n").read(seq(notEmpty))
        .validate(Json.parse("""{"n":["foo", ""]}""")) mustEqual(Failure(Seq(Path \ "n" \ 1 -> Seq(ValidationError("validation.nonemptytext")))))
    }

    "validate dependent fields" in {
      val v = Json.obj(
        "login" -> "Alice",
        "password" -> "s3cr3t",
        "verify" -> "s3cr3t")

      val i1 = Json.obj(
        "login" -> "Alice",
        "password" -> "s3cr3t",
        "verify" -> "")

      val i2 = Json.obj(
        "login" -> "Alice",
        "password" -> "s3cr3t",
        "verify" -> "bam")

      val passRule = From[JsValue] { __ =>
        ((__ \ "password").read(notEmpty) ~ (__ \ "verify").read(notEmpty))
          .tupled.compose(Rule.uncurry(Rules.equalTo[String]).repath(_ => (Path \ "verify")))
      }

      val rule = From[JsValue] { __ =>
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

      val b = Json.obj("name" -> "B", "foo" -> 4)
      val c = Json.obj("name" -> "C", "bar" -> 6)
      val e = Json.obj("name" -> "E", "eee" -> 6)

      val typeFailure = Failure(Seq(Path -> Seq(ValidationError("validation.unknownType"))))

      "by trying all possible Rules" in {
        val rb: Rule[JsValue, A] = From[JsValue]{ __ =>
          ((__ \ "name").read[String] ~ (__ \ "foo").read[Int])(B.apply _)
        }

        val rc: Rule[JsValue, A] = From[JsValue]{ __ =>
          ((__ \ "name").read[String] ~ (__ \ "bar").read[Int])(C.apply _)
        }

        val rule = rb orElse rc orElse Rule(_ => typeFailure)

        rule.validate(b) mustEqual(Success(B("B", 4)))
        rule.validate(c) mustEqual(Success(C("C", 6)))
        rule.validate(e) mustEqual(Failure(Seq(Path -> Seq(ValidationError("validation.unknownType")))))
      }

      "by dicriminating on fields" in {

        val rule = From[JsValue] { __ =>
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

      val validJson = Json.obj(
        "firstname" -> "Julien",
        "lastname" -> "Tournay",
        "age" -> 27,
        "informations" -> Seq(Json.obj(
          "label" -> "Personal",
          "email" -> "fakecontact@gmail.com",
          "phones" -> Seq("01.23.45.67.89", "98.76.54.32.10"))))

      val invalidJson = Json.obj(
        "firstname" -> "Julien",
        "lastname" -> "Tournay",
        "age" -> 27,
        "informations" -> Seq(Json.obj(
          "label" -> "",
          "email" -> "fakecontact@gmail.com",
          "phones" -> Seq("01.23.45.67.89", "98.76.54.32.10"))))

      val infoValidation = From[JsValue] { __ =>
         ((__ \ "label").read(notEmpty) ~
          (__ \ "email").read(option(email)) ~
          (__ \ "phones").read(seq(notEmpty))) (ContactInformation.apply _)
      }

      val contactValidation = From[JsValue] { __ =>
        ((__ \ "firstname").read(notEmpty) ~
         (__ \ "lastname").read(notEmpty) ~
         (__ \ "company").read[Option[String]] ~
         (__ \ "informations").read(seq(infoValidation))) (Contact.apply _)
      }

      val expected =
        Contact("Julien", "Tournay", None, Seq(
          ContactInformation("Personal", Some("fakecontact@gmail.com"), List("01.23.45.67.89", "98.76.54.32.10"))))

      contactValidation.validate(validJson) mustEqual(Success(expected))
      contactValidation.validate(invalidJson) mustEqual(Failure(Seq(
        (Path \ "informations" \ 0 \"label") -> Seq(ValidationError("validation.nonemptytext")))))
    }

  }
}
