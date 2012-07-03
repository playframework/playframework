package play.api.libs.json

import org.specs2.mutable._
import play.api.libs.json._
import play.api.libs.json.Json._
import play.api.libs.json.Generic._
import play.api.libs.json.JsResultHelpers._
import play.api.libs.json.Reads._
import scala.util.control.Exception._
import java.text.ParseException
import play.api.data.validation.ValidationError
import Constraint._

object JsonValidSpec extends Specification {
  "JSON reads" should {
    "validate simple types" in {
      JsString("string").validate[String] must equalTo(JsSuccess("string"))
      JsNumber(5).validate[Int] must equalTo(JsSuccess(5))
      JsNumber(5L).validate[Long] must equalTo(JsSuccess(5L))
      JsNumber(5).validate[Short] must equalTo(JsSuccess(5))
      JsNumber(123.5).validate[Float] must equalTo(JsSuccess(123.5))
      JsNumber(123456789123456.567891234).validate[Double] must equalTo(JsSuccess(123456789123456.567891234))
      JsBoolean(true).validate[Boolean] must equalTo(JsSuccess(true))
    }

    "invalidate wrong simple type conversion" in {
      JsString("string").validate[Long] must equalTo(JsError(JsString("string"), JsPath() -> Seq(ValidationError("validate.error.expected.jsnumber"))))
      JsNumber(5).validate[String] must equalTo(JsError(JsNumber(5), JsPath() -> Seq(ValidationError("validate.error.expected.jsstring"))))
      JsBoolean(false).validate[Double] must equalTo(JsError(JsBoolean(false), JsPath() -> Seq(ValidationError("validate.error.expected.jsnumber"))))
    }

    "validate simple numbered type conversion" in {
      JsNumber(5).validate[Double] must equalTo(JsSuccess(5.0))
      JsNumber(5.123).validate[Int] must equalTo(JsSuccess(5))
      JsNumber(BigDecimal(5)).validate[Double] must equalTo(JsSuccess(5.0))
      JsNumber(5.123).validate[BigDecimal] must equalTo(JsSuccess(BigDecimal(5.123)))
    }

    "validate JsObject to Map" in {
      Json.obj("key1" -> "value1", "key2" -> "value2").validate[Map[String, String]] must equalTo(JsSuccess(Map("key1" -> "value1", "key2" -> "value2")))
      Json.obj("key1" -> 5, "key2" -> 3).validate[Map[String, Int]] must equalTo(JsSuccess(Map("key1" -> 5, "key2" -> 3)))
      Json.obj("key1" -> 5.123, "key2" -> 3.543).validate[Map[String, Float]] must equalTo(JsSuccess(Map("key1" -> 5.123F, "key2" -> 3.543F)))
      Json.obj("key1" -> 5.123, "key2" -> 3.543).validate[Map[String, Double]] must equalTo(JsSuccess(Map("key1" -> 5.123, "key2" -> 3.543)))
    }

    "invalidate JsObject to Map with wrong type conversion" in {
      Json.obj("key1" -> "value1", "key2" -> "value2", "key3" -> "value3").validate[Map[String, Int]] must equalTo(
        JsError(
          Json.obj("key1" -> "value1", "key2" -> "value2", "key3" -> "value3"),
          JsPath \ "key1" -> Seq(ValidationError("validate.error.expected.jsnumber")),
          JsPath \ "key2" -> Seq(ValidationError("validate.error.expected.jsnumber")),
          JsPath \ "key3" -> Seq(ValidationError("validate.error.expected.jsnumber"))
        )
      )

      Json.obj("key1" -> "value1", "key2" -> 5, "key3" -> true).validate[Map[String, Int]] must equalTo(
        JsError(
          Json.obj("key1" -> "value1", "key2" -> 5, "key3" -> true),
          JsPath \ "key1" -> Seq(ValidationError("validate.error.expected.jsnumber")),
          JsPath \  "key3" -> Seq(ValidationError("validate.error.expected.jsnumber"))
        )
      )
    }

    "validate JsArray to List" in {
      Json.arr("alpha", "beta", "delta").validate[List[String]] must equalTo(JsSuccess(List("alpha", "beta", "delta")))
      Json.arr(123, 567, 890).validate[List[Int]] must equalTo(JsSuccess(List(123, 567, 890)))
      Json.arr(123.456, 567.123, 890.654).validate[List[Int]] must equalTo(JsSuccess(List(123, 567, 890)))
      Json.arr(123.456, 567.123, 890.654).validate[List[Double]] must equalTo(JsSuccess(List(123.456, 567.123, 890.654)))
    }

    "invalidate JsArray to List with wrong type conversion" in {
      Json.arr("alpha", "beta", "delta").validate[List[Int]] must equalTo(
        JsError(
          Json.arr("alpha", "beta", "delta"),
          JsPath(0) -> Seq(ValidationError("validate.error.expected.jsnumber")),
          JsPath(1) -> Seq(ValidationError("validate.error.expected.jsnumber")),
          JsPath(2) -> Seq(ValidationError("validate.error.expected.jsnumber"))
        )
      )

      Json.arr("alpha", 5, true).validate[List[Int]] must equalTo(
        JsError(
          Json.arr("alpha", 5, true),
          JsPath(0) -> Seq(ValidationError("validate.error.expected.jsnumber")),
          JsPath(2) -> Seq(ValidationError("validate.error.expected.jsnumber"))
        )
      )
    }

    "custom validate reads" in {
      implicit object myReads extends Reads[(Int, String, List[Float])] {
        def reads(js: JsValue) = {
          product(
            (js \ "key1").validate[Int],
            (js \ "key2").validate[String],
            (js \ "key3").validate[List[Float]]
          ).rebase(js)
        }
      }

      val obj = Json.obj("key1" -> 5, "key2" -> "blabla", "key3" -> List(1.234F, 4.543F, 8.987F))
      obj.validate[(Int, String, List[Float])] must equalTo(JsSuccess((5, "blabla", List(1.234F, 4.543F, 8.987F))))

      val badObj = Json.obj("key1" -> 5, "key2" -> true, "key3" -> List(1.234F, 4.543F, 8.987F))
      // AT THE END SHOULD BE badObj.validate[(Int, String, List[Float])] must equalTo(JsError(badObj, JsErrorObj(JsBoolean(true), "validate.error.expected.jsstring")))
      badObj.validate[(Int, String, List[Float])] must equalTo(JsError(badObj, JsPath() -> Seq(ValidationError("validate.error.expected.jsstring"))))
    }

  }

