package play.api.libs.json

import org.specs2.mutable._
import scala.util.control.Exception._
import play.api.data.mapping._

import scala.language.reflectiveCalls

object RulesSpec extends Specification {

  "Json Rules" should {
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
      val error = Failure(Seq(errPath -> Seq(ValidationError("error.required"))))
      errPath.read[JsValue, String].validate(invalid) mustEqual(error)
    }

    "support checked" in {
      val js = Json.obj("issmth" -> true)
      val p = Path \ "issmth"
      p.from[JsValue](checked).validate(js) mustEqual(Success(true))
      p.from[JsValue](checked).validate(Json.obj()) mustEqual(Failure(Seq(Path \ "issmth" -> Seq(ValidationError("error.required")))))
      p.from[JsValue](checked).validate(Json.obj("issmth" -> false)) mustEqual(Failure(Seq(Path \ "issmth" -> Seq(ValidationError("error.equals", true)))))
    }

    "support all types of Json values" in {

      "null" in {
        (Path \ "n").read[JsValue, JsNull.type].validate(Json.obj("n" -> JsNull)) mustEqual(Success(JsNull))
        (Path \ "n").read[JsValue, JsNull.type].validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.invalid", "null")))))
        (Path \ "n").read[JsValue, JsNull.type].validate(Json.obj("n" -> 4.8)) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.invalid", "null")))))
      }

      "Int" in {
        (Path \ "n").read[JsValue, Int].validate(Json.obj("n" -> 4)) mustEqual(Success(4))
        (Path \ "n").read[JsValue, Int].validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.invalid", "Int")))))
        (Path \ "n").read[JsValue, Int].validate(Json.obj("n" -> 4.8)) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.invalid", "Int")))))
        (Path \ "n" \ "o").read[JsValue, Int].validate(Json.obj("n" -> Json.obj("o" -> 4))) mustEqual(Success(4))
        (Path \ "n" \ "o").read[JsValue, Int].validate(Json.obj("n" -> Json.obj("o" -> "foo"))) mustEqual(Failure(Seq(Path \ "n" \ "o" -> Seq(ValidationError("error.invalid", "Int")))))

        (Path \ "n" \ "o" \ "p" ).read[JsValue, Int].validate(Json.obj("n" -> Json.obj("o" -> Json.obj("p" -> 4)))) mustEqual(Success(4))
        (Path \ "n" \ "o" \ "p").read[JsValue, Int].validate(Json.obj("n" -> Json.obj("o" -> Json.obj("p" -> "foo")))) mustEqual(Failure(Seq(Path \ "n" \ "o" \ "p" -> Seq(ValidationError("error.invalid", "Int")))))

        val errPath = Path \ "foo"
        val error = Failure(Seq(errPath -> Seq(ValidationError("error.required"))))
        errPath.read[JsValue, Int].validate(Json.obj("n" -> 4)) mustEqual(error)
      }

      "Short" in {
        (Path \ "n").read[JsValue, Short].validate(Json.obj("n" -> 4)) mustEqual(Success(4))
        (Path \ "n").read[JsValue, Short].validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.invalid", "Short")))))
        (Path \ "n").read[JsValue, Short].validate(Json.obj("n" -> 4.8)) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.invalid", "Short")))))
      }

      "Long" in {
        (Path \ "n").read[JsValue, Long].validate(Json.obj("n" -> 4)) mustEqual(Success(4))
        (Path \ "n").read[JsValue, Long].validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.invalid", "Long")))))
        (Path \ "n").read[JsValue, Long].validate(Json.obj("n" -> 4.8)) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.invalid", "Long")))))
      }

      "Float" in {
        (Path \ "n").read[JsValue, Float].validate(Json.obj("n" -> 4)) mustEqual(Success(4))
        (Path \ "n").read[JsValue, Float].validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.invalid", "Float")))))
        (Path \ "n").read[JsValue, Float].validate(Json.obj("n" -> 4.8)) mustEqual(Success(4.8F))
      }

      "Double" in {
        (Path \ "n").read[JsValue, Double].validate(Json.obj("n" -> 4)) mustEqual(Success(4))
        (Path \ "n").read[JsValue, Double].validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.invalid", "Double")))))
        (Path \ "n").read[JsValue, Double].validate(Json.obj("n" -> 4.8)) mustEqual(Success(4.8))
      }

      "java BigDecimal" in {
        import java.math.{ BigDecimal => jBigDecimal }
        (Path \ "n").read[JsValue, jBigDecimal].validate(Json.obj("n" -> 4)) mustEqual(Success(new jBigDecimal("4")))
        (Path \ "n").read[JsValue, jBigDecimal].validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.invalid", "BigDecimal")))))
        (Path \ "n").read[JsValue, jBigDecimal].validate(Json.obj("n" -> 4.8)) mustEqual(Success(new jBigDecimal("4.8")))
      }

      "scala BigDecimal" in {
        (Path \ "n").read[JsValue, BigDecimal].validate(Json.obj("n" -> 4)) mustEqual(Success(BigDecimal(4)))
        (Path \ "n").read[JsValue, BigDecimal].validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.invalid", "BigDecimal")))))
        (Path \ "n").read[JsValue, BigDecimal].validate(Json.obj("n" -> 4.8)) mustEqual(Success(BigDecimal(4.8)))
      }

      "date" in {
        import java.util.Date
        val f = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.FRANCE)
        (Path \ "n").from[JsValue](date).validate(Json.obj("n" -> "1985-09-10")) mustEqual(Success(f.parse("1985-09-10")))
        (Path \ "n").from[JsValue](date).validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.expected.date", "yyyy-MM-dd")))))
      }

