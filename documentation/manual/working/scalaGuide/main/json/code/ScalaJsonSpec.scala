/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.json

import play.api.data.validation.ValidationError

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ScalaJsonSpec extends Specification {

  val sampleJson = {
    //#convert-from-string
      import play.api.libs.json._

      val json: JsValue = Json.parse("""
      {
        "name" : "Watership Down",
        "location" : {
          "lat" : 51.235685,
          "long" : -1.309197
        },
        "residents" : [ {
          "name" : "Fiver",
          "age" : 4,
          "role" : null
        }, {
          "name" : "Bigwig",
          "age" : 6,
          "role" : "Owsla"
        } ]
      }
      """)
      //#convert-from-string
      json
  }

  object SampleModel {
    //#sample-model
    case class Location(lat: Double, long: Double)
    case class Resident(name: String, age: Int, role: Option[String])
    case class Place(name: String, location: Location, residents: Seq[Resident])
    //#sample-model
  }

  "Scala JSON" should {
    "parse json" in {
      import play.api.libs.json._
      val json = sampleJson
      (json \ "name").get must_== JsString("Watership Down")
      (json \ "location" \ "lat").get must_== JsNumber(51.235685)
    }

    "allow constructing json using case classes" in {
      //#convert-from-classes
      import play.api.libs.json._

      val json: JsValue = JsObject(Seq(
        "name" -> JsString("Watership Down"),
        "location" -> JsObject(Seq("lat" -> JsNumber(51.235685), "long" -> JsNumber(-1.309197))),
        "residents" -> JsArray(Seq(
          JsObject(Seq(
            "name" -> JsString("Fiver"),
            "age" -> JsNumber(4),
            "role" -> JsNull
          )),
          JsObject(Seq(
            "name" -> JsString("Bigwig"),
            "age" -> JsNumber(6),
            "role" -> JsString("Owsla")
          ))
        ))
      ))
      //#convert-from-classes
      (json \ "name").get must_== JsString("Watership Down")
    }

    "allow constructing json using factory methods" in {
      //#convert-from-factory
      import play.api.libs.json.{JsNull,Json,JsString,JsValue}

      val json: JsValue = Json.obj(
        "name" -> "Watership Down",
        "location" -> Json.obj("lat" -> 51.235685, "long" -> -1.309197),
        "residents" -> Json.arr(
          Json.obj(
            "name" -> "Fiver",
            "age" -> 4,
            "role" -> JsNull
          ),
          Json.obj(
            "name" -> "Bigwig",
            "age" -> 6,
            "role" -> "Owsla"
          )
        )
      )
      //#convert-from-factory
      (json \ "name").get must_== JsString("Watership Down")
    }

    "allow converting simple types" in {
      //#convert-from-simple
      import play.api.libs.json._

      // basic types
      val jsonString = Json.toJson("Fiver")
      val jsonNumber = Json.toJson(4)
      val jsonBoolean = Json.toJson(false)

      // collections of basic types
      val jsonArrayOfInts = Json.toJson(Seq(1, 2, 3, 4))
      val jsonArrayOfStrings = Json.toJson(List("Fiver", "Bigwig"))

      //#convert-from-simple

      jsonString === JsString("Fiver")
      jsonNumber === JsNumber(4)
      jsonBoolean === JsBoolean(false)
      jsonArrayOfInts === Json.arr(1, 2, 3, 4)
      jsonArrayOfStrings === Json.arr("Fiver", "Bigwig")
    }

    "allow converting of models" in {

      import SampleModel._

      //#convert-from-model
      import play.api.libs.json._

      implicit val locationWrites = new Writes[Location] {
        def writes(location: Location) = Json.obj(
          "lat" -> location.lat,
          "long" -> location.long
        )
      }

      implicit val residentWrites = new Writes[Resident] {
        def writes(resident: Resident) = Json.obj(
          "name" -> resident.name,
          "age" -> resident.age,
          "role" -> resident.role
        )
      }

      implicit val placeWrites = new Writes[Place] {
        def writes(place: Place) = Json.obj(
          "name" -> place.name,
          "location" -> place.location,
          "residents" -> place.residents)
      }

      val place = Place(
        "Watership Down",
        Location(51.235685, -1.309197),
        Seq(
          Resident("Fiver", 4, None),
          Resident("Bigwig", 6, Some("Owsla"))
        )
      )

      val json = Json.toJson(place)
      //#convert-from-model

      (json \ "name").get === JsString("Watership Down")
    }

    "allow converting models preferred" in {

      import SampleModel._

      //#convert-from-model-prefwrites
      import play.api.libs.json._
      import play.api.libs.functional.syntax._

      implicit val locationWrites: Writes[Location] = (
        (JsPath \ "lat").write[Double] and
        (JsPath \ "long").write[Double]
      )(unlift(Location.unapply))

      implicit val residentWrites: Writes[Resident] = (
        (JsPath \ "name").write[String] and
        (JsPath \ "age").write[Int] and
        (JsPath \ "role").writeNullable[String]
      )(unlift(Resident.unapply))

      implicit val placeWrites: Writes[Place] = (
        (JsPath \ "name").write[String] and
        (JsPath \ "location").write[Location] and
        (JsPath \ "residents").write[Seq[Resident]]
      )(unlift(Place.unapply))
      //#convert-from-model-prefwrites

      val place = Place(
        "Watership Down",
        Location(51.235685, -1.309197),
        Seq(
          Resident("Fiver", 4, None),
          Resident("Bigwig", 6, Some("Owsla"))
        )
      )

      val json = Json.toJson(place)
      //#convert-from-model

      (json \ "name").get === JsString("Watership Down")
    }


    "allow traversing JsValue tree" in {

      import play.api.libs.json._
      val json = sampleJson

      //#traverse-simple-path
      val lat = (json \ "location" \ "lat").get
      // returns JsNumber(51.235685)
      //#traverse-simple-path

      lat === JsNumber(51.235685)

      //#traverse-recursive-path
      val names = json \\ "name"
      // returns Seq(JsString("Watership Down"), JsString("Fiver"), JsString("Bigwig"))
      //#traverse-recursive-path
      names === Seq(JsString("Watership Down"), JsString("Fiver"), JsString("Bigwig"))

      //#traverse-array-index
      val bigwig = (json \ "residents")(1)
      // returns {"name":"Bigwig","age":6,"role":"Owsla"}
      //#traverse-array-index
      (bigwig \ "name").get === JsString("Bigwig")
    }

    "allow converting JsValue to String" in {

      import play.api.libs.json._
      val json = sampleJson

      //#convert-to-string
      val minifiedString: String = Json.stringify(json)
      //#convert-to-string

      //#convert-to-string-pretty
      val readableString: String = Json.prettyPrint(json)
      //#convert-to-string-pretty

      minifiedString must contain("Fiver")
      readableString must contain("Bigwig")
    }

    "allow converting JsValue using as" in {

      import play.api.libs.json._
      val json = sampleJson

      //#convert-to-type-as
      val name = (json \ "name").as[String]
      // "Watership Down"

      val names = (json \\ "name").map(_.as[String])
      // Seq("Watership Down", "Fiver", "Bigwig")
      //#convert-to-type-as

      name === "Watership Down"
      names === Seq("Watership Down", "Fiver", "Bigwig")
    }

    "allow converting JsValue using asOpt" in {

      import play.api.libs.json._
      val json = sampleJson

      //#convert-to-type-as-opt
      val nameOption = (json \ "name").asOpt[String]
      // Some("Watership Down")

      val bogusOption = (json \ "bogus").asOpt[String]
      // None
      //#convert-to-type-as-opt

      nameOption === Some("Watership Down")
      bogusOption must beNone
    }

    "allow converting JsValue using validate" in {
      import SampleModel._

      import play.api.libs.json._
      import play.api.libs.json.Reads._

      //#convert-to-type-validate
      //###replace: val json = { ... }
      val json: JsValue = sampleJson

      val nameResult: JsResult[String] = (json \ "name").validate[String]

      // Pattern matching
      nameResult match {
        case s: JsSuccess[String] => println("Name: " + s.get)
        case e: JsError => println("Errors: " + JsError.toFlatJson(e).toString())
      }

      // Fallback value
      val nameOrFallback = nameResult.getOrElse("Undefined")

      // map
      val nameUpperResult: JsResult[String] = nameResult.map(_.toUpperCase())

      // fold
      val nameOption: Option[String] = nameResult.fold(
        invalid = {
          fieldErrors => fieldErrors.foreach(x => {
            println("field: " + x._1 + ", errors: " + x._2)
          })
          None
        },
        valid = {
          name => Some(name)
        }
      )
      //#convert-to-type-validate
      nameResult must beLike {case x: JsSuccess[String] =>  x.get === "Watership Down"}
    }

    "allow converting JsValue to model" in {

      import SampleModel._

      //#convert-to-model
      import play.api.libs.json._
      import play.api.libs.functional.syntax._

      implicit val locationReads: Reads[Location] = (
        (JsPath \ "lat").read[Double] and
        (JsPath \ "long").read[Double]
      )(Location.apply _)

      implicit val residentReads: Reads[Resident] = (
        (JsPath \ "name").read[String] and
        (JsPath \ "age").read[Int] and
        (JsPath \ "role").readNullable[String]
      )(Resident.apply _)

      implicit val placeReads: Reads[Place] = (
        (JsPath \ "name").read[String] and
        (JsPath \ "location").read[Location] and
        (JsPath \ "residents").read[Seq[Resident]]
      )(Place.apply _)


      //###replace: val json = { ... }
      val json = sampleJson

      val placeResult: JsResult[Place] = json.validate[Place]
      // JsSuccess(Place(...),)

      val residentResult: JsResult[Resident] = (json \ "residents")(1).validate[Resident]
      // JsSuccess(Resident(Bigwig,6,Some(Owsla)),)
      //#convert-to-model

      placeResult must beLike {case x: JsSuccess[Place] =>  x.get.name === "Watership Down"}
      residentResult must beLike {case x: JsSuccess[Resident] =>  x.get.name === "Bigwig"}
    }

  }

}
