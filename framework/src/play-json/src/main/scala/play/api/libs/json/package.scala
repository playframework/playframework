/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs

/**
 * Json API
 * For example:
 * {{{
 * import play.api.libs.json._
 * import play.api.libs.functional.syntax._
 *
 * case class User(id: Long, name: String, friends: Seq[User] = Seq.empty)
 * object User {
 *
 *   // In this format, an undefined friends property is mapped to an empty list
 *   implicit val format: Format[User] = (
 *     (__ \ "id").format[Long] and
 *     (__ \ "name").format[String] and
 *     (__ \ "friends").lazyFormatNullable(implicitly[Format[Seq[User]]])
 *       .inmap[Seq[User]](_ getOrElse Seq.empty, Some(_))
 *   )(User.apply, unlift(User.unapply))
 * }
 *
 * //then in a controller:
 *
 * object MyController extends Controller {
 *    def displayUserAsJson(id: String) = Action {
 *       Ok(Json.toJson(User(id.toLong, "myName")))
 *    }
 *    def saveUser(jsonString: String)= Action {
 *      val user = Json.parse(jsonString).as[User]
 *      myDataStore.save(user)
 *      Ok
 *    }
 * }
 * }}}
 */
package object json {
  /**
   * Alias for `JsPath` companion object
   */
  val __ = JsPath
}
