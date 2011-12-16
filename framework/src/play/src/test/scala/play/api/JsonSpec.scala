package play.api.json

import org.specs2.mutable._
import play.api.json._

object JsonSpec extends Specification {

  case class User(id: Long, name: String, friends: List[User])

  implicit def userListReader(json: JsValue): List[User] = listReader(json)
  implicit def reads(json: JsValue): User = User(
    json \ "id",
    json \ "name",
    (json \ "friends").asOpt[List[User]].getOrElse(List()))

  implicit def writes(u: User): JsObject = Map[String, JsValue](
    "id" -> u.id,
    "name" -> u.name,
    "friends" -> u.friends.map(fr => Map[String, JsValue]("id" -> fr.id, "name" -> fr.name)))

  "JSON" should {

    "serialize and deserialize" in {
      val luigi = User(1, "Luigi", List())
      val kinopio = User(2, "Kinopio", List())
      val yoshi = User(3, "Yoshi", List())
      val mario = User(0, "Mario", List(luigi, kinopio, yoshi))
      val jsonMario: JsValue = mario
      jsonMario.as[User] must equalTo(mario)
      (jsonMario \\ "name") must equalTo(Seq(JsString("Mario"), JsString("Luigi"), JsString("Kinopio"), JsString("Yoshi")))
    }

  }
}
