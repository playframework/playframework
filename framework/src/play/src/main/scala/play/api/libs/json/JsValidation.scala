package play.api.libs.json

case class Constraint[T](name: Option[String], args: Seq[Any])(f: (JsValue => JsResult[T])) {

  /**
   * Run the constraint validation.
   *
   * @param t the value to validate
   * @return the validation result
   */
  def apply(json: JsValue): JsResult[T] = f(json)
}

object Constraint {
  def apply[T](name: String, args: Any*)(f: (JsValue => JsResult[T])): Constraint[T] = Constraint(Some(name), args.toSeq)(f)
}

object JsValidator {
  def apply[T, A1](constraint1: (JsPath, Constraint[A1]))
           (apply: Function1[A1, T])(unapply: Function1[T, Option[A1]])
           (implicit w1: Writes[A1]) = {
    new Format[T] {
      def reads(json: JsValue): JsResult[T] = constraint1 match {
        case (path, constr) => path(json) match {
          case Nil => JsError(json, json, Some(Json.arr(JsErrorObj(json, "validation.error.missing-path", JsString(constraint1._1.toString)))))
          case List(js) => constr(js).fold( 
            valid = a1 => JsSuccess(apply(a1)), 
            invalid = (o, e, g) => JsError(o, path.set(json, e), g)
          )
        }
      }  

      def writes(t: T): JsValue = {
        unapply(t) match {
          case Some(a1) => constraint1._1.set(Json.obj(), Json.toJson(a1))
          case _ => JsUndefined("couldn't find the right type when calling unapply")
        }
      }
    }
  }
}