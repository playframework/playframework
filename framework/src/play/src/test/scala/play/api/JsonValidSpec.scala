package play.api.libs.json

import org.specs2.mutable._
import play.api.libs.json._
import play.api.libs.json.Json._
import play.api.libs.json.Generic._
import play.api.libs.json.JsResultHelpers._
import play.api.libs.json.Reads._
import play.api.libs.json.JsValidator._
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

  "JSON JsMapper 1-field case class" should {
    case class User(name: String)
    implicit val UserFormat = JsMapper(
      JsPath \ "name" -> in( minLength(5) )
    )(User)(User.unapply)

    "validate simple case class" in {
      val bobby = User("bobby")
      val js = Json.toJson(bobby)
      js.validate[User] must equalTo(JsSuccess(bobby))
    }

    "fail validation when type are not respected " in {
      val obj = Json.obj("name" -> 5)
      obj.validate[User] must equalTo(JsError(obj, JsPath \ 'name -> Seq(ValidationError("validate.error.expected.jsstring"))))
    }

    "fail validation when constraints are not respected " in {
      val bob = User("bob")
      val js = Json.toJson(bob)
      // SHOULD BE AT THE END js.validate[User] must equalTo(JsError(js, Json.obj("name" -> JsErrorObj(JsString("bob"), "validate.error.minLength", JsNumber(5)))))
      js.validate[User] must equalTo(JsError(js, JsPath \ "name" -> Seq(ValidationError("validate.error.minLength", 5))))
    }

    "fail validation when field missing" in {
      val bob = User("bob")
      val js = Json.obj("nick" -> "bob")
      js.validate[User] must equalTo(
        JsError(
          js, 
          JsPath \ "name" -> Seq(ValidationError("validate.error.missing-path"), ValidationError("validate.error.expected.jsstring"))))
    }
  }


  "JSON JsMapper 3-fields case class" should {
    case class User2(id: Long, name: String, age: Int)
    implicit val UserFormat2 = JsMapper(
      JsPath \ 'id -> in( of[Long] ),
      JsPath \ 'name -> in( minLength(5) ),
      JsPath \ 'age -> in( max(85) )
    )(User2)(User2.unapply)

    "validate simple case class" in {
      val bobby = User2(1234L, "bobby", 75)
      val js = Json.toJson(bobby)
      js.validate[User2] must equalTo(JsSuccess(bobby))
    }

    "fail validation when type are not respected " in {
      val obj = Json.obj("id" -> 1234L, "name" -> 5, "age" -> "blabla")
      obj.validate[User2] must equalTo(JsError(obj, 
        JsPath \ 'name -> Seq(ValidationError("validate.error.expected.jsstring")), 
        JsPath \ 'age -> Seq(ValidationError("validate.error.expected.jsnumber"))))
    }

    "fail validation when constraints are not respected " in {
      val bob = User2(1234L, "bob", 86)
      val js = Json.toJson(bob)
      // SHOULD BE AT THE END js.validate[User] must equalTo(JsError(js, Json.obj("name" -> JsErrorObj(JsString("bob"), "validate.error.minLength", JsNumber(5)))))
      js.validate[User2] must equalTo(JsError(js, 
        JsPath \ "name" -> Seq(ValidationError("validate.error.minLength", 5)), 
        JsPath \ "age" -> Seq(ValidationError("validate.error.max", 85))))
    }

    "fail validation when field missing" in {
      val js = Json.obj("id" -> 1234L, "nick" -> "bob")
      js.validate[User2] must equalTo(
        JsError(
          js, 
          JsPath \ "name" -> Seq(ValidationError("validate.error.missing-path"), ValidationError("validate.error.expected.jsstring")),
          JsPath \ "age" -> Seq(ValidationError("validate.error.missing-path"), ValidationError("validate.error.expected.jsnumber"))
        ))
    }


  }

  "JSON JsMapper 3-fields case class with multiple constraints" should {
    case class User3(id: Long, name: String, age: Int)
    implicit val UserFormat3 = JsMapper(
      JsPath \ 'id -> in( of[Long] ),
      JsPath \ 'name -> in( minLength(5) or valueEquals[String]("John") ),
      JsPath \ 'age -> in( max(85) and min(15) )
    )(User3)(User3.unapply)

    "validate simple case class" in {
      val bobby = User3(1234L, "bobby", 75)
      val js = Json.toJson(bobby)
      js.validate[User3] must equalTo(JsSuccess(bobby))
    }

    "fail validation when type are not respected " in {
      val obj = Json.obj("id" -> 1234L, "name" -> 5, "age" -> "blabla")
      obj.validate[User3] must equalTo(JsError(obj, 
        JsPath \ 'name -> Seq(ValidationError("validate.error.expected.jsstring")), 
        JsPath \ 'age -> Seq(ValidationError("validate.error.expected.jsnumber"))))
    }

    "fail validation when constraints are NOT respected " in {
      val bob = User3(1234L, "bob", 86)
      val js = Json.toJson(bob)
      // SHOULD BE AT THE END js.validate[User] must equalTo(JsError(js, Json.obj("name" -> JsErrorObj(JsString("bob"), "validate.error.minLength", JsNumber(5)))))
      js.validate[User3] must equalTo(JsError(js, 
        JsPath \ "name" -> Seq(ValidationError("validate.error.minLength", 5), ValidationError("validate.error.equals", "John")), 
        JsPath \ "age" -> Seq(ValidationError("validate.error.max", 85))))
    }

    "fail validation when OR constraints are respected " in {
      val bob = User3(1234L, "John", 86)
      val js = Json.toJson(bob)
      // SHOULD BE AT THE END js.validate[User] must equalTo(JsError(js, Json.obj("name" -> JsErrorObj(JsString("bob"), "validate.error.minLength", JsNumber(5)))))
      js.validate[User3] must equalTo(JsError(js, 
        JsPath \ "age" -> Seq(ValidationError("validate.error.max", 85))))
    }

    "fail validation when field missing" in {
      val js = Json.obj("id" -> 1234L, "nick" -> "bob")
      js.validate[User3] must equalTo(
        JsError(
          js, 
          JsPath \ "name" -> Seq(ValidationError("validate.error.missing-path"), ValidationError("validate.error.expected.jsstring")),
          JsPath \ "age" -> Seq(ValidationError("validate.error.missing-path"), ValidationError("validate.error.expected.jsnumber"))
        ))
    }

    
  }  


  "JSON required constraint" should {
    case class User4(id: Long, name: String, age: Int)
    implicit val UserFormat4 = JsMapper(
      JsPath \ 'id -> in( of[Long] ),
      JsPath \ 'name -> in( required[String] and minLength(5) ),
      JsPath \ 'age -> in( max(85) and min(15) )
    )(User4)(User4.unapply)

   "validate missing field" in {
      val obj = Json.obj("id" -> 1234L, "age" -> "blabla")
      obj.validate[User4] must equalTo(
        JsError(
          obj, 
          JsPath \ "name" -> Seq(ValidationError("validate.error.missing-path"), ValidationError("validate.error.required"), ValidationError("validate.error.expected.jsstring")),
          JsPath \ "age" -> Seq(ValidationError("validate.error.expected.jsnumber"))
        ))
    }

    "validate failed constraints" in {
      val obj = Json.obj("id" -> 1234L, "name" -> "bob", "age" -> 5)
      obj.validate[User4] must equalTo(
        JsError(
          obj, 
          JsPath \ "name" -> Seq(ValidationError("validate.error.minLength", 5)),
          JsPath \ "age" -> Seq(ValidationError("validate.error.min", 15))
        ))
    }
  }


  "JSON optional constraint" should {
    case class User5(id: Long, name: String, age: Option[Int])
    implicit val UserFormat5 = JsMapper(
      JsPath \ 'id -> in( of[Long] ),
      JsPath \ 'name -> in( required[String] ),
      JsPath \ 'age -> in( optional[Int] )
    )(User5)(User5.unapply)

    "validate missing optional field" in {
      val obj = Json.obj("id" -> 1234L, "name" -> "bob")
      obj.validate[User5] must equalTo(JsSuccess(User5(1234L, "bob", None)))
    }

    "validate optional field" in {
      val obj = Json.obj("id" -> 1234L, "name" -> "bob", "age" -> 5)
      obj.validate[User5] must equalTo(JsSuccess(User5(1234L, "bob", Some(5))))
    }

     "validate other than optional missing field" in {
      val obj = Json.obj("id" -> 1234L)
      obj.validate[User5] must equalTo(
        JsError(
          obj, 
          JsPath \ "name" -> Seq(ValidationError("validate.error.missing-path"), ValidationError("validate.error.required"), ValidationError("validate.error.expected.jsstring"))
        ))
    }
  }

  "JSON email constraint" should {
    val myFormat = JsValidator(
      JsPath \ 'email -> in(email)
    )

    "validate email" in {
      val obj = Json.obj("email" -> "pascal.voitot@zenexity.com")
      obj.validate[String](myFormat) must equalTo(JsSuccess("pascal.voitot@zenexity.com"))
    }

    "refuse email" in {
      val obj = Json.obj("email" -> "pascal.voitotnexity.com")
      obj.validate[String](myFormat) must equalTo(JsError(obj, JsPath \ "email" -> Seq(ValidationError("validate.error.email"))))
    }
  }    


  "JSON JsValidator 3-fields" should {
    implicit val myFormat = JsValidator(
      JsPath \ 'id -> in[Long],
      JsPath \ 'name -> in( required[String] ),
      JsPath \ 'password -> (in( required[String] ) ++ out( pruned[String] ))
    )

    "validate json to tuple" in {
      val obj = Json.obj("id" -> 1234L, "name" -> "bob", "password" -> "password")
      obj.validate[(Long, String, String)] must equalTo(JsSuccess((1234L, "bob", "password")))
    }

    "rewrite tuple to json" in {
      val obj = Json.obj("id" -> 1234L, "name" -> "bob")

      toJson((1234L, "bob", "password")) must equalTo(obj)
    }
  }

}