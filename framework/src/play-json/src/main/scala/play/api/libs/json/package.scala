package play.api.libs

/**
 * Json API
 * For example:
 * {{{
 *  case class User(id: Long, name: String, friends: List[User])
 *
 *  implicit object UserFormat extends Format[User] {
 *   def reads(json: JsValue): User = User(
 *     (json \ "id").as[Long],
 *     (json \ "name").as[String],
 *     (json \ "friends").asOpt[List[User]].getOrElse(List()))
 *   def writes(u: User): JsValue = JsObject(List(
 *     "id" -> JsNumber(u.id),
 *     "name" -> JsString(u.name),
 *     "friends" -> JsArray(u.friends.map(fr => JsObject(List("id" -> JsNumber(fr.id),
 *     "name" -> JsString(fr.name)))))))
 * }
 *
 * //then in a controller:
 * object MyController extends Controller {
 *    def displayUserAsJson(id: String) = Action {
 *       Ok(toJson( User(id.toLong, "myName", friends: List())))
 *    }
 *    def saveUser(jsonString: String)= Action {
 *      val user = play.api.libs.json.parse(jsonString).as[User]
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

  import play.api.data.validation.{ Path, Validation, ValidationError }
  // TODO: replace that by
  // type JsResult[A] = VA[JsValue, O]
  type JsResult[O] = Validation[(JsPath, Seq[ValidationError]), O]

  // Backward compat
  trait WithRepath[A] {
    val self: Validation[(JsPath, Seq[ValidationError]), A]

    def repath(path: JsPath): JsResult[A] = self match {
      case JsSuccess(a, p) => JsSuccess(a, path ++ p)
      case JsError(es) => JsError(es.map { case (p, s) => path ++ p -> s })
    }
  }
  import scala.language.implicitConversions
  @scala.deprecated("JsResult.repath will be deleted", "2.3.0")
  implicit def jsValidationToWithRepath[A](v: Validation[(JsPath, Seq[ValidationError]), A]): WithRepath[A] = new WithRepath[A] {
    val self = v
  }

  import play.api.libs.functional._

  implicit def alternativeJsResult(implicit a: Applicative[JsResult]): Alternative[JsResult] = new Alternative[JsResult] {
    val app = a
    def |[A, B >: A](alt1: JsResult[A], alt2: JsResult[B]): JsResult[B] = (alt1, alt2) match {
      case (JsError(e), JsSuccess(t, p)) => JsSuccess(t, p)
      case (JsSuccess(t, p), _) => JsSuccess(t, p)
      case (JsError(e1), JsError(e2)) => JsError(JsError.merge(e1, e2))
    }
    def empty: JsResult[Nothing] = JsError(Seq())
  }
}
