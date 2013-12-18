/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.validation

import org.specs2.mutable.Specification

object ScalaValidationJsonSpec extends Specification with play.api.mvc.Controller {

  //#validation-json
  import play.api.libs.json._

  val json: JsValue = Json.parse("""
  {
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
  }
  """)
  //#validation-json

  //#validation-json-path
  import play.api.data.mapping._
  val location: Path = Path \ "user" \ "friend"
  //#validation-json-path

  //#validation-json-import
  import play.api.data.mapping.json.Rules._
  //#validation-json-import

  "Scala Validation Json" should {
    "find friend" in {
      //#validation-json-findfriend-def
      import play.api.data.mapping.json.Rules._
      val findFriend: Rule[JsValue, JsValue] = location.read[JsValue, JsValue]
      //#validation-json-findfriend-def

      //#validation-json-findfriend-test
      findFriend.validate(json) === Success(Json.parse("""{"name":"tata","age":20,"email":"tata@coldmail.com"}"""))
      //#validation-json-findfriend-test

      //#validation-json-findfriend-fail
      (Path \ "somenonexistinglocation").read[JsValue, JsValue].validate(json) === Failure(Seq((Path \ "somenonexistinglocation", Seq(ValidationError("error.required")))))
      //#validation-json-findfriend-fail
    }
  }
}
