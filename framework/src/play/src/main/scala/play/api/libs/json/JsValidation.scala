package play.api.libs.json

import play.api.data.validation.ValidationError
import JsResultHelpers._

object Constraint {
  def of[T](implicit r: Reads[T]): Reads[T] = r
}

object JsMapper {
  def apply[T, A1](jsc1: (JsPath, Reads[A1]))
           (apply: Function1[A1, T])(unapply: Function1[T, Option[A1]])
           (implicit w1: Writes[A1]) = {
    new Format[T] {
      def reads(json: JsValue): JsResult[T] = jsc1._1.asSingleJsResult(json).flatMap(jsc1._2.reads(_).repath(jsc1._1)).map(apply(_)).rebase(json)
      
      def writes(t: T): JsValue = {
        unapply(t) match {
          case Some(a1) => jsc1._1.set(Json.obj(), Json.toJson(a1))
          case _ => JsUndefined("couldn't find the right type when calling unapply")
        }
      }
    }
  }

  def apply[T, A1, A2](jsc1: (JsPath, Reads[A1]),
              jsc2: (JsPath, Reads[A2]))
           (apply: (A1, A2) => T)(unapply: T => Option[Product2[A1, A2]])
           (implicit w1: Writes[A1], w2: Writes[A2]) = {
    new Format[T] {
      def reads(json: JsValue): JsResult[T] = product(
        jsc1._1.asSingleJsResult(json), 
        jsc2._1.asSingleJsResult(json)
      ).flatMap{ case (js1, js2) => product(
        jsc1._2.reads(js1).repath(jsc1._1), 
        jsc2._2.reads(js2).repath(jsc2._1)
      )}.map{ case(a1, a2) => apply(a1, a2) }.rebase(json)

      def writes(t: T): JsValue = {
        unapply(t) match {
          case Some((a1, a2)) => jsc2._1.set(jsc1._1.set(Json.obj(), Json.toJson(a1)), Json.toJson(a2))
          case _ => JsUndefined("couldn't find the right type when calling unapply")
        }
      }
    }
  }

  def apply[T, A1, A2, A3](jsc1: (JsPath, Reads[A1]),
              jsc2: (JsPath, Reads[A2]),
              jsc3: (JsPath, Reads[A3]))
           (apply: (A1, A2, A3) => T)(unapply: T => Option[Product3[A1, A2, A3]])
           (implicit w1: Writes[A1], w2: Writes[A2], w3: Writes[A3]) = {
    new Format[T] {
      def reads(json: JsValue): JsResult[T] = product(
        jsc1._1.asSingleJsResult(json), 
        jsc2._1.asSingleJsResult(json), 
        jsc3._1.asSingleJsResult(json)
      ).flatMap{ case (js1, js2, js3) => 
       product(
        jsc1._2.reads(js1).repath(jsc1._1), 
        jsc2._2.reads(js2).repath(jsc2._1), 
        jsc3._2.reads(js3).repath(jsc3._1)
      ) }.map{ case(a1, a2, a3) => apply(a1, a2, a3) }.rebase(json)

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