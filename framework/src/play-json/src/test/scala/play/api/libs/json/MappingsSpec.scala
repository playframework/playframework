package play.api.libs.json

import org.specs2.mutable._
import scala.util.control.Exception._
import play.api.libs.functional._
import play.api.libs.functional.syntax._
import play.api.data.validation._

object MappingsSpec extends Specification {

  "Json Mappings" should {
    import play.api.libs.json.Rules._

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
      (__ \ "firstname").read(string).validate(valid) mustEqual(Success("Julien"))
      val errPath = __ \ "foo"
      val error = Failure(Seq(errPath -> Seq(ValidationError("validation.required"))))
      errPath.read(string).validate(invalid) mustEqual(error)
    }

    "support all types of Json values" in {

      "Int" in {
        (__ \ "n").read(int).validate(Json.obj("n" -> 4)) mustEqual(Success(4))
        (__ \ "n").read(int).validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Int")))))
        (__ \ "n").read(int).validate(Json.obj("n" -> 4.8)) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Int")))))
        (__ \ "n" \ "o").read(int).validate(Json.obj("n" -> Json.obj("o" -> 4))) mustEqual(Success(4))
        (__ \ "n" \ "o").read(int).validate(Json.obj("n" -> Json.obj("o" -> "foo"))) mustEqual(Failure(Seq(__ \ "n" \ "o" -> Seq(ValidationError("validation.type-mismatch", "Int")))))

        (__ \ "n" \ "o" \ "p" ).read(int).validate(Json.obj("n" -> Json.obj("o" -> Json.obj("p" -> 4)))) mustEqual(Success(4))
        (__ \ "n" \ "o" \ "p").read(int).validate(Json.obj("n" -> Json.obj("o" -> Json.obj("p" -> "foo")))) mustEqual(Failure(Seq(__ \ "n" \ "o" \ "p" -> Seq(ValidationError("validation.type-mismatch", "Int")))))

        val errPath = __ \ "foo"
        val error = Failure(Seq(errPath -> Seq(ValidationError("validation.required"))))
        errPath.read(int).validate(Json.obj("n" -> 4)) mustEqual(error)
      }

      "Short" in {
        (__ \ "n").read(short).validate(Json.obj("n" -> 4)) mustEqual(Success(4))
        (__ \ "n").read(short).validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Short")))))
        (__ \ "n").read(short).validate(Json.obj("n" -> 4.8)) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Short")))))
      }

      "Long" in {
        (__ \ "n").read(long).validate(Json.obj("n" -> 4)) mustEqual(Success(4))
        (__ \ "n").read(long).validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Long")))))
        (__ \ "n").read(long).validate(Json.obj("n" -> 4.8)) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Long")))))
      }

      "Float" in {
        (__ \ "n").read(float).validate(Json.obj("n" -> 4)) mustEqual(Success(4))
        (__ \ "n").read(float).validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Float")))))
        (__ \ "n").read(float).validate(Json.obj("n" -> 4.8)) mustEqual(Success(4.8F))
      }

      "Double" in {
        (__ \ "n").read(double).validate(Json.obj("n" -> 4)) mustEqual(Success(4))
        (__ \ "n").read(double).validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Double")))))
        (__ \ "n").read(double).validate(Json.obj("n" -> 4.8)) mustEqual(Success(4.8))
      }

      "java BigDecimal" in {
        import java.math.{ BigDecimal => jBigDecimal }
        (__ \ "n").read(javaBigDecimal).validate(Json.obj("n" -> 4)) mustEqual(Success(new jBigDecimal("4")))
        (__ \ "n").read(javaBigDecimal).validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "BigDecimal")))))
        (__ \ "n").read(javaBigDecimal).validate(Json.obj("n" -> 4.8)) mustEqual(Success(new jBigDecimal("4.8")))
      }

      "scala BigDecimal" in {
        (__ \ "n").read(bigDecimal).validate(Json.obj("n" -> 4)) mustEqual(Success(BigDecimal(4)))
        (__ \ "n").read(bigDecimal).validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "BigDecimal")))))
        (__ \ "n").read(bigDecimal).validate(Json.obj("n" -> 4.8)) mustEqual(Success(BigDecimal(4.8)))
      }

      "date" in { skipped }
      "joda date" in { skipped }
      "joda local data" in { skipped }
      "sql date" in { skipped }

      "Boolean" in {
        (__ \ "n").read(boolean).validate(Json.obj("n" -> true)) mustEqual(Success(true))
        (__ \ "n").read(boolean).validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Boolean")))))
      }

      "String" in {
        (__ \ "n").read(string).validate(Json.obj("n" -> "foo")) mustEqual(Success("foo"))
        (__ \ "n").read(string).validate(Json.obj("n" -> 42)) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "String")))))
        (__ \ "n").read(string).validate(Json.obj("n" -> Seq("foo"))) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "String")))))
        (__ \ "o").read(string).validate(Json.obj("o" -> Json.obj("n" -> "foo"))) mustEqual(Failure(Seq(__ \ "o" -> Seq(ValidationError("validation.type-mismatch", "String")))))
      }

      "JsObject" in {
        (__ \ "o").read(jsObject).validate(Json.obj("o" -> Json.obj("n" -> "foo"))) mustEqual(Success(JsObject(Seq("n" -> JsString("foo")))))
        (__ \ "n").read(jsObject).validate(Json.obj("n" -> 42)) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Object")))))
        (__ \ "n").read(jsObject).validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Object")))))
        (__ \ "n").read(jsObject).validate(Json.obj("n" -> Seq("foo"))) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Object")))))
      }

      "JsString" in {
        (__ \ "n").read(jsString).validate(Json.obj("n" -> "foo")) mustEqual(Success(JsString("foo")))
        (__ \ "n").read(jsString).validate(Json.obj("n" -> 42)) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "String")))))
      }

      "JsNumber" in {
        (__ \ "n").read(jsNumber).validate(Json.obj("n" -> 4)) mustEqual(Success(JsNumber(4)))
        (__ \ "n").read(jsNumber).validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Number")))))
        (__ \ "n").read(jsNumber).validate(Json.obj("n" -> 4.8)) mustEqual(Success(JsNumber(4.8)))
      }

      "JsBoolean" in {
        (__ \ "n").read(jsBoolean).validate(Json.obj("n" -> true)) mustEqual(Success(JsBoolean(true)))
        (__ \ "n").read(jsBoolean).validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Boolean")))))
      }

      "Option" in {
        (__ \ "n").read(option(boolean)).validate(Json.obj("n" -> true)) mustEqual(Success(Some(true)))
        (__ \ "n").read(option(boolean)).validate(Json.obj("n" -> JsNull)) mustEqual(Success(None))
        (__ \ "n").read(option(boolean)).validate(Json.obj("foo" -> "bar")) mustEqual(Success(None))
        (__ \ "n").read(option(boolean)).validate(Json.obj("n" -> "bar")) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Boolean")))))
      }

      "Map[String, V]" in {
        (__ \ "n").read(Rules.map(string)).validate(Json.obj("n" -> Json.obj("foo" -> "bar"))) mustEqual(Success(Map("foo" -> "bar")))
        (__ \ "n").read(Rules.map(int)).validate(Json.obj("n" -> Json.obj("foo" -> 4, "bar" -> 5))) mustEqual(Success(Map("foo" -> 4, "bar" -> 5)))
        (__ \ "n").read(Rules.map(int)).validate(Json.obj("n" -> Json.obj("foo" -> 4, "bar" -> "frack"))) mustEqual(Failure(Seq(__ \ "n" \ "bar" -> Seq(ValidationError("validation.type-mismatch", "Int")))))
      }

      "Traversable" in {
        (__ \ "n").read(Rules.traversable(string)).validate(Json.obj("n" -> Seq("foo"))).get.toSeq must haveTheSameElementsAs(Seq("foo"))
        (__ \ "n").read(Rules.traversable(int)).validate(Json.obj("n" -> Seq(1, 2, 3))).get.toSeq must haveTheSameElementsAs(Seq(1, 2, 3))
        (__ \ "n").read(Rules.traversable(string)).validate(Json.obj("n" -> "paf")) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Array")))))
      }

      "Array" in {
        (__ \ "n").read(array(string)).validate(Json.obj("n" -> Seq("foo"))).get.toSeq must haveTheSameElementsAs(Seq("foo"))
        (__ \ "n").read(array(int)).validate(Json.obj("n" -> Seq(1, 2, 3))).get.toSeq must haveTheSameElementsAs(Seq(1, 2, 3))
        (__ \ "n").read(array(string)).validate(Json.obj("n" -> "paf")) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Array")))))
      }

      "Seq" in {
        (__ \ "n").read(seq(string)).validate(Json.obj("n" -> Seq("foo"))).get must haveTheSameElementsAs(Seq("foo"))
        (__ \ "n").read(seq(int)).validate(Json.obj("n" -> Seq(1, 2, 3))).get must haveTheSameElementsAs(Seq(1, 2, 3))
        (__ \ "n").read(seq(string)).validate(Json.obj("n" -> "paf")) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Array")))))
        (__ \ "n").read(seq(string)).validate(Json.parse("""{"n":["foo", 2]}""")) mustEqual(Failure(Seq(__ \ "n" \ 1 -> Seq(ValidationError("validation.type-mismatch", "String")))))
      }

    }

    "validate data" in {
      (__ \ "firstname").read(string compose notEmpty).validate(valid) mustEqual(Success("Julien"))

      val p = (__ \ "informations" \ "label")
      p.read(string compose notEmpty).validate(valid) mustEqual(Success("Personal"))
      p.read(string compose notEmpty).validate(invalid) mustEqual(Failure(Seq(p -> Seq(ValidationError("validation.nonemptytext")))))
    }

    "validate optional" in {
      (__ \ "firstname").read(option(string)).validate(valid) mustEqual(Success(Some("Julien")))
      (__ \ "foobar").read(option(string)).validate(valid) mustEqual(Success(None))
    }

    "validate deep" in {
      val p = (__ \ "informations" \ "label")

      (__ \ "informations").read(
        (__ \ "label").read(string compose notEmpty)).validate(valid) mustEqual(Success("Personal"))

      (__ \ "informations").read(
        (__ \ "label").read(string compose notEmpty)).validate(invalid) mustEqual(Failure(Seq(p -> Seq(ValidationError("validation.nonemptytext")))))
    }

    "coerce type" in {
      (__ \ "age").read(int).validate(valid) mustEqual(Success(27))
      (__ \ "firstname").read(int).validate(valid) mustEqual(Failure(Seq((__ \ "firstname") -> Seq(ValidationError("validation.type-mismatch", "Int")))))
    }

    "compose constraints" in {
      // TODO: create MonoidOps
      val composed = string compose monoidConstraint.append(notEmpty, minLength(3))
      (__ \ "firstname").read(composed).validate(valid) mustEqual(Success("Julien"))

      val p = __ \ "informations" \ "label"
      val err = Failure(Seq(p -> Seq(ValidationError("validation.nonemptytext"), ValidationError("validation.minLength", 3))))
      p.read(composed).validate(invalid) mustEqual(err)
    }

    "compose validations" in {
      import play.api.libs.functional.syntax._

      val nonEmptyText = string compose notEmpty
      ((__ \ "firstname").read(nonEmptyText) ~
       (__ \ "lastname").read(nonEmptyText)){ _ -> _ }
         .validate(valid) mustEqual Success("Julien" -> "Tournay")

      ((__ \ "firstname").read(nonEmptyText) ~
      (__ \ "lastname").read(nonEmptyText) ~
      (__ \ "informations" \ "label").read(nonEmptyText)){ (_, _, _) }
       .validate(invalid) mustEqual Failure(Seq((__ \ "informations" \ "label") -> Seq(ValidationError("validation.nonemptytext"))))
    }

    "lift validations to seq validations" in {
      val nonEmptyText = string compose notEmpty
      (__ \ "foo").read(seq(nonEmptyText)).validate(Json.obj("foo" -> Seq("bar")))
        .get must haveTheSameElementsAs(Seq("bar"))

      (__ \ "foo").read(
        (__ \ "foo").read(seq(nonEmptyText)))
          .validate(Json.obj("foo" -> Json.obj("foo" -> Seq("bar"))))
            .get must haveTheSameElementsAs(Seq("bar"))

      (__ \ "n").read(seq(nonEmptyText))
        .validate(Json.parse("""{"n":["foo", ""]}""")) mustEqual(Failure(Seq(__ \ "n" \ 1 -> Seq(ValidationError("validation.nonemptytext")))))
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

      val nonEmptyText = string compose notEmpty

      val infoValidation =
       ((__ \ "label").read(nonEmptyText) ~
        (__ \ "email").read(option(string compose email)) ~
        (__ \ "phones").read(seq(nonEmptyText))) (ContactInformation.apply _)

      val contactValidation =
       ((__ \ "firstname").read(nonEmptyText) ~
        (__ \ "lastname").read(nonEmptyText) ~
        (__ \ "company").read(option(string)) ~
        (__ \ "informations").read(seq(infoValidation))) (Contact.apply _)

      val expected =
        Contact("Julien", "Tournay", None, Seq(
          ContactInformation("Personal", Some("fakecontact@gmail.com"), List("01.23.45.67.89", "98.76.54.32.10"))))

      contactValidation.validate(validJson) mustEqual(Success(expected))
      contactValidation.validate(invalidJson) mustEqual(Failure(Seq(
        (__ \ "informations" \ 0 \"label") -> Seq(ValidationError("validation.nonemptytext")))))
    }

  }
}
