package play.api.data.validation

import org.specs2.mutable._
import scala.util.control.Exception._
import play.api.libs.functional._
import play.api.libs.functional.syntax._

object PathSpec extends Specification {

  "Path" should {
    "be compareable" in {
       val __ = Path[String]()
      (__ \ "foo" \ "bar") must equalTo((__ \ "foo" \ "bar"))
      (__ \ "foo" \ "bar").hashCode must equalTo((__ \ "foo" \ "bar").hashCode)
      (__ \ "foo" \ "bar") must not equalTo((__ \ "foo"))
      (__ \ "foo" \ "bar").hashCode must not equalTo((__ \ "foo").hashCode)
    }

    "compose" in {
      val __ = Path[String]()
      val c = (__ \ "foo" \ "bar") compose (__ \ "baz")
      val c2 = (__ \ "foo" \ "bar") ++ (__ \ "baz")
      c must equalTo(__ \ "foo" \ "bar" \ "baz")
      c2 must equalTo(__ \ "foo" \ "bar" \ "baz")
    }

    "have deconstructors" in {
      val __ = Path[String]()
      val path = __ \ "foo" \ "bar" \ "baz"

      val (h \: t) = path
      h must equalTo(KeyPathNode("foo"))
      t must equalTo(__ \ "bar" \ "baz")

      val (h1 \: h2 \: t2) = path
      h1 must equalTo(KeyPathNode("foo"))
      h2 must equalTo(KeyPathNode("bar"))
      t2 must equalTo(__ \ "baz")
    }

    import Mappings._
    import Constraints._
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
      val __ = Path[M]()
      (__ \ "firstname").read[String].validate(valid) mustEqual(Success("Julien"))

      val errPath = __ \ "foo"
      val error = Failure(Seq(errPath -> Seq(ValidationError("validation.required"))))
      errPath.read[String].validate(invalid) mustEqual(error)
    }

    "validate data" in {
      val __ = Path[M]()
      (__ \ "firstname").read(nonEmptyText).validate(valid) mustEqual(Success("Julien"))

      val p = (__ \ "informations" \ "label")
      p.read(nonEmptyText).validate(valid) mustEqual(Success("Personal"))
      p.read(nonEmptyText).validate(invalid) mustEqual(Failure(Seq(p -> Seq(ValidationError("validation.nonemptytext")))))
    }

    "validate seq" in {
      val __ = Path[M]()
      (__ \ "firstname").read[Seq[String]].validate(valid) mustEqual(Success(Seq("Julien")))
      (__ \ "foobar").read[Seq[String]].validate(valid) mustEqual(Failure(Seq(__ \ "foobar" -> Seq(ValidationError("validation.required")))))
    }

    "validate optional" in {
      val __ = Path[M]()

      (__ \ "firstname").read[Option[String]].validate(valid) mustEqual(Success(Some("Julien")))
      (__ \ "foobar").read[Option[String]].validate(valid) mustEqual(Success(None))
    }

    "validate deep" in {
      val __ = Path[M]()
      val p = (__ \ "informations" \ "label")

      (__ \ "informations").read(
        (__ \ "label").read(nonEmptyText)).validate(valid) mustEqual(Success("Personal"))

      (__ \ "informations").read(
        (__ \ "label").read(nonEmptyText)).validate(invalid) mustEqual(Failure(Seq(p -> Seq(ValidationError("validation.nonemptytext")))))
    }

    "coerce type" in {
      val __ = Path[M]()
      (__ \ "age").read[Int].validate(valid) mustEqual(Success(27))
      (__ \ "firstname").read[Int].validate(valid) mustEqual(Failure(Seq((__ \ "firstname") -> Seq(ValidationError("validation.type-mismatch", "Int")))))
    }

    "compose constraints" in {
      val __ = Path[M]()
      // TODO: create MonoidOps

      val composed = monoidConstraint.append(nonEmptyText, minLength(3))
      (__ \ "firstname").read(composed).validate(valid) mustEqual(Success("Julien"))

      val p = __ \ "informations" \ "label"
      val err = Failure(Seq(p -> Seq(ValidationError("validation.nonemptytext"), ValidationError("validation.minLength", 3))))
      p.read(composed).validate(invalid) mustEqual(err)
    }

    "compose validations" in {
      val __ = Path[M]()
      import play.api.libs.functional.syntax._

      ((__ \ "firstname").read(nonEmptyText) ~
       (__ \ "lastname").read(nonEmptyText)){ _ -> _ }
         .validate(valid) mustEqual Success("Julien" -> "Tournay")

      ((__ \ "firstname").read(nonEmptyText) ~
      (__ \ "lastname").read(nonEmptyText) ~
      (__ \ "informations" \ "label").read(nonEmptyText)){ (_, _, _) }
       .validate(invalid) mustEqual Failure(Seq((__ \ "informations" \ "label") -> Seq(ValidationError("validation.nonemptytext"))))
    }

  }
}
