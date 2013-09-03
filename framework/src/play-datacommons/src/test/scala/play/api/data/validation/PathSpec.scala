package play.api.data.mapping

import org.specs2.mutable._
import scala.util.control.Exception._
import play.api.libs.functional._
import play.api.libs.functional.syntax._

object PathSpec extends Specification {

  "Path" should {
    "be compareable" in {
      (Path \ "foo" \ "bar") must equalTo((Path \ "foo" \ "bar"))
      (Path \ "foo" \ "bar").hashCode must equalTo((Path \ "foo" \ "bar").hashCode)
      (Path \ "foo" \ "bar") must not equalTo((Path \ "foo"))
      (Path \ "foo" \ "bar").hashCode must not equalTo((Path \ "foo").hashCode)
    }

    "compose" in {
      val c = (Path \ "foo" \ "bar") compose (Path \ "baz")
      val c2 = (Path \ "foo" \ "bar") ++ (Path \ "baz")
      c must equalTo(Path \ "foo" \ "bar" \ "baz")
      c2 must equalTo(Path \ "foo" \ "bar" \ "baz")
    }

    "have deconstructors" in {
      val path = Path \ "foo" \ "bar" \ "baz"

      val (h \: t) = path
      h must equalTo(Path \ "foo")
      t must equalTo(Path \ "bar" \ "baz")

      val (h1 \: h2 \: t2) = path
      h1 must equalTo(Path \ "foo")
      h2 must equalTo(Path \ "bar")
      t2 must equalTo(Path \ "baz")
    }

    import Rules._
    val valid: M = Map(
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
      val v = string compose notEmpty
      (Path \ "firstname").read(v).validate(valid) mustEqual(Success("Julien"))

      val errPath = Path \ "foo"
      val error = Failure(Seq(errPath -> Seq(ValidationError("validation.required"))))
      errPath.read(string).validate(invalid) mustEqual(error)
    }

    "ingore values" in {
      val v = string compose notEmpty
      val r = ((Path \ "firstname").read(v) ~ (Path \ "test").read(ignored(42))).tupled
      r.validate(valid) mustEqual(Success("Julien" -> 42))
    }

    "support primitives types" in {

      "Int" in {
        (Path \ "n").read(int).validate(Map("n" -> Seq("4"))) mustEqual(Success(4))
        (Path \ "n").read(int).validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Int")))))
        (Path \ "n").read(int).validate(Map("n" -> Seq("4.8"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Int")))))
        (Path \ "n" \ "o").read(int).validate(Map("n.o" -> Seq("4"))) mustEqual(Success(4))
        (Path \ "n" \ "o").read(int).validate(Map("n.o" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" \ "o" -> Seq(ValidationError("validation.type-mismatch", "Int")))))

        (Path \ "n" \ "o" \ "p").read(int).validate(Map("n.o.p" -> Seq("4"))) mustEqual(Success(4))
        (Path \ "n" \ "o" \ "p").read(int).validate(Map("n.o.p" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" \ "o" \ "p" -> Seq(ValidationError("validation.type-mismatch", "Int")))))

        val errPath = Path \ "foo"
        val error = Failure(Seq(errPath -> Seq(ValidationError("validation.required"))))
        errPath.read(int).validate(Map("n" -> Seq("4"))) mustEqual(error)
      }


      "Short" in {
        (Path \ "n").read(short).validate(Map("n" -> Seq("4"))) mustEqual(Success(4))
        (Path \ "n").read(short).validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Short")))))
        (Path \ "n").read(short).validate(Map("n" -> Seq("4.8"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Short")))))
      }

      "Long" in {
        (Path \ "n").read(long).validate(Map("n" -> Seq("4"))) mustEqual(Success(4))
        (Path \ "n").read(long).validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Long")))))
        (Path \ "n").read(long).validate(Map("n" -> Seq("4.8"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Long")))))
      }

      "Float" in {
        (Path \ "n").read(float).validate(Map("n" -> Seq("4"))) mustEqual(Success(4))
        (Path \ "n").read(float).validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Float")))))
        (Path \ "n").read(float).validate(Map("n" -> Seq("4.8"))) mustEqual(Success(4.8F))
      }

      "Double" in {
        (Path \ "n").read(double).validate(Map("n" -> Seq("4"))) mustEqual(Success(4))
        (Path \ "n").read(double).validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Double")))))
        (Path \ "n").read(double).validate(Map("n" -> Seq("4.8"))) mustEqual(Success(4.8))
      }

      "java BigDecimal" in {
        import java.math.{ BigDecimal => jBigDecimal }
        (Path \ "n").read(javaBigDecimal).validate(Map("n" -> Seq("4"))) mustEqual(Success(new jBigDecimal("4")))
        (Path \ "n").read(javaBigDecimal).validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "BigDecimal")))))
        (Path \ "n").read(javaBigDecimal).validate(Map("n" -> Seq("4.8"))) mustEqual(Success(new jBigDecimal("4.8")))
      }

      "scala BigDecimal" in {
        (Path \ "n").read(bigDecimal).validate(Map("n" -> Seq("4"))) mustEqual(Success(BigDecimal(4)))
        (Path \ "n").read(bigDecimal).validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "BigDecimal")))))
        (Path \ "n").read(bigDecimal).validate(Map("n" -> Seq("4.8"))) mustEqual(Success(BigDecimal(4.8)))
      }

      "date" in { skipped }
      "joda date" in { skipped }
      "joda local data" in { skipped }
      "sql date" in { skipped }

      "Boolean" in {
        (Path \ "n").read(boolean).validate(Map("n" -> Seq("true"))) mustEqual(Success(true))
        (Path \ "n").read(boolean).validate(Map("n" -> Seq("TRUE"))) mustEqual(Success(true))
        (Path \ "n").read(boolean).validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Boolean")))))
      }

      "String" in {
        (Path \ "n").read(string).validate(Map("n" -> Seq("foo"))) mustEqual(Success("foo"))
        (Path \ "o").read(string).validate(Map("o.n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "o" -> Seq(ValidationError("validation.required")))))
      }

      "Option" in {
        (Path \ "n").read(option(boolean)).validate(Map("n" -> Seq("true"))) mustEqual(Success(Some(true)))
        (Path \ "n").read(option(boolean)).validate(Map("foo" -> Seq("bar"))) mustEqual(Success(None))
        (Path \ "n").read(option(boolean)).validate(Map("n" -> Seq("bar"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Boolean")))))
      }

      "Map[String, Seq[V]]" in {
        (Path \ "n").read(Rules.map(seq(string))).validate(Map("n.foo" -> Seq("bar"))) mustEqual(Success(Map("foo" -> Seq("bar"))))
        (Path \ "n").read(Rules.map(seq(int))).validate(Map("n.foo" -> Seq("4"), "n.bar" -> Seq("5"))) mustEqual(Success(Map("foo" -> Seq(4), "bar" -> Seq(5))))
        (Path \ "n").read(Rules.map(seq(int))).validate(Map("n.foo" -> Seq("4"), "n.bar" -> Seq("frack"))) mustEqual(Failure(Seq(Path \ "n" \ "bar" \ 0 -> Seq(ValidationError("validation.type-mismatch", "Int")))))
      }

      "Traversable" in {
        (Path \ "n").read(Rules.traversable(string)).validate(Map("n" -> Seq("foo"))).get.toSeq must haveTheSameElementsAs(Seq("foo"))
        (Path \ "n").read(Rules.traversable(int)).validate(Map("n" -> Seq("1", "2", "3"))).get.toSeq must haveTheSameElementsAs(Seq(1, 2, 3))
        (Path \ "n").read(Rules.traversable(int)).validate(Map("n" -> Seq("1", "paf"))) mustEqual(Failure(Seq(Path \ "n" \ 1 -> Seq(ValidationError("validation.type-mismatch", "Int")))))
      }

      "Array" in {
        (Path \ "n").read(array(string)).validate(Map("n" -> Seq("foo"))).get.toSeq must haveTheSameElementsAs(Seq("foo"))
        (Path \ "n").read(array(int)).validate(Map("n" -> Seq("1", "2", "3"))).get.toSeq must haveTheSameElementsAs(Seq(1, 2, 3))
        (Path \ "n").read(array(int)).validate(Map("n" -> Seq("1", "paf"))) mustEqual(Failure(Seq(Path \ "n" \ 1 -> Seq(ValidationError("validation.type-mismatch", "Int")))))
      }

      "Seq" in {
        (Path \ "n").read(seq(string)).validate(Map("n" -> Seq("foo"))).get must haveTheSameElementsAs(Seq("foo"))
        (Path \ "n").read(seq(int)).validate(Map("n" -> Seq("1", "2", "3"))).get must haveTheSameElementsAs(Seq(1, 2, 3))
        (Path \ "n").read(seq(int)).validate(Map("n" -> Seq("1", "paf"))) mustEqual(Failure(Seq(Path \ "n" \ 1 -> Seq(ValidationError("validation.type-mismatch", "Int")))))
      }
    }

    "validate data" in {
      (Path \ "firstname").read(string compose notEmpty).validate(valid) mustEqual(Success("Julien"))

      val p = (Path \ "informations" \ "label")
      p.read(string compose notEmpty).validate(valid) mustEqual(Success("Personal"))
      p.read(string compose notEmpty).validate(invalid) mustEqual(Failure(Seq(p -> Seq(ValidationError("validation.nonemptytext")))))
    }

    "validate seq" in {
      (Path \ "firstname").read(seq(string)).validate(valid) mustEqual(Success(Seq("Julien")))
      (Path \ "foobar").read(seq(string)).validate(valid) mustEqual(Failure(Seq(Path \ "foobar" -> Seq(ValidationError("validation.required")))))
    }

    "validate optional" in {
      (Path \ "firstname").read(option(string)).validate(valid) mustEqual(Success(Some("Julien")))
      (Path \ "firstname").read(option(int)).validate(valid) mustEqual(Failure(Seq(Path \ "firstname" -> Seq(ValidationError("validation.type-mismatch", "Int")))))
      (Path \ "foobar").read(option(string)).validate(valid) mustEqual(Success(None))
    }

    "validate deep" in {
      val p = (Path \ "informations" \ "label")

      (Path \ "informations").read(
        (Path \ "label").read(string compose notEmpty)).validate(valid) mustEqual(Success("Personal"))

      (Path \ "informations").read(
        (Path \ "label").read(string compose notEmpty)).validate(invalid) mustEqual(Failure(Seq(p -> Seq(ValidationError("validation.nonemptytext")))))
    }

    "coerce type" in {
      (Path \ "age").read(int).validate(valid) mustEqual(Success(27))
      (Path \ "firstname").read(int).validate(valid) mustEqual(Failure(Seq((Path \ "firstname") -> Seq(ValidationError("validation.type-mismatch", "Int")))))
    }

    "compose constraints" in {
      // TODO: create MonoidOps
      val composed = string compose monoidConstraint.append(notEmpty, minLength(3))
      (Path \ "firstname").read(composed).validate(valid) mustEqual(Success("Julien"))

      val p = Path \ "informations" \ "label"
      val err = Failure(Seq(p -> Seq(ValidationError("validation.nonemptytext"), ValidationError("validation.minLength", 3))))
      p.read(composed).validate(invalid) mustEqual(err)
    }

    "compose validations" in {
      import play.api.libs.functional.syntax._

      ((Path \ "firstname").read(string compose notEmpty) ~
       (Path \ "lastname").read(string compose notEmpty)){ _ -> _ }
         .validate(valid) mustEqual Success("Julien" -> "Tournay")

      ((Path \ "firstname").read(string compose notEmpty) ~
      (Path \ "lastname").read(string compose notEmpty) ~
      (Path \ "informations" \ "label").read(string compose notEmpty)){ (_, _, _) }
       .validate(invalid) mustEqual Failure(Seq((Path \ "informations" \ "label") -> Seq(ValidationError("validation.nonemptytext"))))
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

      val nonEmptyText = string compose notEmpty

      val infoValidation =
       ((Path \ "label").read(nonEmptyText) ~
        (Path \ "email").read(option(string compose email)) ~
        (Path \ "phones").read(seq(nonEmptyText))) (ContactInformation.apply _)

      val contactValidation =
       ((Path \ "firstname").read(nonEmptyText) ~
        (Path \ "lastname").read(nonEmptyText) ~
        (Path \ "company").read(option(string)) ~
        (Path \ "informations").read[M, Seq[M], Seq[ContactInformation]](seq(infoValidation))) (Contact.apply _)

      val expected =
        Contact("Julien", "Tournay", None, Seq(
          ContactInformation("Personal", Some("fakecontact@gmail.com"), List("01.23.45.67.89", "98.76.54.32.10"))))

      // contactValidation.validate(validM) mustEqual(Success(expected))
      contactValidation.validate(validWithPhones) mustEqual(Success(expected))
      // contactValidation.validate(invalidM) mustEqual(Failure(Seq(
        // (Path \ "informations" \ 0 \"label") -> Seq(ValidationError("validation.nonemptytext")))))
    }
  }
}
