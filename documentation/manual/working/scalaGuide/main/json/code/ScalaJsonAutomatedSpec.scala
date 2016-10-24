/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.json

import play.api.data.validation.ValidationError
import play.api.libs.json.Json
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.libs.json.JsonNaming.SnakeCase


@RunWith(classOf[JUnitRunner])
class ScalaJsonAutomatedSpec extends Specification {

  //#model
  case class Resident(name: String, age: Int, role: Option[String])
  //#model

  //#model2
  case class PlayUser(name: String, firstName: String, userAge: Int)
  //#model2

  val sampleJson = Json.parse(
    """{
      "name" : "Fiver",
      "age" : 4
    }"""
  )
  val sampleData = Resident("Fiver", 4, None)

  val sampleJson2 = Json.parse(
    """{
      "name": "Schmitt",
      "first_name": "Christian",
      "user_age": 26
    }"""
  )
  val sampleJson3 = Json.parse(
    """{
      "lightbend_name": "Schmitt",
      "lightbend_firstName": "Christian",
      "lightbend_userAge": 26
    }"""
  )
  val sampleData2 = PlayUser("Schmitt", "Christian", 26)

  "Scala JSON automated" should {
    "produce a working Reads" in {

      //#auto-reads
      import play.api.libs.json._

      implicit val residentReads = Json.reads[Resident]
      //#auto-reads

      sampleJson.as[Resident] must_=== sampleData
    }
    "do the same thing as a manual Reads" in {

      //#manual-reads
      import play.api.libs.json._
      import play.api.libs.functional.syntax._

      implicit val residentReads = (
        (__ \ "name").read[String] and
        (__ \ "age").read[Int] and
        (__ \ "role").readNullable[String]
      )(Resident)
      //#manual-reads

      sampleJson.as[Resident] must_=== sampleData
    }
    "produce a working Writes" in {

      //#auto-writes
      import play.api.libs.json._

      implicit val residentWrites = Json.writes[Resident]
      //#auto-writes

      Json.toJson(sampleData) must_=== sampleJson
    }
    "produce a working Format" in {

      //#auto-format
      import play.api.libs.json._

      implicit val residentFormat = Json.format[Resident]
      //#auto-format

      sampleJson.as[Resident] must_=== sampleData and {
        Json.toJson(sampleData) must_=== sampleJson
      }
    }

    "produce a working Writes with SnakeCase" in {
      //#auto-naming-writes
      import play.api.libs.json._

      implicit val config = JsonConfiguration(SnakeCase)

      implicit val userWrites: OWrites[PlayUser] = Json.writes[PlayUser]
      //#auto-naming-writes

      Json.toJson(sampleData2) must_=== sampleJson2
    }

    "produce a working Format with SnakeCase" in {
      //#auto-naming-format
      import play.api.libs.json._

      implicit val config = JsonConfiguration(SnakeCase)

      implicit val userFormat: OFormat[PlayUser] = Json.format[PlayUser]
      //#auto-naming-format

      sampleJson2.as[PlayUser] must_=== sampleData2 and {
        Json.toJson(sampleData2) must_=== sampleJson2
      }
    }

    "produce a working Reads with SnakeCase" in {
      //#auto-naming-reads
      import play.api.libs.json._

      implicit val config = JsonConfiguration(SnakeCase)

      implicit val userReads: Reads[PlayUser] = Json.reads[PlayUser]
      //#auto-naming-reads

      sampleJson2.as[PlayUser] must_=== sampleData2
    }

    "produce a working Format with Custom Naming" in {
      //#auto-custom-naming-format
      import play.api.libs.json._

      object Lightbend extends JsonNaming {
        override def apply(property: String): String = s"lightbend_$property"
      }

      implicit val config = JsonConfiguration(Lightbend)

      implicit val customWrites: OFormat[PlayUser] = Json.format[PlayUser]
      //#auto-custom-naming-format

      sampleJson3.as[PlayUser] must_=== sampleData2 and {
        Json.toJson(sampleData2) must_=== sampleJson3
      }
    }

    "automatically serialize a case class to JSON" in {
      //#auto-case-class-to-JSON
      import play.api.libs.json._

      implicit val residentWrites = Json.writes[Resident]

      val resident = Resident(name="Fiver", age=4, role=None)

      Json.toJson(resident)
      //#auto-case-class-to-JSON

      Json.toJson(resident) must_=== sampleJson
    }

    "automatically convert JSON to a case class" in {
      //#auto-JSON-to-case-class
      import play.api.libs.json._

      implicit val residentReads = Json.reads[Resident]

      // In a request, a JsValue is likely to come from
      // `request.body.asJson` or just `request.body`
      // if using the `Action(parse.json)` body parser
      val jsonString: JsValue = Json.parse(
        """{
          "name" : "Fiver",
          "age" : 4
        }""")

      Json.fromJson[Resident](jsonString)
      //#auto-JSON-to-case-class

      Json.fromJson[Resident](jsonString).get must_=== sampleData
    }
  }
}
