/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.json

import scala.concurrent.Future

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import play.api.mvc._
import play.api.test._

@RunWith(classOf[JUnitRunner])
class ScalaJsonHttpSpec extends PlaySpecification with Results {
  
  "JSON with HTTP" should {
    "allow serving JSON" in {
      
      //#serve-json-imports
      //###insert: import play.api.mvc._
      import play.api.libs.json._
      import play.api.libs.functional.syntax._
      //#serve-json-imports
      
      //#serve-json-implicits
      implicit val locationWrites: Writes[Location] = (
        (JsPath \ "lat").write[Double] and
        (JsPath \ "long").write[Double]
      )(unlift(Location.unapply))
  
      implicit val placeWrites: Writes[Place] = (
        (JsPath \ "name").write[String] and
        (JsPath \ "location").write[Location]
      )(unlift(Place.unapply))
      //#serve-json-implicits
      
      //#serve-json
      def listPlaces = Action {
        val json = Json.toJson(Place.list)
        Ok(json)
      }
      //#serve-json
      
      val result: Future[Result] = listPlaces().apply(FakeRequest())
      status(result) === OK
      contentType(result) === Some("application/json")
      contentAsString(result) === """[{"name":"Sandleford","location":{"lat":51.377797,"long":-1.318965}},{"name":"Watership Down","location":{"lat":51.235685,"long":-1.309197}}]"""
    }
    
    "allow handling JSON" in {
      
      //#handle-json-imports
      import play.api.libs.json._
      import play.api.libs.functional.syntax._
      //#handle-json-imports
      
      //#handle-json-implicits
      implicit val locationReads: Reads[Location] = (
        (JsPath \ "lat").read[Double] and
        (JsPath \ "long").read[Double]
      )(Location.apply _)
      
      implicit val placeReads: Reads[Place] = (
        (JsPath \ "name").read[String] and
        (JsPath \ "location").read[Location]
      )(Place.apply _)
      //#handle-json-implicits
      
      //#handle-json
      def savePlace = Action { request =>
        request.body.asJson.map { json =>
          val placeResult = json.validate[Place] 
          placeResult.fold(
            errors => {
              BadRequest(Json.obj("status" ->"KO", "message" -> JsError.toFlatJson(errors)))
            },
            place => { 
              Place.save(place)
              Ok(Json.obj("status" ->"OK", "message" -> ("Place '"+place.name+"' saved.") ))  
            }
          )
        }.getOrElse {
          BadRequest(Json.obj("status" ->"KO", "message" -> "Expecting JSON data."))
        }
      }
      //#handle-json
      
      val body = Json.parse("""
      {
        "name" : "Nuthanger Farm",
        "location" : {
          "lat" : 51.244031,
          "long" : -1.263224
        }
      }
      """)
      val request = FakeRequest().withHeaders(CONTENT_TYPE -> "application/json").withJsonBody(body)
      val result: Future[Result] = savePlace().apply(request)
      
      status(result) === OK
      contentType(result) === Some("application/json")
      contentAsString(result) === """{"status":"OK","message":"Place 'Nuthanger Farm' saved."}"""
    }
    
    "allow handling JSON with BodyParser" in {
      
      import play.api.libs.json._
      import play.api.libs.functional.syntax._
      
      implicit val locationReads: Reads[Location] = (
        (JsPath \ "lat").read[Double] and
        (JsPath \ "long").read[Double]
      )(Location.apply _)
      
      implicit val placeReads: Reads[Place] = (
        (JsPath \ "name").read[String] and
        (JsPath \ "location").read[Location]
      )(Place.apply _)
      
      //#handle-json-bodyparser
      def savePlace = Action(BodyParsers.parse.json) { request =>
        val placeResult = request.body.validate[Place]
        placeResult.fold(
          errors => {
            BadRequest(Json.obj("status" ->"KO", "message" -> JsError.toFlatJson(errors)))
          },
          place => { 
            Place.save(place)
            Ok(Json.obj("status" ->"OK", "message" -> ("Place '"+place.name+"' saved.") ))  
          }
        )
      }
      //#handle-json-bodyparser
      
      val body: JsValue = Json.parse("""
      {
        "name" : "Nuthanger Farm",
        "location" : {
          "lat" : 51.244031,
          "long" : -1.263224
        }
      }
      """)
      val request = FakeRequest().withHeaders(CONTENT_TYPE -> "application/json").withBody(body)
      val result: Future[Result] = savePlace().apply(request)
      val bodyText: String = contentAsString(result)
      status(result) === OK
      contentType(result) === Some("application/json")
      contentAsString(result) === """{"status":"OK","message":"Place 'Nuthanger Farm' saved."}"""
    }
  }
  
}

//#model
case class Location(lat: Double, long: Double)

case class Place(name: String, location: Location)

object Place {
    
  var list: List[Place] = {
    List(
      Place(
        "Sandleford",
        Location(51.377797, -1.318965)
      ),
      Place(
        "Watership Down",
        Location(51.235685, -1.309197)
      )
    )
  }
    
  def save(place: Place) = {
    list = list ::: List(place)
  }
}
//#model

//#controller
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._

object Application extends Controller {
  
}
//#controller
