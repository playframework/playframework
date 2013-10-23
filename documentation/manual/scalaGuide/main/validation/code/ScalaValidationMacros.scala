/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.validation

import org.specs2.mutable.Specification

object ScalaValidationMacrosSpec extends Specification {

  //#macro-person-def
  case class Person(name: String, age: Int, lovesChocolate: Boolean)
  //#macro-person-def

  "Scala Validation Macros" should {

    "allow manual definition" in {
      //#macro-manual
      import play.api.libs.json._
      import play.api.data.mapping._

      implicit val personRule = From[JsValue] { __ =>
        import play.api.data.mapping.json.Rules._
        ((__ \ "name").read[String] and
         (__ \ "age").read[Int] and
         (__ \ "lovesChocolate").read[Boolean])(Person.apply _)
      }
      //#macro-manual

      //#macro-manual-test
      val json = Json.parse("""{
        "name": "Julien",
        "age": 28,
        "lovesChocolate": true
      }""")

      personRule.validate(json) == Success(Person("Julien",28,true))
      //#macro-manual-test
    }

    "generate a Person validation" in {
      //#macro-macro
      import play.api.libs.json._
      import play.api.data.mapping._

      implicit val personRule = {
        import play.api.data.mapping.json.Rules._ // let's no leak implicits everywhere
        Rule.gen[JsValue, Person]
      }
      //#macro-macro

      //#macro-macro-test
      val json = Json.parse("""{
        "name": "Julien",
        "age": 28,
        "lovesChocolate": true
      }""")

      personRule.validate(json) == Success(Person("Julien",28,true))
      //#macro-macro-test
    }

    "generate a Person write" in {
      //#macro-write
      import play.api.libs.json._
      import play.api.data.mapping._

      implicit val personWrite = {
        import play.api.data.mapping.json.Writes._ // let's no leak implicits everywhere
        Write.gen[Person, JsObject]
      }

      personWrite.writes(Person("Julien", 28, true)) === Json.parse("""{"name":"Julien","age":28,"lovesChocolate":true}""")
      //#macro-write
    }

  }
}
