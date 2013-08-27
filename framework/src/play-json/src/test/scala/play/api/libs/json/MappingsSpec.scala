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
      (Path \ "firstname").read(string).validate(valid) mustEqual(Success("Julien"))
      val errPath = Path \ "foo"
      val error = Failure(Seq(errPath -> Seq(ValidationError("validation.required"))))
      errPath.read(string).validate(invalid) mustEqual(error)
    }

    "support all types of Json values" in {

      "null" in {
        (Path \ "n").read(jsNull).validate(Json.obj("n" -> JsNull)) mustEqual(Success(JsNull))
        (Path \ "n").read(jsNull).validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "null")))))
        (Path \ "n").read(jsNull).validate(Json.obj("n" -> 4.8)) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "null")))))
      }

      "Int" in {
        (Path \ "n").read(int).validate(Json.obj("n" -> 4)) mustEqual(Success(4))
        (Path \ "n").read(int).validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Int")))))
        (Path \ "n").read(int).validate(Json.obj("n" -> 4.8)) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Int")))))
        (Path \ "n" \ "o").read(int).validate(Json.obj("n" -> Json.obj("o" -> 4))) mustEqual(Success(4))
        (Path \ "n" \ "o").read(int).validate(Json.obj("n" -> Json.obj("o" -> "foo"))) mustEqual(Failure(Seq(Path \ "n" \ "o" -> Seq(ValidationError("validation.type-mismatch", "Int")))))

        (Path \ "n" \ "o" \ "p" ).read(int).validate(Json.obj("n" -> Json.obj("o" -> Json.obj("p" -> 4)))) mustEqual(Success(4))
        (Path \ "n" \ "o" \ "p").read(int).validate(Json.obj("n" -> Json.obj("o" -> Json.obj("p" -> "foo")))) mustEqual(Failure(Seq(Path \ "n" \ "o" \ "p" -> Seq(ValidationError("validation.type-mismatch", "Int")))))

        val errPath = Path \ "foo"
        val error = Failure(Seq(errPath -> Seq(ValidationError("validation.required"))))
        errPath.read(int).validate(Json.obj("n" -> 4)) mustEqual(error)
      }

      "Short" in {
        (Path \ "n").read(short).validate(Json.obj("n" -> 4)) mustEqual(Success(4))
        (Path \ "n").read(short).validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Short")))))
        (Path \ "n").read(short).validate(Json.obj("n" -> 4.8)) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Short")))))
      }

      "Long" in {
        (Path \ "n").read(long).validate(Json.obj("n" -> 4)) mustEqual(Success(4))
        (Path \ "n").read(long).validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Long")))))
        (Path \ "n").read(long).validate(Json.obj("n" -> 4.8)) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Long")))))
      }

      "Float" in {
        (Path \ "n").read(float).validate(Json.obj("n" -> 4)) mustEqual(Success(4))
        (Path \ "n").read(float).validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Float")))))
        (Path \ "n").read(float).validate(Json.obj("n" -> 4.8)) mustEqual(Success(4.8F))
      }

      "Double" in {
        (Path \ "n").read(double).validate(Json.obj("n" -> 4)) mustEqual(Success(4))
        (Path \ "n").read(double).validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Double")))))
        (Path \ "n").read(double).validate(Json.obj("n" -> 4.8)) mustEqual(Success(4.8))
      }

      "java BigDecimal" in {
        import java.math.{ BigDecimal => jBigDecimal }
        (Path \ "n").read(javaBigDecimal).validate(Json.obj("n" -> 4)) mustEqual(Success(new jBigDecimal("4")))
        (Path \ "n").read(javaBigDecimal).validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "BigDecimal")))))
        (Path \ "n").read(javaBigDecimal).validate(Json.obj("n" -> 4.8)) mustEqual(Success(new jBigDecimal("4.8")))
      }

      "scala BigDecimal" in {
        (Path \ "n").read(bigDecimal).validate(Json.obj("n" -> 4)) mustEqual(Success(BigDecimal(4)))
        (Path \ "n").read(bigDecimal).validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "BigDecimal")))))
        (Path \ "n").read(bigDecimal).validate(Json.obj("n" -> 4.8)) mustEqual(Success(BigDecimal(4.8)))
      }

      "date" in { skipped }
      "joda date" in { skipped }
      "joda local data" in { skipped }
      "sql date" in { skipped }

      "Boolean" in {
        (Path \ "n").read(boolean).validate(Json.obj("n" -> true)) mustEqual(Success(true))
        (Path \ "n").read(boolean).validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Boolean")))))
      }

      "String" in {
        (Path \ "n").read(string).validate(Json.obj("n" -> "foo")) mustEqual(Success("foo"))
        (Path \ "n").read(string).validate(Json.obj("n" -> 42)) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "String")))))
        (Path \ "n").read(string).validate(Json.obj("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "String")))))
        (Path \ "o").read(string).validate(Json.obj("o" -> Json.obj("n" -> "foo"))) mustEqual(Failure(Seq(Path \ "o" -> Seq(ValidationError("validation.type-mismatch", "String")))))
      }

      "JsObject" in {
        (Path \ "o").read(jsObject).validate(Json.obj("o" -> Json.obj("n" -> "foo"))) mustEqual(Success(JsObject(Seq("n" -> JsString("foo")))))
        (Path \ "n").read(jsObject).validate(Json.obj("n" -> 42)) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Object")))))
        (Path \ "n").read(jsObject).validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Object")))))
        (Path \ "n").read(jsObject).validate(Json.obj("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Object")))))
      }

      "JsString" in {
        (Path \ "n").read(jsString).validate(Json.obj("n" -> "foo")) mustEqual(Success(JsString("foo")))
        (Path \ "n").read(jsString).validate(Json.obj("n" -> 42)) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "String")))))
      }

      "JsNumber" in {
        (Path \ "n").read(jsNumber).validate(Json.obj("n" -> 4)) mustEqual(Success(JsNumber(4)))
        (Path \ "n").read(jsNumber).validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Number")))))
        (Path \ "n").read(jsNumber).validate(Json.obj("n" -> 4.8)) mustEqual(Success(JsNumber(4.8)))
      }

      "JsBoolean" in {
        (Path \ "n").read(jsBoolean).validate(Json.obj("n" -> true)) mustEqual(Success(JsBoolean(true)))
        (Path \ "n").read(jsBoolean).validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Boolean")))))
      }

      "Option" in {
        (Path \ "n").read(option(boolean)).validate(Json.obj("n" -> true)) mustEqual(Success(Some(true)))
        (Path \ "n").read(option(boolean)).validate(Json.obj("n" -> JsNull)) mustEqual(Success(None))
        (Path \ "n").read(option(boolean)).validate(Json.obj("foo" -> "bar")) mustEqual(Success(None))
        (Path \ "n").read(option(boolean)).validate(Json.obj("n" -> "bar")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Boolean")))))
      }

      "Map[String, V]" in {
        (Path \ "n").read(Rules.map(string)).validate(Json.obj("n" -> Json.obj("foo" -> "bar"))) mustEqual(Success(Map("foo" -> "bar")))
        (Path \ "n").read(Rules.map(int)).validate(Json.obj("n" -> Json.obj("foo" -> 4, "bar" -> 5))) mustEqual(Success(Map("foo" -> 4, "bar" -> 5)))
        (Path \ "n").read(Rules.map(int)).validate(Json.obj("n" -> Json.obj("foo" -> 4, "bar" -> "frack"))) mustEqual(Failure(Seq(Path \ "n" \ "bar" -> Seq(ValidationError("validation.type-mismatch", "Int")))))
      }

      "Traversable" in {
        (Path \ "n").read(Rules.traversable(string)).validate(Json.obj("n" -> Seq("foo"))).get.toSeq must haveTheSameElementsAs(Seq("foo"))
        (Path \ "n").read(Rules.traversable(int)).validate(Json.obj("n" -> Seq(1, 2, 3))).get.toSeq must haveTheSameElementsAs(Seq(1, 2, 3))
        (Path \ "n").read(Rules.traversable(string)).validate(Json.obj("n" -> "paf")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Array")))))
      }

      "Array" in {
        (Path \ "n").read(array(string)).validate(Json.obj("n" -> Seq("foo"))).get.toSeq must haveTheSameElementsAs(Seq("foo"))
        (Path \ "n").read(array(int)).validate(Json.obj("n" -> Seq(1, 2, 3))).get.toSeq must haveTheSameElementsAs(Seq(1, 2, 3))
        (Path \ "n").read(array(string)).validate(Json.obj("n" -> "paf")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Array")))))
      }

      "Seq" in {
        (Path \ "n").read(seq(string)).validate(Json.obj("n" -> Seq("foo"))).get must haveTheSameElementsAs(Seq("foo"))
        (Path \ "n").read(seq(int)).validate(Json.obj("n" -> Seq(1, 2, 3))).get must haveTheSameElementsAs(Seq(1, 2, 3))
        (Path \ "n").read(seq(string)).validate(Json.obj("n" -> "paf")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Array")))))
        (Path \ "n").read(seq(string)).validate(Json.parse("""{"n":["foo", 2]}""")) mustEqual(Failure(Seq(Path \ "n" \ 1 -> Seq(ValidationError("validation.type-mismatch", "String")))))
      }

    }

    "validate data" in {
      (Path \ "firstname").read(string compose notEmpty).validate(valid) mustEqual(Success("Julien"))

      val p = (Path \ "informations" \ "label")
      p.read(string compose notEmpty).validate(valid) mustEqual(Success("Personal"))
      p.read(string compose notEmpty).validate(invalid) mustEqual(Failure(Seq(p -> Seq(ValidationError("validation.nonemptytext")))))
    }

    "validate optional" in {
      (Path \ "firstname").read(option(string)).validate(valid) mustEqual(Success(Some("Julien")))
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
      (Path \ "age").read(int compose min(20)).validate(valid) mustEqual(Success(27))
      (Path \ "age").read(int compose max(50)).validate(valid) mustEqual(Success(27))
      (Path \ "age").read(int compose min(50)).validate(valid) mustEqual(Failure(Seq((Path \ "age") -> Seq(ValidationError("validation.min", 50)))))
      (Path \ "age").read(int compose max(0)).validate(valid) mustEqual(Failure(Seq((Path \ "age") -> Seq(ValidationError("validation.max", 0)))))
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

      val nonEmptyText = string compose notEmpty
      ((Path \ "firstname").read(nonEmptyText) ~
       (Path \ "lastname").read(nonEmptyText)){ _ -> _ }
         .validate(valid) mustEqual Success("Julien" -> "Tournay")

      ((Path \ "firstname").read(nonEmptyText) ~
      (Path \ "lastname").read(nonEmptyText) ~
      (Path \ "informations" \ "label").read(nonEmptyText)){ (_, _, _) }
       .validate(invalid) mustEqual Failure(Seq((Path \ "informations" \ "label") -> Seq(ValidationError("validation.nonemptytext"))))
    }

    "lift validations to seq validations" in {
      val nonEmptyText = string compose notEmpty
      (Path \ "foo").read(seq(nonEmptyText)).validate(Json.obj("foo" -> Seq("bar")))
        .get must haveTheSameElementsAs(Seq("bar"))

      (Path \ "foo").read(
        (Path \ "foo").read(seq(nonEmptyText)))
          .validate(Json.obj("foo" -> Json.obj("foo" -> Seq("bar"))))
            .get must haveTheSameElementsAs(Seq("bar"))

      (Path \ "n").read(seq(nonEmptyText))
        .validate(Json.parse("""{"n":["foo", ""]}""")) mustEqual(Failure(Seq(Path \ "n" \ 1 -> Seq(ValidationError("validation.nonemptytext")))))
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
       ((Path \ "label").read(nonEmptyText) ~
        (Path \ "email").read(option(string compose email)) ~
        (Path \ "phones").read(seq(nonEmptyText))) (ContactInformation.apply _)

      val contactValidation =
       ((Path \ "firstname").read(nonEmptyText) ~
        (Path \ "lastname").read(nonEmptyText) ~
        (Path \ "company").read(option(string)) ~
        (Path \ "informations").read(seq(infoValidation))) (Contact.apply _)

      val expected =
        Contact("Julien", "Tournay", None, Seq(
          ContactInformation("Personal", Some("fakecontact@gmail.com"), List("01.23.45.67.89", "98.76.54.32.10"))))

      contactValidation.validate(validJson) mustEqual(Success(expected))
      contactValidation.validate(invalidJson) mustEqual(Failure(Seq(
        (Path \ "informations" \ 0 \"label") -> Seq(ValidationError("validation.nonemptytext")))))
    }

  }
}
