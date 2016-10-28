/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.json

import play.api.data.validation.ValidationError
import play.api.libs.json.Json

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner


@RunWith(classOf[JUnitRunner])
class ScalaJsonAutomatedSpec extends Specification {

  //#model
  case class Resident(name: String, age: Int, role: Option[String])
  //#model


  val sampleJson = Json.parse(
    """{
      "name" : "Fiver",
      "age" : 4
    }"""
  )
  val sampleData = Resident("Fiver", 4, None)

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

      sampleJson.as[Resident] must_=== sampleData
      Json.toJson(sampleData) must_=== sampleJson
    }

    "automatically serialize a case class to JSON" in {
      //#auto-case-class-to-JSON
      import play.api.libs.json._

      implicit val residentWrites = Json.writes[Resident]

      val resident = Resident(name="Fiver", age=4, role=None)

      val residentJson: JsValue = Json.toJson(resident)
      //#auto-case-class-to-JSON

      residentJson must_=== sampleJson
    }

    "automatically convert JSON to a case class" in {
      //#auto-JSON-to-case-class
      import play.api.libs.json._

      implicit val residentReads = Json.reads[Resident]

      // In a request, a JsValue is likely to come from `request.body.asJson`
      // or just `request.body` if using the `Action(parse.json)` body parser
      val jsonString: JsValue = Json.parse(
        """{
          "name" : "Fiver",
          "age" : 4
        }""")

      val residentFromJson: JsResult[Resident] = Json.fromJson[Resident](jsonString)

      residentFromJson match {
        case JsSuccess(r: Resident, path: JsPath) => println("Name: " + r.name)
        case e: JsError => println("Errors: " + JsError.toJson(e).toString())
      }
      //#auto-JSON-to-case-class

      residentFromJson.get must_=== sampleData
    }
  }

}
