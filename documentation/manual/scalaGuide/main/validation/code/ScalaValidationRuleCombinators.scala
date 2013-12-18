/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.validation

import org.specs2.mutable.Specification

object ScalaValidationRuleCombinatorsSpec extends Specification {

  //#combinators-path
  import play.api.data.mapping.Path
  val path = Path \ "foo" \ "bar"
  //#combinators-path

  //#combinators-path-index
  val pi = Path \ "foo" \ 0
  //#combinators-path-index

  //#combinators-js
  import play.api.libs.json._

  val js: JsValue = Json.parse("""{
    "user": {
      "name" : "toto",
      "age" : 25,
      "email" : "toto@jmail.com",
      "isAlive" : true,
      "friend" : {
        "name" : "tata",
        "age" : 20,
        "email" : "tata@coldmail.com"
      }
    }
  }""")
  //#combinators-js

  "Scala Validation Rule Combinators" should {

    //#combinators-path-location
    import play.api.data.mapping._
    val location: Path = Path \ "user" \ "friend"
    //#combinators-path-location

    "extract data" in {
      import play.api.data.mapping.Rules

      //#rule-extract-import
      import play.api.data.mapping.json.Rules._
      //#rule-extract-import

      //#rule-extract
      import play.api.libs.json.JsValue
      import play.api.data.mapping._

      val location: Path = Path \ "user" \ "friend"
      val findFriend: Rule[JsValue, JsValue] = location.read[JsValue, JsValue]
      //#rule-extract

      //#rule-extract-test
      findFriend.validate(js) === Success(Json.parse("""{"name":"tata","age":20,"email":"tata@coldmail.com"}"""))
      //#rule-extract-test

      //#rule-extract-fail
      (Path \ "foobar").read[JsValue, JsValue].validate(js) === Failure(Seq((Path \ "foobar", Seq(ValidationError("error.required")))))
      //#rule-extract-fail
    }

    "extract age" in {
      //#rule-extract-age
      import play.api.data.mapping._
      import play.api.data.mapping.json.Rules._

      val age = (Path \ "user" \ "age").read[JsValue, JsValue]
      //#rule-extract-age

      //#rule-extract-age-test
      age.validate(js) === Success(JsNumber(25))
      //#rule-extract-age-test

      //#rule-extract-age-fail
      age.validate(Json.obj()) === Failure(Seq((Path \ "user" \ "age", Seq(ValidationError("error.required")))))
      //#rule-extract-age-fail
    }

    "extract ageas Int" in {
      import play.api.data.mapping._
      import play.api.data.mapping.json.Rules._

      //#rule-extract-ageInt
      val age = (Path \ "user" \ "age").read[JsValue, Int]
      //#rule-extract-ageInt

      //#rule-extract-ageInt-test
      age.validate(js) === Success(25)
      //#rule-extract-ageInt-test

      //#rule-extract-ageInt-fail
      (Path \ "user" \ "name").read[JsValue, Int].validate(js) === Failure(Seq((Path \ "user" \ "name", Seq(ValidationError("error.invalid", "Int")))))
      //#rule-extract-ageInt-fail
    }

    "validate business rules" in {
      import play.api.data.mapping.json.Rules._

      //#rule-validate-js
      val js = Json.parse("""{
        "user": {
          "age" : -33
        }
      }""")

      val age = (Path \ "user" \ "age").read[JsValue, Int]
      //#rule-validate-js

      //#rule-validate-testNeg
      age.validate(js) === Success(-33)
      //#rule-validate-testNeg

      //#rule-validate-pos
      val positiveAge = (Path \ "user" \ "age").from[JsValue](min(0))
      //#rule-validate-pos

      //#rule-validate-pos-test
      positiveAge.validate(js) === Failure(Seq((Path \ "user" \ "age", Seq(ValidationError("error.min", 0)))))
      //#rule-validate-pos-test

      //#rule-validate-pos-big
      val js2 = Json.parse("""{ "user": { "age" : 8765 } }""")
      positiveAge.validate(js2) === Success(8765)
      //#rule-validate-pos-big

      //#rule-validate-proper
      val properAge = (Path \ "user" \ "age").from[JsValue](min(0) |+| max(130))
      //#rule-validate-proper

      //#rule-validate-proper-test
      val jsBig = Json.parse("""{ "user": { "age" : 8765 } }""")
      properAge.validate(jsBig) === Failure(Seq((Path \ "user" \ "age", Seq(ValidationError("error.max", 130)))))
      //#rule-validate-proper-test
    }

    "validate full" in {
      //#rule-validate-full
      import play.api.libs.json.Json
      import play.api.data.mapping._
      import play.api.data.mapping.json.Rules._

      val js = Json.parse("""{
        "user": {
          "name" : "toto",
          "age" : 25,
          "email" : "toto@jmail.com",
          "isAlive" : true,
          "friend" : {
            "name" : "tata",
            "age" : 20,
            "email" : "tata@coldmail.com"
          }
        }
      }""")

      val age = (Path \ "user" \ "age").from[JsValue](min(0) |+| max(130))
      age.validate(js) === Success(25)
      //#rule-validate-full
    }

    "combine rules" in {

      //#rule-combine-user
      case class User(
        name: String,
        age: Int,
        email: Option[String],
        isAlive: Boolean)
      //#rule-combine-user

      //#rule-combine-rule
      import play.api.libs.json._
      import play.api.data.mapping._

      val userRule = From[JsValue] { __ =>
        import play.api.data.mapping.json.Rules._
        ((__ \ "name").read[String] and
         (__ \ "age").read[Int] and
         (__ \ "email").read[Option[String]] and
         (__ \ "isAlive").read[Boolean])(User.apply _)
      }
      //#rule-combine-rule

      success
    }

  }
}
