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
      val __ = Path[M]()
      (__ \ "firstname").read[String].validate(valid) mustEqual(Success("Julien"))

      val errPath = __ \ "foo"
      val error = Failure(Seq(errPath -> Seq(ValidationError("validation.required"))))
      errPath.read[String].validate(invalid) mustEqual(error)
    }

    "support primitives types" in {

      val __ = Path[M]()

      "Int" in {
        (__ \ "n").read[Int].validate(Map("n" -> Seq("4"))) mustEqual(Success(4))
        (__ \ "n").read[Int].validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Int")))))
        (__ \ "n").read[Int].validate(Map("n" -> Seq("4.8"))) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Int")))))
        (__ \ "n" \ "o").read[Int].validate(Map("n.o" -> Seq("4"))) mustEqual(Success(4))
        (__ \ "n" \ "o").read[Int].validate(Map("n.o" -> Seq("foo"))) mustEqual(Failure(Seq(__ \ "n" \ "o" -> Seq(ValidationError("validation.type-mismatch", "Int")))))

        (__ \ "n" \ "o" \ "p").read[Int].validate(Map("n.o.p" -> Seq("4"))) mustEqual(Success(4))
        (__ \ "n" \ "o" \ "p").read[Int].validate(Map("n.o.p" -> Seq("foo"))) mustEqual(Failure(Seq(__ \ "n" \ "o" \ "p" -> Seq(ValidationError("validation.type-mismatch", "Int")))))

        val errPath = __ \ "foo"
        val error = Failure(Seq(errPath -> Seq(ValidationError("validation.required"))))
        errPath.read[Int].validate(Map("n" -> Seq("4"))) mustEqual(error)
      }

      "Short" in {
        (__ \ "n").read[Short].validate(Map("n" -> Seq("4"))) mustEqual(Success(4))
        (__ \ "n").read[Short].validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Short")))))
        (__ \ "n").read[Short].validate(Map("n" -> Seq("4.8"))) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Short")))))
      }


      "Long" in {
        (__ \ "n").read[Long].validate(Map("n" -> Seq("4"))) mustEqual(Success(4))
        (__ \ "n").read[Long].validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Long")))))
        (__ \ "n").read[Long].validate(Map("n" -> Seq("4.8"))) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Long")))))
      }

      "Float" in {
        (__ \ "n").read[Float].validate(Map("n" -> Seq("4"))) mustEqual(Success(4))
        (__ \ "n").read[Float].validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Float")))))
        (__ \ "n").read[Float].validate(Map("n" -> Seq("4.8"))) mustEqual(Success(4.8F))
      }

      "Double" in {
        (__ \ "n").read[Double].validate(Map("n" -> Seq("4"))) mustEqual(Success(4))
        (__ \ "n").read[Double].validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Double")))))
        (__ \ "n").read[Double].validate(Map("n" -> Seq("4.8"))) mustEqual(Success(4.8))
      }

      "java BigDecimal" in {
        import java.math.{ BigDecimal => jBigDecimal }
        (__ \ "n").read[jBigDecimal].validate(Map("n" -> Seq("4"))) mustEqual(Success(new jBigDecimal("4")))
        (__ \ "n").read[jBigDecimal].validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "BigDecimal")))))
        (__ \ "n").read[jBigDecimal].validate(Map("n" -> Seq("4.8"))) mustEqual(Success(new jBigDecimal("4.8")))
      }

      "scala BigDecimal" in {
        (__ \ "n").read[BigDecimal].validate(Map("n" -> Seq("4"))) mustEqual(Success(BigDecimal(4)))
        (__ \ "n").read[BigDecimal].validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "BigDecimal")))))
        (__ \ "n").read[BigDecimal].validate(Map("n" -> Seq("4.8"))) mustEqual(Success(BigDecimal(4.8)))
      }

      "date" in { skipped }
      "joda date" in { skipped }
      "joda local data" in { skipped }
      "sql date" in { skipped }

      // "Boolean" in {
      //   (__ \ "n").read[Boolean].validate(Json.obj("n" -> true)) mustEqual(Success(true))
      //   (__ \ "n").read[Boolean].validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Boolean")))))
      // }

      "String" in {
        (__ \ "n").read[String].validate(Map("n" -> Seq("foo"))) mustEqual(Success("foo"))
        (__ \ "o").read[String].validate(Map("o.n" -> Seq("foo"))) mustEqual(Failure(Seq(__ \ "o" -> Seq(ValidationError("validation.required")))))
      }

      // "Option" in {
      //   skipped("There's a problem here. How to handle JsNull ?? Empty values ??")
      //   (__ \ "n").read[Option[Boolean]].validate(Json.obj("n" -> true)) mustEqual(Success(Some(true)))
      //   (__ \ "n").read[Option[Boolean]].validate(Json.obj("n" -> JsNull)) mustEqual(Success(None))
      //   (__ \ "n").read[Option[Boolean]].validate(Json.obj("foo" -> "bar")) mustEqual(Success(None))
      //   (__ \ "n").read[Option[Boolean]].validate(Json.obj("n" -> "bar")) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Option[Boolean]")))))
      // }

      // "Map[String, V]" in {
      //   (__ \ "n").read[Map[String, String]].validate(Json.obj("n" -> Json.obj("foo" -> "bar"))) mustEqual(Success(Map("foo" -> "bar")))
      //   (__ \ "n").read[Map[String, Int]].validate(Json.obj("n" -> Json.obj("foo" -> 4, "bar" -> 5))) mustEqual(Success(Map("foo" -> 4, "bar" -> 5)))
      //   (__ \ "n").read[Map[String, Int]].validate(Json.obj("n" -> Json.obj("foo" -> 4, "bar" -> "frack"))) mustEqual(Failure(Seq(__ \ "n" \ "bar" -> Seq(ValidationError("validation.type-mismatch", "Int")))))
      // }

      // "Traversable" in {
      //   (__ \ "n").read[Traversable[String]].validate(Json.obj("n" -> Seq("foo"))).get.toSeq must haveTheSameElementsAs(Seq("foo"))
      //   (__ \ "n").read[Traversable[Int]].validate(Json.obj("n" -> Seq(1, 2, 3))).get.toSeq must haveTheSameElementsAs(Seq(1, 2, 3))
      //   (__ \ "n").read[Traversable[String]].validate(Json.obj("n" -> "paf")) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Array")))))
      // }

      // "Array" in {
      //   (__ \ "n").read[Array[String]].validate(Json.obj("n" -> Seq("foo"))).get.toSeq must haveTheSameElementsAs(Seq("foo"))
      //   (__ \ "n").read[Array[Int]].validate(Json.obj("n" -> Seq(1, 2, 3))).get.toSeq must haveTheSameElementsAs(Seq(1, 2, 3))
      //   (__ \ "n").read[Array[String]].validate(Json.obj("n" -> "paf")) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Array")))))
      // }

      // "Seq" in {
      //   (__ \ "n").read[Seq[String]].validate(Json.obj("n" -> Seq("foo"))).get must haveTheSameElementsAs(Seq("foo"))
      //   (__ \ "n").read[Seq[Int]].validate(Json.obj("n" -> Seq(1, 2, 3))).get must haveTheSameElementsAs(Seq(1, 2, 3))
      //   (__ \ "n").read[Seq[String]].validate(Json.obj("n" -> "paf")) mustEqual(Failure(Seq(__ \ "n" -> Seq(ValidationError("validation.type-mismatch", "Array")))))
      //   (__ \ "n").read[Seq[String]].validate(Json.parse("""{"n":["foo", 2]}""")) mustEqual(Failure(Seq(__ \ "n" \ 1 -> Seq(ValidationError("validation.type-mismatch", "String")))))
      // }

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
