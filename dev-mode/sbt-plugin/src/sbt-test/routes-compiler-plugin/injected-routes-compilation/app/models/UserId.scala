package models

import java.net.URLEncoder

import play.api.mvc.{ PathBindable, QueryStringBindable }

object UserId {
  implicit object pathBindable extends PathBindable.Parsing[UserId](
    UserId.apply,
    _.id,
    (key: String, e: Exception) => "Cannot parse parameter %s as UserId: %s".format(key, e.getMessage)
  )
  implicit object queryStringBindable extends QueryStringBindable.Parsing[UserId](
    UserId.apply,
    userId => URLEncoder.encode(userId.id, "utf-8"),
    (key: String, e: Exception) => "Cannot parse parameter %s as UserId: %s".format(key, e.getMessage)
  )
}

case class UserId(id: String) extends AnyVal
