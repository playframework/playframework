/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.json

import javax.inject.Inject

import scala.concurrent.Future

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import play.api.mvc._
import play.api.test._

@RunWith(classOf[JUnitRunner])
class ScalaJsonHttpSpec extends PlaySpecification with Results {
  "JSON with HTTP" should {
    "allow serving JSON" in new WithApplication() with Injecting {
      val Action = inject[DefaultActionBuilder]

      // #serve-json-imports
      // ###insert: import play.api.mvc._
      import play.api.libs.functional.syntax._
      import play.api.libs.json._
      // #serve-json-imports

      // #serve-json-implicits
      implicit val locationWrites: Writes[Location] =
        (JsPath \ "lat").write[Double].and((JsPath \ "long").write[Double])(unlift(Location.unapply))

      implicit val placeWrites: Writes[Place] =
        (JsPath \ "name").write[String].and((JsPath \ "location").write[Location])(unlift(Place.unapply))
      // #serve-json-implicits

      // #serve-json
      def listPlaces() = Action {
        val json = Json.toJson(Place.list)
        Ok(json)
      }
      // #serve-json

      val result: Future[Result] = listPlaces().apply(FakeRequest())
      status(result) === OK
      contentType(result) === Some("application/json")
      contentAsString(
        result
      ) === """[{"name":"Sandleford","location":{"lat":51.377797,"long":-1.318965}},{"name":"Watership Down","location":{"lat":51.235685,"long":-1.309197}}]"""
    }

    "allow handling JSON" in new WithApplication() with Injecting {
      val Action = inject[DefaultActionBuilder]

      // #handle-json-imports
      import play.api.libs.functional.syntax._
      import play.api.libs.json._
      // #handle-json-imports

      // #handle-json-implicits
      implicit val locationReads: Reads[Location] =
        (JsPath \ "lat").read[Double].and((JsPath \ "long").read[Double])(Location.apply _)

      implicit val placeReads: Reads[Place] =
        (JsPath \ "name").read[String].and((JsPath \ "location").read[Location])(Place.apply _)
      // #handle-json-implicits

      // #handle-json
      def savePlace(): Action[AnyContent] = Action { request =>
        request.body.asJson
          .map { json =>
            val placeResult = json.validate[Place]
            placeResult.fold(
              errors => {
                BadRequest(Json.obj("message" -> JsError.toJson(errors)))
              },
              place => {
                Place.save(place)
                Ok(Json.obj("message" -> ("Place '" + place.name + "' saved.")))
              }
            )
          }
          .getOrElse {
            BadRequest(Json.obj("message" -> "Expecting JSON data."))
          }
      }
      // #handle-json

      val body = Json.parse("""
      {
        "name" : "Nuthanger Farm",
        "location" : {
          "lat" : 51.244031,
          "long" : -1.263224
        }
      }
      """)
      val request                = FakeRequest().withHeaders(CONTENT_TYPE -> "application/json").withJsonBody(body)
      val result: Future[Result] = savePlace().apply(request)

      status(result) === OK
      contentType(result) === Some("application/json")
      contentAsString(result) === """{"message":"Place 'Nuthanger Farm' saved."}"""
    }

    "allow handling JSON with BodyParser" in new WithApplication() with Injecting {
      import play.api.libs.functional.syntax._
      import play.api.libs.json._

      implicit val locationReads: Reads[Location] =
        (JsPath \ "lat").read[Double].and((JsPath \ "long").read[Double])(Location.apply _)

      implicit val placeReads: Reads[Place] =
        (JsPath \ "name").read[String].and((JsPath \ "location").read[Location])(Place.apply _)

      val parse  = inject[PlayBodyParsers]
      val Action = inject[DefaultActionBuilder]

      // #handle-json-bodyparser
      def savePlace(): Action[JsValue] = Action(parse.json) { request =>
        val placeResult = request.body.validate[Place]
        placeResult.fold(
          errors => {
            BadRequest(Json.obj("message" -> JsError.toJson(errors)))
          },
          place => {
            Place.save(place)
            Ok(Json.obj("message" -> ("Place '" + place.name + "' saved.")))
          }
        )
      }
      // #handle-json-bodyparser

      val body: JsValue = Json.parse("""
      {
        "name" : "Nuthanger Farm",
        "location" : {
          "lat" : 51.244031,
          "long" : -1.263224
        }
      }
      """)
      val request                = FakeRequest().withHeaders(CONTENT_TYPE -> "application/json").withBody(body)
      val result: Future[Result] = savePlace().apply(request)
      val bodyText: String       = contentAsString(result)
      status(result) === OK
      contentType(result) === Some("application/json")
      contentAsString(result) === """{"message":"Place 'Nuthanger Farm' saved."}"""
    }

    "allow concise handling JSON with BodyParser" in new WithApplication() with Injecting {
      import scala.concurrent.ExecutionContext.Implicits.global

      val parse  = inject[PlayBodyParsers]
      val Action = inject[DefaultActionBuilder]

      // #handle-json-bodyparser-concise
      import play.api.libs.functional.syntax._
      import play.api.libs.json._
      import play.api.libs.json.Reads._

      implicit val locationReads: Reads[Location] =
        (JsPath \ "lat")
          .read[Double](min(-90.0).keepAnd(max(90.0)))
          .and((JsPath \ "long").read[Double](min(-180.0).keepAnd(max(180.0))))(Location.apply _)

      implicit val placeReads: Reads[Place] =
        (JsPath \ "name").read[String](minLength[String](2)).and((JsPath \ "location").read[Location])(Place.apply _)

      // This helper parses and validates JSON using the implicit `placeReads`
      // above, returning errors if the parsed json fails validation.
      def validateJson[A: Reads] = parse.json.validate(
        _.validate[A].asEither.left.map(e => BadRequest(JsError.toJson(e)))
      )

      // if we don't care about validation we could replace `validateJson[Place]`
      // with `BodyParsers.parse.json[Place]` to get an unvalidated case class
      // in `request.body` instead.
      def savePlaceConcise: Action[Place] = Action(validateJson[Place]) { request =>
        // `request.body` contains a fully validated `Place` instance.
        val place = request.body
        Place.save(place)
        Ok(Json.obj("message" -> ("Place '" + place.name + "' saved.")))
      }
      // #handle-json-bodyparser-concise

      val body: JsValue = Json.parse("""
      {
        "name" : "Nuthanger Farm",
        "location" : {
          "lat" : 51.244031,
          "long" : -1.263224
        }
      }
      """)
      val request =
        FakeRequest().withHeaders(CONTENT_TYPE -> "application/json").withBody(Json.fromJson[Place](body).get)
      val result: Future[Result] = savePlaceConcise.apply(request)
      val bodyText: String       = contentAsString(result)
      status(result) === OK
      contentType(result) === Some("application/json")
      contentAsString(result) === """{"message":"Place 'Nuthanger Farm' saved."}"""
    }
  }
}

//#model
case class Location(lat: Double, long: Double)
object Location {
  def unapply(l: Location): Option[(Double, Double)] = Some(l.lat, l.long)
}

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

  def save(place: Place): Unit = {
    list = list ::: List(place)
  }

  def unapply(p: Place): Option[(String, Location)] = Some(p.name, p.location)
}
//#model

//#controller
import play.api.mvc._

class HomeController @Inject() (cc: ControllerComponents) extends AbstractController(cc) {}
//#controller
