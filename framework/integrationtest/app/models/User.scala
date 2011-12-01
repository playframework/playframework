package models

import sjson.json._
import dispatch.json._
import JsonSerialization._

case class User(id: Long, name: String, favThings: List[String])

object Protocol extends DefaultProtocol {
    implicit val UserFormat: Format[User] = asProduct3("id", "name", "favThings")(User)(User.unapply(_).get)
}

