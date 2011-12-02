package models

import play.api.Json._
import com.codahale.jerkson.AST._

case class User(id: Long, name: String, favThings: List[String])

object Protocol {

    implicit object UserFormat extends Format[User] {

        def writes(o: User): JValue = JObject(
            List(JField("id", JInt(o.id)),
                 JField("name", JString(o.name)),
                 JField("favThings", JArray(o.favThings.map(JString(_))))
             ))

        def reads(json: JValue): User = User(
            fromjson[Long](json \ "id"),
            fromjson[String](json \ "name"),
            fromjson[List[String]](json \ "favThings")
        )

    }

}