      "iso date" in {
        skipped("Can't test on CI")
        import java.util.Date
        val f = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.FRANCE)
        (Path \ "n").from[JsValue](isoDate).validate(Json.obj("n" -> "1985-09-10T00:00:00+02:00")) mustEqual(Success(f.parse("1985-09-10")))
        (Path \ "n").from[JsValue](isoDate).validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.expected.date.isoformat")))))
      }

      "joda" in {
        import org.joda.time.DateTime
        val f = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.FRANCE)
        val dd = f.parse("1985-09-10")
        val jd = new DateTime(dd)

        "date" in {
          (Path \ "n").from[JsValue](jodaDate).validate(Json.obj("n" -> "1985-09-10")) mustEqual(Success(jd))
          (Path \ "n").from[JsValue](jodaDate).validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")))))
        }

        "time" in {
          (Path \ "n").from[JsValue](jodaTime).validate(Json.obj("n" -> dd.getTime)) mustEqual(Success(jd))
          (Path \ "n").from[JsValue](jodaDate).validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")))))
        }

        "local date" in {
          import org.joda.time.LocalDate
          val ld = new LocalDate()
          (Path \ "n").from[JsValue](jodaLocalDate).validate(Json.obj("n" -> ld.toString())) mustEqual(Success(ld))
          (Path \ "n").from[JsValue](jodaLocalDate).validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.expected.jodadate.format", "")))))
        }
      }

      "sql date" in {
        import java.util.Date
        val f = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.FRANCE)
        val dd = f.parse("1985-09-10")
        val ds = new java.sql.Date(dd.getTime())
        (Path \ "n").from[JsValue](sqlDate).validate(Json.obj("n" -> "1985-09-10")) mustEqual(Success(ds))
      }

      "Boolean" in {
        (Path \ "n").read[JsValue, Boolean].validate(Json.obj("n" -> true)) mustEqual(Success(true))
        (Path \ "n").read[JsValue, Boolean].validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.invalid", "Boolean")))))
      }

      "String" in {
        (Path \ "n").read[JsValue, String].validate(Json.obj("n" -> "foo")) mustEqual(Success("foo"))
        (Path \ "n").read[JsValue, String].validate(Json.obj("n" -> 42)) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.invalid", "String")))))
        (Path \ "n").read[JsValue, String].validate(Json.obj("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.invalid", "String")))))
        (Path \ "o").read[JsValue, String].validate(Json.obj("o" -> Json.obj("n" -> "foo"))) mustEqual(Failure(Seq(Path \ "o" -> Seq(ValidationError("error.invalid", "String")))))
      }

      "JsObject" in {
        (Path \ "o").read[JsValue, JsObject].validate(Json.obj("o" -> Json.obj("n" -> "foo"))) mustEqual(Success(JsObject(Seq("n" -> JsString("foo")))))
        (Path \ "n").read[JsValue, JsObject].validate(Json.obj("n" -> 42)) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.invalid", "Object")))))
        (Path \ "n").read[JsValue, JsObject].validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.invalid", "Object")))))
        (Path \ "n").read[JsValue, JsObject].validate(Json.obj("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.invalid", "Object")))))
      }

      "JsString" in {
        (Path \ "n").read[JsValue, JsString].validate(Json.obj("n" -> "foo")) mustEqual(Success(JsString("foo")))
        (Path \ "n").read[JsValue, JsString].validate(Json.obj("n" -> 42)) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.invalid", "String")))))
      }

      "JsNumber" in {
        (Path \ "n").read[JsValue, JsNumber].validate(Json.obj("n" -> 4)) mustEqual(Success(JsNumber(4)))
        (Path \ "n").read[JsValue, JsNumber].validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.invalid", "Number")))))
        (Path \ "n").read[JsValue, JsNumber].validate(Json.obj("n" -> 4.8)) mustEqual(Success(JsNumber(4.8)))
      }

      "JsBoolean" in {
        (Path \ "n").read[JsValue, JsBoolean].validate(Json.obj("n" -> true)) mustEqual(Success(JsBoolean(true)))
        (Path \ "n").read[JsValue, JsBoolean].validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.invalid", "Boolean")))))
      }

      "Option" in {
        (Path \ "n").read[JsValue, Option[Boolean]].validate(Json.obj("n" -> true)) mustEqual(Success(Some(true)))
        (Path \ "n").read[JsValue, Option[Boolean]].validate(Json.obj("n" -> JsNull)) mustEqual(Success(None))
        (Path \ "n").read[JsValue, Option[Boolean]].validate(Json.obj("foo" -> "bar")) mustEqual(Success(None))
        (Path \ "n").read[JsValue, Option[Boolean]].validate(Json.obj("n" -> "bar")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.invalid", "Boolean")))))
      }

      "Map[String, V]" in {
        import Rules.{ map => mm }
        (Path \ "n").read[JsValue, Map[String, String]].validate(Json.obj("n" -> Json.obj("foo" -> "bar"))) mustEqual(Success(Map("foo" -> "bar")))
        (Path \ "n").read[JsValue, Map[String, Int]].validate(Json.obj("n" -> Json.obj("foo" -> 4, "bar" -> 5))) mustEqual(Success(Map("foo" -> 4, "bar" -> 5)))
        (Path \ "x").read[JsValue, Map[String, Int]].validate(Json.obj("n" -> Json.obj("foo" -> 4, "bar" -> "frack"))) mustEqual(Failure(Seq(Path \ "x" -> Seq(ValidationError("error.required")))))
        (Path \ "n").read[JsValue, Map[String, Int]].validate(Json.obj("n" -> Json.obj("foo" -> 4, "bar" -> "frack"))) mustEqual(Failure(Seq(Path \ "n" \ "bar" -> Seq(ValidationError("error.invalid", "Int")))))
      }

      "Traversable" in {

        (Path \ "n").read[JsValue, Traversable[String]].validate(Json.obj("n" -> Seq("foo"))).get.toSeq must haveTheSameElementsAs(Seq("foo"))
        (Path \ "n").read[JsValue, Traversable[Int]].validate(Json.obj("n" -> Seq(1, 2, 3))).get.toSeq must haveTheSameElementsAs(Seq(1, 2, 3))
        (Path \ "n").read[JsValue, Traversable[String]].validate(Json.obj("n" -> "paf")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.invalid", "Array")))))
      }

      "Array" in {
        (Path \ "n").read[JsValue, Array[String]].validate(Json.obj("n" -> Seq("foo"))).get.toSeq must haveTheSameElementsAs(Seq("foo"))
        (Path \ "n").read[JsValue, Array[Int]].validate(Json.obj("n" -> Seq(1, 2, 3))).get.toSeq must haveTheSameElementsAs(Seq(1, 2, 3))
        (Path \ "n").read[JsValue, Array[String]].validate(Json.obj("n" -> "paf")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.invalid", "Array")))))
      }

      "Seq" in {
        (Path \ "n").read[JsValue, Seq[String]].validate(Json.obj("n" -> Seq("foo"))).get must haveTheSameElementsAs(Seq("foo"))
        (Path \ "n").read[JsValue, Seq[Int]].validate(Json.obj("n" -> Seq(1, 2, 3))).get must haveTheSameElementsAs(Seq(1, 2, 3))
        (Path \ "n").read[JsValue, Seq[String]].validate(Json.obj("n" -> "paf")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.invalid", "Array")))))
        (Path \ "n").read[JsValue, Seq[String]].validate(Json.parse("""{"n":["foo", 2]}""")) mustEqual(Failure(Seq(Path \ "n" \ 1 -> Seq(ValidationError("error.invalid", "String")))))
      }

    }

    "validate data" in {
      (Path \ "firstname").from[JsValue](notEmpty).validate(valid) mustEqual(Success("Julien"))

      val p = (Path \ "informations" \ "label")
      p.from[JsValue](notEmpty).validate(valid) mustEqual(Success("Personal"))
      p.from[JsValue](notEmpty).validate(invalid) mustEqual(Failure(Seq(p -> Seq(ValidationError("error.required")))))
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
      }.validate(invalid) mustEqual(Failure(Seq(p -> Seq(ValidationError("error.required")))))
    }

    "coerce type" in {
      (Path \ "age").read[JsValue, Int].validate(valid) mustEqual(Success(27))
      (Path \ "age").from[JsValue](min(20)).validate(valid) mustEqual(Success(27))
      (Path \ "age").from[JsValue](max(50)).validate(valid) mustEqual(Success(27))
      (Path \ "age").from[JsValue](min(50)).validate(valid) mustEqual(Failure(Seq((Path \ "age") -> Seq(ValidationError("error.min", 50)))))
      (Path \ "age").from[JsValue](max(0)).validate(valid) mustEqual(Failure(Seq((Path \ "age") -> Seq(ValidationError("error.max", 0)))))
      (Path \ "firstname").read[JsValue, Int].validate(valid) mustEqual(Failure(Seq((Path \ "firstname") -> Seq(ValidationError("error.invalid", "Int")))))
    }

    "compose constraints" in {
      val composed = notEmpty |+| minLength(3)
      (Path \ "firstname").from[JsValue](composed).validate(valid) mustEqual(Success("Julien"))

      val p = Path \ "informations" \ "label"
      val err = Failure(Seq(p -> Seq(ValidationError("error.required"), ValidationError("error.minLength", 3))))
      p.from[JsValue](composed).validate(invalid) mustEqual(err)
    }

    "compose validations" in {
      From[JsValue]{ __ =>
        ((__ \ "firstname").read(notEmpty) ~
         (__ \ "lastname").read(notEmpty)).tupled
      }.validate(valid) mustEqual Success("Julien" -> "Tournay")

      From[JsValue]{ __ =>
        ((__ \ "firstname").read(notEmpty) ~
         (__ \ "lastname").read(notEmpty) ~
         (__ \ "informations" \ "label").read(notEmpty)).tupled
      }.validate(invalid) mustEqual Failure(Seq((Path \ "informations" \ "label") -> Seq(ValidationError("error.required"))))
    }

    "lift validations to seq validations" in {
      (Path \ "foo").from[JsValue](seq(notEmpty)).validate(Json.obj("foo" -> Seq("bar")))
        .get must haveTheSameElementsAs(Seq("bar"))

      From[JsValue]{ __ =>
        (__ \ "foo").read(
          (__ \ "foo").read(seq(notEmpty)))
      }.validate(Json.obj("foo" -> Json.obj("foo" -> Seq("bar"))))
        .get must haveTheSameElementsAs(Seq("bar"))

      (Path \ "n").from[JsValue](seq(notEmpty))
        .validate(Json.parse("""{"n":["foo", ""]}""")) mustEqual(Failure(Seq(Path \ "n" \ 1 -> Seq(ValidationError("error.required")))))
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
      rule.validate(i1).mustEqual(Failure(Seq(Path \ "verify" -> Seq(ValidationError("error.required")))))
      rule.validate(i2).mustEqual(Failure(Seq(Path \ "verify" -> Seq(ValidationError("error.equals", "s3cr3t")))))
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
        (Path \ "informations" \ 0 \ "label") -> Seq(ValidationError("error.required")))))
    }

    "read recursive" in {
      case class RecUser(name: String, friends: Seq[RecUser] = Nil)
      val u = RecUser(
        "bob",
        Seq(RecUser("tom")))

      val m = Json.obj(
        "name" -> "bob",
        "friends" -> Seq(Json.obj("name" -> "tom", "friends" -> Seq[JsObject]())))

      case class User1(name: String, friend: Option[User1] = None)
      val u1 = User1("bob", Some(User1("tom")))
      val m1 = Json.obj(
        "name" -> "bob",
        "friend" -> Json.obj("name" -> "tom"))

      "using explicit notation" in {
        lazy val w: Rule[JsValue, RecUser] = From[JsValue]{ __ =>
          ((__ \ "name").read[String] ~
           (__ \ "friends").read(seq(w)))(RecUser.apply _)
        }
        w.validate(m) mustEqual Success(u)

        lazy val w2: Rule[JsValue, RecUser] =
          ((Path \ "name").read[JsValue, String] ~
           (Path \ "friends").from[JsValue](seq(w2)))(RecUser.apply _)
        w2.validate(m) mustEqual Success(u)

        lazy val w3: Rule[JsValue, User1] = From[JsValue]{ __ =>
          ((__ \ "name").read[String] ~
           (__ \ "friend").read(option(w3)))(User1.apply _)
        }
        w3.validate(m1) mustEqual Success(u1)
      }

      "using implicit notation" in {
        implicit lazy val w: Rule[JsValue, RecUser] = From[JsValue]{ __ =>
          ((__ \ "name").read[String] ~
           (__ \ "friends").read[Seq[RecUser]])(RecUser.apply _)
        }
        w.validate(m) mustEqual Success(u)

        implicit lazy val w3: Rule[JsValue, User1] = From[JsValue]{ __ =>
          ((__ \ "name").read[String] ~
           (__ \ "friend").read[Option[User1]])(User1.apply _)
        }
        w3.validate(m1) mustEqual Success(u1)
      }

    }

  }
}
