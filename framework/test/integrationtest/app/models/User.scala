package models

import play.api.libs.json._

case class User(id: Long, name: String, favThings: List[String])

object Protocol {

    implicit object UserFormat extends Format[User] {

        def writes(o: User): JsValue = JsObject(
            List("id" -> JsNumber(o.id),
                "name" -> JsString(o.name),
                "favThings" -> JsArray(o.favThings.map(JsString(_)))
            )
        )

        def reads(json: JsValue): JsResult[User] = JsSuccess(User(
            (json \ "id").as[Long],
            (json \ "name").as[String],
            (json \ "favThings").as[List[String]]
        ))

    }

}

