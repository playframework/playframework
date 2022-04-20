/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.binder.models

import scala.Left
import scala.Right
import play.api.mvc.PathBindable
import play.api.Logging

//#declaration
case class User(id: Int, name: String) {}
//#declaration
object User extends Logging {
  // stubbed test
  // designed to be lightweight operation
  def findById(id: Int): Option[User] = {
    logger.info("findById: " + id.toString)
    if (id > 3) None
    var user = new User(id, "User " + String.valueOf(id))
    Some(user)
  }

  //#bindPath
  implicit def pathBinder(implicit intBinder: PathBindable[Int]) = new PathBindable[User] {
    override def bindPath(key: String, value: String): Either[String, User] = {
      for {
        id   <- intBinder.bindPath(key, value).right
        user <- User.findById(id).toRight("User not found").right
      } yield user
    }
    override def unbindPath(key: String, user: User): String = {
      user.id.toString
    }
  }
  //#bindPath
}