  case class User(name: String)

  def minLength(length: Int): Constraint[String] = Constraint[String]("constraint.js.minLength", length) { 
    case js @ JsString(s) => if (s.size >= length) JsSuccess(s) else JsError(js, JsPath() -> Seq(ValidationError("validate.error.minLength", JsNumber(length))))
    case js => JsError(js, JsPath() -> Seq(ValidationError("error.expected.jsstring")))
  }
  
  implicit val UserFormat = JsMapper(
    JsPath \ "name" -> minLength(5)
  )(User)(User.unapply)


  "JSON validators" should {
    "validate case class" in {
      val bobby = User("bobby")
      val js = Json.toJson(bobby)
      js.validate[User] must equalTo(JsSuccess(bobby))
    }

    "fail validation when constraints are not respected " in {
      val obj = Json.obj("name" -> 5)
      obj.validate[User] must equalTo(JsError(obj, JsPath \ 'name -> Seq(ValidationError("error.expected.jsstring"))))
    }

    "fail validation when rules are not respected " in {
      val bob = User("bob")
      val js = Json.toJson(bob)
      // SHOULD BE AT THE END js.validate[User] must equalTo(JsError(js, Json.obj("name" -> JsErrorObj(JsString("bob"), "validation.error.minLength", JsNumber(5)))))
      js.validate[User] must equalTo(JsError(js, JsPath \ "name" -> Seq(ValidationError("validate.error.minLength", JsNumber(5)))))
    }

    "fail validation when field missing" in {
      val bob = User("bob")
      val js = Json.obj("nick" -> "bob")
      js.validate[User] must equalTo(
        JsError(
          js, 
          JsPath \ "name" -> Seq(ValidationError("validation.error.missing-path"))))
    }
  }

}