/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.binder.models

import scala.Left
import scala.Right
import play.api.mvc.PathBindable
import play.Logger

//#declaration
case class User(id: Int, name: String) {}
//#declaration
object User {

  // stubbed test
  // designed to be lightweight operation
  def findById(id: Int): Option[User] = {
    Logger.info("findById: " + id.toString)
    if (id > 3) None
    var user = new User(id, "User " + String.valueOf(id))
    Some(user)
  }

  //#bind
  implicit def pathBinder(implicit intBinder: PathBindable[Int]) = new PathBindable[User] {
    override def bind(key: String, value: String): Either[String, User] = {
      for {
        id   <- intBinder.bind(key, value).right
        user <- User.findById(id).toRight("User not found").right
      } yield user
    }
    override def unbind(key: String, user: User): String = {
      user.id.toString
    }
  }
  //#bind
}
