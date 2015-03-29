/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.json

import play.api.data.validation.ValidationError

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ScalaJsonCombinatorsSpec extends Specification {

  val sampleJson = {
    //#sample-json
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
      //#sample-json
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

    "allow using JsPath" in {

      //#jspath-define
      import play.api.libs.json._

      //###replace: val json = { ... }
      val json: JsValue = sampleJson

      // Simple path
      val latPath = JsPath \ "location" \ "lat"

      // Recursive path
      val namesPath = JsPath \\ "name"

      // Indexed path
      val firstResidentPath = (JsPath \ "residents")(0)
      //#jspath-define

      //#jspath-define-alias
      val longPath = __ \ "location" \ "long"
      //#jspath-define-alias

      //#jspath-traverse
      val lat: List[JsValue] = latPath(json)
      // List(JsNumber(51.235685))
      //#jspath-traverse

      //val name = (JsPath \ "name").read[String] and (JsPath \ "location").read[Int]
      latPath.toString === "/location/lat"
      namesPath.toString === "//name"
      firstResidentPath.toString === "/residents(0)"
    }

    "allow creating simple Reads" in {

      //#reads-imports
      import play.api.libs.json._ // JSON library
      import play.api.libs.json.Reads._ // Custom validation helpers
      import play.api.libs.functional.syntax._ // Combinator syntax
      //#reads-imports

      //###replace: val json = { ... }
      val json: JsValue = sampleJson

      //#reads-simple
      val nameReads: Reads[String] = (JsPath \ "name").read[String]
      //#reads-simple

      json.validate(nameReads) must beLike {case x: JsSuccess[String] =>  x.get === "Watership Down"}
    }

    "allow creating complex Reads" in {
      import SampleModel._

      import play.api.libs.json._
      import play.api.libs.functional.syntax._

      //###replace: val json = { ... }
      val json: JsValue = sampleJson

      //#reads-complex-builder
      val locationReadsBuilder =
        (JsPath \ "lat").read[Double] and
        (JsPath \ "long").read[Double]
      //#reads-complex-builder

      //#reads-complex-buildertoreads
      implicit val locationReads = locationReadsBuilder.apply(Location.apply _)
      //#reads-complex-buildertoreads

      val locationResult = (json \ "location").validate[Location]
      locationResult must beLike {case x: JsSuccess[Location] =>  x.get.lat === 51.235685}
    }

    "allow creating complex Reads in a single statement" in {
      import SampleModel._

      import play.api.libs.json._
      import play.api.libs.functional.syntax._

      //###replace: val json = { ... }
      val json: JsValue = sampleJson

      //#reads-complex-statement
      implicit val locationReads: Reads[Location] = (
        (JsPath \ "lat").read[Double] and
        (JsPath \ "long").read[Double]
      )(Location.apply _)
      //#reads-complex-statement

      val locationResult = (json \ "location").validate[Location]
      locationResult must beLike {case x: JsSuccess[Location] =>  x.get.lat === 51.235685}
    }

    "allow validation with Reads" in {
      import SampleModel._

      import play.api.libs.json._
      import play.api.libs.json.Reads._
      import play.api.libs.functional.syntax._

      //#reads-validation-simple
      //###replace: val json = { ... }
      val json: JsValue = sampleJson

      val nameReads: Reads[String] = (JsPath \ "name").read[String]

      val nameResult: JsResult[String] = json.validate[String](nameReads)

      nameResult match {
        case s: JsSuccess[String] => println("Name: " + s.get)
        case e: JsError => println("Errors: " + JsError.toFlatJson(e).toString())
      }
      //#reads-validation-simple
      nameResult must beLike {case x: JsSuccess[String] =>  x.get === "Watership Down"}

      //#reads-validation-custom
      val improvedNameReads =
        (JsPath \ "name").read[String](minLength[String](2))
      //#reads-validation-custom
      json.validate[String](improvedNameReads) must beLike {case x: JsSuccess[String] =>  x.get === "Watership Down"}

    }

    "allow creating Reads for model" in {
      import SampleModel._

      //#reads-model
      import play.api.libs.json._
      import play.api.libs.json.Reads._
      import play.api.libs.functional.syntax._

      implicit val locationReads: Reads[Location] = (
        (JsPath \ "lat").read[Double](min(-90.0) keepAnd max(90.0)) and
        (JsPath \ "long").read[Double](min(-180.0) keepAnd max(180.0))
      )(Location.apply _)

      implicit val residentReads: Reads[Resident] = (
        (JsPath \ "name").read[String](minLength[String](2)) and
        (JsPath \ "age").read[Int](min(0) keepAnd max(150)) and
        (JsPath \ "role").readNullable[String]
      )(Resident.apply _)

      implicit val placeReads: Reads[Place] = (
        (JsPath \ "name").read[String](minLength[String](2)) and
        (JsPath \ "location").read[Location] and
        (JsPath \ "residents").read[Seq[Resident]]
      )(Place.apply _)


      //###replace: val json = { ... }
      val json: JsValue = sampleJson

      json.validate[Place] match {
        case s: JsSuccess[Place] => {
          val place: Place = s.get
          // do something with place
        }
        case e: JsError => {
          // error handling flow
        }
      }
      //#reads-model

      json.validate[Place] must beLike {case x: JsSuccess[Place] =>  x.get.name === "Watership Down"}
    }

    "allow creating Writes for model" in {
      import SampleModel._

      //#writes-model
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


      val place = Place(
        "Watership Down",
        Location(51.235685, -1.309197),
        Seq(
          Resident("Fiver", 4, None),
          Resident("Bigwig", 6, Some("Owsla"))
        )
      )

      val json = Json.toJson(place)
      //#writes-model

      val some = (JsPath \ "lat").write[Double] and (JsPath \ "long").write[Double]
      val placeSome = Place.unapply(place)

      (json \ "name").get === JsString("Watership Down")
    }

    "allow creating Reads/Writes for recursive types"  in {

      import play.api.libs.json._
      import play.api.libs.json.Reads._
      import play.api.libs.functional.syntax._

      //#reads-writes-recursive
      case class User(name: String, friends: Seq[User])

      implicit lazy val userReads: Reads[User] = (
        (__ \ "name").read[String] and
        (__ \ "friends").lazyRead(Reads.seq[User](userReads))
      )(User)

      implicit lazy val userWrites: Writes[User] = (
        (__ \ "name").write[String] and
        (__ \ "friends").lazyWrite(Writes.seq[User](userWrites))
      )(unlift(User.unapply))
      //#reads-writes-recursive

      // Use Reads for JSON -> model
      val json: JsValue = Json.parse("""
      {
        "name" : "Fiver",
        "friends" : [ {
          "name" : "Bigwig",
          "friends" : []
        }, {
          "name" : "Hazel",
          "friends" : []
        } ]
      }
      """)
      val userResult = json.validate[User]
      userResult must beLike {case x: JsSuccess[User] =>  x.get.name === "Fiver"}

      // Use Writes for model -> JSON
      val jsonFromUser = Json.toJson(userResult.get)
      (jsonFromUser \ "name").as[String] === "Fiver"
    }

    "allow creating Format from components" in {
      import SampleModel._

      import play.api.libs.json._
      import play.api.libs.json.Reads._
      import play.api.libs.functional.syntax._

      //#format-components
      val locationReads: Reads[Location] = (
        (JsPath \ "lat").read[Double](min(-90.0) keepAnd max(90.0)) and
        (JsPath \ "long").read[Double](min(-180.0) keepAnd max(180.0))
      )(Location.apply _)

      val locationWrites: Writes[Location] = (
        (JsPath \ "lat").write[Double] and
        (JsPath \ "long").write[Double]
      )(unlift(Location.unapply))

      implicit val locationFormat: Format[Location] =
        Format(locationReads, locationWrites)
      //#format-components

      // Use Reads for JSON -> model
      val json: JsValue = Json.parse("""
      {
        "lat" : 51.235685,
        "long" : -1.309197
      }
      """)
      val location = json.validate[Location].get
      location ===  Location(51.235685,-1.309197)

      // Use Writes for model -> JSON
      val jsonFromLocation = Json.toJson(location)
      (jsonFromLocation \ "lat").as[Double] === 51.235685
    }

    "allow creating Format from combinators" in {
      import SampleModel._

      import play.api.libs.json._
      import play.api.libs.json.Reads._
      import play.api.libs.functional.syntax._

      //#format-combinators
      implicit val locationFormat: Format[Location] = (
        (JsPath \ "lat").format[Double](min(-90.0) keepAnd max(90.0)) and
        (JsPath \ "long").format[Double](min(-180.0) keepAnd max(180.0))
      )(Location.apply, unlift(Location.unapply))
      //#format-combinators

      // Use Reads for JSON -> model
      val json: JsValue = Json.parse("""
      {
        "lat" : 51.235685,
        "long" : -1.309197
      }
      """)
      val location = json.validate[Location].get
      location ===  Location(51.235685,-1.309197)

      // Use Writes for model -> JSON
      val jsonFromLocation = Json.toJson(location)
      (jsonFromLocation \ "lat").as[Double] === 51.235685
    }

  }

}
