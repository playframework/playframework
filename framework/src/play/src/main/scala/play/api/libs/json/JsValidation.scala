package play.api.libs.json

import play.api.data.validation.ValidationError
import JsResultHelpers._

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

  def of[T](implicit r: Reads[T]): Constraint[T] = Constraint(None, Seq())(js => r.reads(js))

  //def list[T]: Constraint[List[T]] = Constraint(None, Seq())( json => JsError[List[T]](json) )
}

object JsMapper {
  def apply[T, A1](jsc1: (JsPath, Constraint[A1]))
           (apply: Function1[A1, T])(unapply: Function1[T, Option[A1]])
           (implicit w1: Writes[A1]) = {
    new Format[T] {
      def reads(json: JsValue): JsResult[T] = jsc1._1.asSingleJsResult(json).flatMap(jsc1._2(_).repath(jsc1._1)).map(apply(_)).rebase(json)
      
      def writes(t: T): JsValue = {
        unapply(t) match {
          case Some(a1) => jsc1._1.set(Json.obj(), Json.toJson(a1))
          case _ => JsUndefined("couldn't find the right type when calling unapply")
        }
      }
    }
  }

  def apply[T, A1, A2](jsc1: (JsPath, Constraint[A1]),
              jsc2: (JsPath, Constraint[A2]))
           (apply: (A1, A2) => T)(unapply: T => Option[Product2[A1, A2]])
           (implicit w1: Writes[A1], w2: Writes[A2]) = {
    new Format[T] {
      def reads(json: JsValue): JsResult[T] = product(
        jsc1._1.asSingleJsResult(json), 
        jsc2._1.asSingleJsResult(json)
      ).flatMap{ case (js1, js2) => product(
        jsc1._2(js1), 
        jsc2._2(js2)
      )}.fold(
        valid = { case (a1, a2) => JsSuccess(apply(a1, a2)) },
        invalid = (o, e, g) => JsError(json, e, g)
      )

      def writes(t: T): JsValue = {
        unapply(t) match {
          case Some((a1, a2)) => jsc2._1.set(jsc1._1.set(Json.obj(), Json.toJson(a1)), Json.toJson(a2))
          case _ => JsUndefined("couldn't find the right type when calling unapply")
        }
      }
    }
  }

  def apply[T, A1, A2, A3](jsc1: (JsPath, Constraint[A1]),
              jsc2: (JsPath, Constraint[A2]),
              jsc3: (JsPath, Constraint[A3]))
           (apply: (A1, A2, A3) => T)(unapply: T => Option[Product3[A1, A2, A3]])
           (implicit w1: Writes[A1], w2: Writes[A2], w3: Writes[A3]) = {
    new Format[T] {
      def reads(json: JsValue): JsResult[T] = product(
        jsc1._1.asSingleJsResult(json), 
        jsc2._1.asSingleJsResult(json), 
        jsc3._1.asSingleJsResult(json)
      ).flatMap{ case (js1, js2, js3) => 
       product(
        jsc1._2(js1), 
        jsc2._2(js2), 
        jsc3._2(js3)
      ) }.fold(
        valid = { case(a1, a2, a3) => JsSuccess(apply(a1, a2, a3)) },
        invalid = (o, e, g) => JsError(json, e, g)
      )

      def writes(t: T): JsValue = {
        unapply(t) match {
          case Some((a1, a2, a3)) => 
            jsc3._1.set(
              jsc2._1.set(
                jsc1._1.set(Json.obj(), Json.toJson(a1)), 
                Json.toJson(a2)
              ),
              Json.toJson(a3)
            )
          case _ => JsUndefined("couldn't find the right type when calling unapply")
        }
      }
    }
  }
}