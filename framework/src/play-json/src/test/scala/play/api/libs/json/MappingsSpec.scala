package play.api.libs.json

import org.specs2.mutable._
import scala.util.control.Exception._
import play.api.libs.functional._
import play.api.libs.functional.syntax._
import play.api.data.validation._
import play.api.data.validation.Constraints._

object MappingsSpec extends Specification {

  "Json Mappings" should {

    import play.api.data.validation.Mappings._
    import play.api.libs.json.Mappings._
    val __ = Path[JsValue]()

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
      (__ \ "firstname").read[String].validate(valid) mustEqual(Success("Julien"))

      val errPath = __ \ "foo"
      val error = Failure(Seq(errPath -> Seq(ValidationError("validation.required"))))
      errPath.read[String].validate(invalid) mustEqual(error)
    }

    "support all types of Json values" in {
      val __ = Path[JsValue]()

      "Int" in {
        (__ \ "n").read[Int].validate(Json.obj("n" -> 4)) mustEqual(Success(4))
        (__ \ "n").read[Int].validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Int")))))
        (__ \ "n").read[Int].validate(Json.obj("n" -> 4.8)) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Int")))))
      }

      "Short" in {
        (__ \ "n").read[Short].validate(Json.obj("n" -> 4)) mustEqual(Success(4))
        (__ \ "n").read[Short].validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Short")))))
        (__ \ "n").read[Short].validate(Json.obj("n" -> 4.8)) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Short")))))
      }

      "Long" in {
        (__ \ "n").read[Long].validate(Json.obj("n" -> 4)) mustEqual(Success(4))
        (__ \ "n").read[Long].validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Long")))))
        (__ \ "n").read[Long].validate(Json.obj("n" -> 4.8)) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Long")))))
      }

      "Float" in {
        (__ \ "n").read[Float].validate(Json.obj("n" -> 4)) mustEqual(Success(4))
        (__ \ "n").read[Float].validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Float")))))
        (__ \ "n").read[Float].validate(Json.obj("n" -> 4.8)) mustEqual(Success(4.8F))
      }

      "Double" in {
        (__ \ "n").read[Double].validate(Json.obj("n" -> 4)) mustEqual(Success(4))
        (__ \ "n").read[Double].validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Double")))))
        (__ \ "n").read[Double].validate(Json.obj("n" -> 4.8)) mustEqual(Success(4.8))
      }

      "java BigDecimal" in {
        import java.math.{ BigDecimal => jBigDecimal }
        (__ \ "n").read[jBigDecimal].validate(Json.obj("n" -> 4)) mustEqual(Success(new jBigDecimal("4")))
        (__ \ "n").read[jBigDecimal].validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "java.math.BigDecimal")))))
        (__ \ "n").read[jBigDecimal].validate(Json.obj("n" -> 4.8)) mustEqual(Success(new jBigDecimal("4.8")))
      }

      "scala BigDecimal" in {
        (__ \ "n").read[BigDecimal].validate(Json.obj("n" -> 4)) mustEqual(Success(BigDecimal(4)))
        (__ \ "n").read[BigDecimal].validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "BigDecimal")))))
        (__ \ "n").read[BigDecimal].validate(Json.obj("n" -> 4.8)) mustEqual(Success(BigDecimal(4.8)))
      }

      "date" in { skipped }
      "joda date" in { skipped }
      "joda local data" in { skipped }
      "dql date" in { skipped }
      "Boolean" in { skipped }
      "String" in { skipped }
      "JsObject" in { skipped }
      "JsString" in { skipped }

      "JsNumber" in {
        (__ \ "n").read[JsNumber].validate(Json.obj("n" -> 4)) mustEqual(Success(JsNumber(4)))
        (__ \ "n").read[JsNumber].validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "JsNumber")))))
        (__ \ "n").read[JsNumber].validate(Json.obj("n" -> 4.8)) mustEqual(Success(JsNumber(4.8)))
      }

      "JsBoolean" in { skipped }
      "Option" in { skipped }
      "Map[String, V]" in { skipped }
      "Traversable" in { skipped }
      "Array" in { skipped }
    }

    "validate data" in {
      (__ \ "firstname").read(nonEmptyText).validate(valid) mustEqual(Success("Julien"))

      val p = (__ \ "informations" \ "label")
      p.read(nonEmptyText).validate(valid) mustEqual(Success("Personal"))
      p.read(nonEmptyText).validate(invalid) mustEqual(Failure(Seq(p -> Seq(ValidationError("validation.nonemptytext")))))
    }

    "validate optional" in {
      (__ \ "firstname").read[Option[String]].validate(valid) mustEqual(Success(Some("Julien")))
      (__ \ "foobar").read[Option[String]].validate(valid) mustEqual(Success(None))
    }

    "validate deep" in {
      val p = (__ \ "informations" \ "label")

      (__ \ "informations").read(
        (__ \ "label").read(nonEmptyText)).validate(valid) mustEqual(Success("Personal"))

      (__ \ "informations").read(
        (__ \ "label").read(nonEmptyText)).validate(invalid) mustEqual(Failure(Seq(p -> Seq(ValidationError("validation.nonemptytext")))))
    }

    "coerce type" in {
      (__ \ "age").read[Int].validate(valid) mustEqual(Success(27))
      (__ \ "firstname").read[Int].validate(valid) mustEqual(Failure(Seq((__ \ "firstname") -> Seq(ValidationError("validation.type-mismatch", "Int")))))
    }

    "compose constraints" in {
      // TODO: create MonoidOps
      val composed = monoidConstraint.append(nonEmptyText, minLength(3))
      (__ \ "firstname").read(composed).validate(valid) mustEqual(Success("Julien"))

      val p = __ \ "informations" \ "label"
      val err = Failure(Seq(p -> Seq(ValidationError("validation.nonemptytext"), ValidationError("validation.minLength", 3))))
      p.read(composed).validate(invalid) mustEqual(err)
    }

    "compose validations" in {
      import play.api.libs.functional.syntax._

      ((__ \ "firstname").read(nonEmptyText) ~
       (__ \ "lastname").read(nonEmptyText)){ _ -> _ }
         .validate(valid) mustEqual Success("Julien" -> "Tournay")

      ((__ \ "firstname").read(nonEmptyText) ~
      (__ \ "lastname").read(nonEmptyText) ~
      (__ \ "informations" \ "label").read(nonEmptyText)){ (_, _, _) }
       .validate(invalid) mustEqual Failure(Seq((__ \ "informations" \ "label") -> Seq(ValidationError("validation.nonemptytext"))))
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

      val infoValidation =
       ((__ \ "label").read(nonEmptyText) ~
        (__ \ "email").read(optional(email)) ~
        (__ \ "phones").read(seq(nonEmptyText))) (ContactInformation.apply _)

      val contactValidation =
       ((__ \ "firstname").read(nonEmptyText) ~
        (__ \ "lastname").read(nonEmptyText) ~
        (__ \ "company").read[Option[String]] ~
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
