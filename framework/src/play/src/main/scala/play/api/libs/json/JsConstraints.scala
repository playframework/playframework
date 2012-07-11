package play.api.libs.json

import play.api.data.validation.ValidationError
import Json._

case class JsFlow[T](in: Option[Reads[T]] = None, out: Option[Writes[T]] = None)
      (implicit fmt: Format[T]) extends Format[T] {
  def reads(json: JsValue): JsResult[T] = fromJson(json)(in.getOrElse(fmt))    
  def writes(t: T): JsValue = toJson(t)(out.getOrElse(fmt))

  def ~(other: JsFlow[T])  = (in, out) match {
    case (Some(in), None) => JsFlow(Some(in), other.out)
    case (None, Some(out)) => JsFlow(other.in, Some(out))
    case (None, None) => JsFlow(other.in, other.out)
    case (Some(in), Some(out)) => JsFlow(other.in, other.out)
  }
}

object Constraints extends JsFlowHelpers with ConstraintReads with ConstraintWrites with JsResultProducts {
    val defaultJs = JsUndefined("default")
}

object JsResultProducts extends JsResultProducts

trait JsFlowHelpers {
  def in[T](implicit fmt: Format[T]): JsFlow[T] = JsFlow(Some(fmt), None)
  def in[T](r: Reads[T])(implicit fmt: Format[T]): JsFlow[T] = JsFlow(Some(r), None)

  def out[T](w: Writes[T])(implicit fmt: Format[T]): JsFlow[T] = JsFlow(None, Some(w))  
}

trait ConstraintReads {
  def of[T](implicit fmt: Format[T]): Format[T] = new Format[T] {
    def reads(json: JsValue): JsResult[T] = fmt.reads(json)
    def writes(t: T): JsValue = fmt.writes(t)
  }

  def required[T](implicit r: Reads[T]): Reads[T] = new Reads[T] {
    def reads(json: JsValue): JsResult[T] = json match {
      case js @ JsUndefined(_) => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.required")))) and fromJson(js)
      case js => fromJson(js)
    }
  }

  def optional[T](implicit r: Reads[Option[T]]): Reads[Option[T]] = new Reads[Option[T]] {
    def reads(json: JsValue): JsResult[Option[T]] = json match {
      case JsUndefined(_) => JsSuccess(None)
      case js => fromJson(js)
    }
  }

  def min(nb: Int): Reads[Int] = new Reads[Int] {
    def reads(json: JsValue): JsResult[Int] = json match {
      case JsNumber(d) => if (d >= nb) JsSuccess(d.toInt) else JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.min", nb))))
      case js => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.jsnumber"))))
    }
  }

  def valueEquals[T](value: T)(implicit r: Reads[T]): Reads[T] = new Reads[T] {
    def reads(json: JsValue): JsResult[T] = fromJson(json)(r).flatMap( t => 
      if(t.equals(value)) JsSuccess(t) 
      else JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.equals", value))))
    )
  }

  def max(nb: Int): Reads[Int] = new Reads[Int] {
    def reads(json: JsValue): JsResult[Int] = json match {
      case JsNumber(d) => if (d <= nb) JsSuccess(d.toInt) else JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.max", nb))))
      case js => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.jsnumber"))))
    }
  }

  def minLength(length: Int): Reads[String] = new Reads[String] {
    def reads(json: JsValue): JsResult[String] = json match {
      case js @ JsString(s) => if (s.size >= length) JsSuccess(s) else JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.minLength", length))))
      case js => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.jsstring"))))
    }
  }

  def maxLength(length: Int): Reads[String] = new Reads[String] {
    def reads(json: JsValue): JsResult[String] = json match {
      case js @ JsString(s) => if (s.size <= length) JsSuccess(s) else JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.maxLength", length))))
      case js => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.jsstring"))))
    }
  }
  
  def email : Reads[String] = new Reads[String] {
    def reads(json: JsValue): JsResult[String] = json match {      
      case js @ JsString(s) => 
        val regex = """\b[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,4}\b""".r
        regex.findFirstIn(s).map(JsSuccess(_)).getOrElse(JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.email")))))
      case js => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.jsstring"))))
    }
  }
}

trait ConstraintWrites {
  def pruned[T](implicit w: Writes[T]): Writes[T] = new Writes[T] {
    def writes(t: T): JsValue = JsUndefined("pruned")
  }
}


object JsTupler {
  import Constraints.defaultJs
  import JsResultProducts._

  def apply[A1 : Writes](jsc1: (JsPath, Format[A1])): Format[A1] = new Format[A1] {
    def reads(json: JsValue): JsResult[A1] = {
      jsc1._1.asSingleJsResult(json).flatMapTryDefault(defaultJs){js => jsc1._2.reads(js).repath(jsc1._1)}
    }

    def writes(a1: A1): JsValue = jsc1._1.setIfDef(Json.obj(), toJson(a1))
  }

  def apply[A1 : Writes, A2 : Writes](
        jsc1: (JsPath, Format[A1]),
        jsc2: (JsPath, Format[A2])) = new Format[(A1, A2)] {
    def reads(json: JsValue): JsResult[(A1, A2)] = product(
      jsc1._1.asSingleJsResult(json).flatMapTryDefault(defaultJs){js => jsc1._2.reads(js).repath(jsc1._1)}, 
      jsc2._1.asSingleJsResult(json).flatMapTryDefault(defaultJs){js => jsc2._2.reads(js).repath(jsc2._1)}
    )

    def writes(t:(A1, A2)): JsValue = {
      jsc2._1.setIfDef(
        jsc1._1.setIfDef(Json.obj(), toJson(t._1)(jsc1._2)), 
        toJson(t._2)(jsc2._2)
      )
    }
  }

  def apply[A1 : Writes, A2 : Writes, A3 : Writes](
        jsc1: (JsPath, Format[A1]),
        jsc2: (JsPath, Format[A2]),
        jsc3: (JsPath, Format[A3])) = new Format[(A1, A2, A3)] {
    def reads(json: JsValue): JsResult[(A1, A2, A3)] = product(
      jsc1._1.asSingleJsResult(json).flatMapTryDefault(defaultJs){js => jsc1._2.reads(js).repath(jsc1._1)}, 
      jsc2._1.asSingleJsResult(json).flatMapTryDefault(defaultJs){js => jsc2._2.reads(js).repath(jsc2._1)}, 
      jsc3._1.asSingleJsResult(json).flatMapTryDefault(defaultJs){js => jsc3._2.reads(js).repath(jsc3._1)}
    )

    def writes(t:(A1, A2, A3)): JsValue = {
      jsc3._1.setIfDef(
        jsc2._1.setIfDef(
          jsc1._1.setIfDef(Json.obj(), toJson(t._1)(jsc1._2)), 
          toJson(t._2)(jsc2._2)
        ),
        toJson(t._3)(jsc3._2)
      )
    }
  }

  def apply[A1 : Writes, A2 : Writes, A3 : Writes, A4: Writes](
        jsc1: (JsPath, Format[A1]),
        jsc2: (JsPath, Format[A2]),
        jsc3: (JsPath, Format[A3]),
        jsc4: (JsPath, Format[A4])) = new Format[(A1, A2, A3, A4)] {
    def reads(json: JsValue): JsResult[(A1, A2, A3, A4)] = product(
      jsc1._1.asSingleJsResult(json).flatMapTryDefault(defaultJs){js => jsc1._2.reads(js).repath(jsc1._1)}, 
      jsc2._1.asSingleJsResult(json).flatMapTryDefault(defaultJs){js => jsc2._2.reads(js).repath(jsc2._1)}, 
      jsc3._1.asSingleJsResult(json).flatMapTryDefault(defaultJs){js => jsc3._2.reads(js).repath(jsc3._1)}, 
      jsc4._1.asSingleJsResult(json).flatMapTryDefault(defaultJs){js => jsc4._2.reads(js).repath(jsc4._1)}
    )

    def writes(t:(A1, A2, A3, A4)): JsValue = {
      jsc4._1.setIfDef(
        jsc3._1.setIfDef(
          jsc2._1.setIfDef(
            jsc1._1.setIfDef(Json.obj(), toJson(t._1)(jsc1._2)), 
            toJson(t._2)(jsc2._2)
          ),
          toJson(t._3)(jsc3._2)
        ),
        toJson(t._4)(jsc4._2)
      )
    }
  }

}

object JsMapper {
  import Constraints.defaultJs
  import JsResultProducts._

  def apply[T, A1](jsc1: (JsPath, Format[A1]))
           (apply: Function1[A1, T])(unapply: Function1[T, Option[A1]]) = {
    new Format[T] {
      def reads(json: JsValue): JsResult[T] = {
        jsc1._1.asSingleJsResult(json).flatMapTryDefault(defaultJs){js => jsc1._2.reads(js).repath(jsc1._1)}
            .map{ apply(_) }
      }
      
      def writes(t: T): JsValue = {
        unapply(t) match {
          case Some(a1) => jsc1._1.setIfDef(Json.obj(), Json.toJson(a1)(jsc1._2))
          case _ => JsUndefined("couldn't find the right type when calling unapply")
        }
      }
    }
  }

  def apply[T, A1 : Writes, A2 : Writes](jsc1: (JsPath, Format[A1]),
              jsc2: (JsPath, Format[A2]))
           (apply: (A1, A2) => T)(unapply: T => Option[Product2[A1, A2]]) = {
    new Format[T] {
      def reads(json: JsValue): JsResult[T] = product(
        jsc1._1.asSingleJsResult(json).flatMapTryDefault(defaultJs){js => jsc1._2.reads(js).repath(jsc1._1)}, 
        jsc2._1.asSingleJsResult(json).flatMapTryDefault(defaultJs){js => jsc2._2.reads(js).repath(jsc2._1)}
      ).map{ case (a1, a2) => apply(a1, a2) }

      def writes(t: T): JsValue = {
        unapply(t) match {
          case Some((a1, a2)) => 
            jsc2._1.setIfDef(
              jsc1._1.setIfDef(Json.obj(), toJson(a1)(jsc1._2)), 
              toJson(a2)(jsc2._2)
            )
          case _ => JsUndefined("couldn't find the right type when calling unapply")
        }
      }
    }
  }

  def apply[T, A1 : Writes, A2 : Writes, A3 : Writes](jsc1: (JsPath, Format[A1]),
            jsc2: (JsPath, Format[A2]),
            jsc3: (JsPath, Format[A3]))
           (apply: (A1, A2, A3) => T)(unapply: T => Option[Product3[A1, A2, A3]]) = {
    new Format[T] {
      def reads(json: JsValue): JsResult[T] = product(
        jsc1._1.asSingleJsResult(json).flatMapTryDefault(defaultJs){js => jsc1._2.reads(js).repath(jsc1._1)}, 
        jsc2._1.asSingleJsResult(json).flatMapTryDefault(defaultJs){js => jsc2._2.reads(js).repath(jsc2._1)}, 
        jsc3._1.asSingleJsResult(json).flatMapTryDefault(defaultJs){js => jsc3._2.reads(js).repath(jsc3._1)}
      ).map{ case(a1, a2, a3) => apply(a1, a2, a3) }

      def writes(t: T): JsValue = {
        unapply(t) match {
          case Some((a1, a2, a3)) => 
            jsc3._1.setIfDef(
              jsc2._1.setIfDef(
                jsc1._1.setIfDef(Json.obj(), toJson(a1)(jsc1._2)), 
                toJson(a2)(jsc2._2)
              ),
              toJson(a3)(jsc3._2)
            )
          case _ => JsUndefined("couldn't find the right type when calling unapply")
        }
      }
    }
  }
}

trait JsResultProducts {
  def product[T1, T2](
    t1: JsResult[T1], 
    t2: JsResult[T2]
  ): JsResult[(T1, T2)] = t1 prod t2

  def product[T1, T2, T3](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3]
  ): JsResult[(T1, T2, T3)] = {
    (t1 prod t2 prod t3) match {
      case JsSuccess(((t1, t2), t3)) => JsSuccess(t1, t2, t3)
      case JsError(e) => JsError(e)
    }
  }

  def product[T1, T2, T3, T4](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3], 
    t4: JsResult[T4]
  ): JsResult[(T1, T2, T3, T4)] = {
    (t1 prod t2 prod t3 prod t4) match {
      case JsSuccess((((t1, t2), t3), t4)) => JsSuccess(t1, t2, t3, t4)
      case JsError(e) => JsError(e)
    }
  }

  def product[T1, T2, T3, T4, T5](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3], 
    t4: JsResult[T4], 
    t5: JsResult[T5]
  ): JsResult[(T1, T2, T3, T4, T5)] = {
    (t1 prod t2 prod t3 prod t4 prod t5) match {
      case JsSuccess(((((t1, t2), t3), t4), t5)) => JsSuccess(t1, t2, t3, t4, t5)
      case JsError(e) => JsError(e)
    }
  }

  def product[T1, T2, T3, T4, T5, T6](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3], 
    t4: JsResult[T4], 
    t5: JsResult[T5], 
    t6: JsResult[T6]
  ): JsResult[(T1, T2, T3, T4, T5, T6)] = {
    (t1 prod t2 prod t3 prod t4 prod t5 prod t6) match {
      case JsSuccess((((((t1, t2), t3), t4), t5), t6)) => JsSuccess(t1, t2, t3, t4, t5, t6)
      case JsError(e) => JsError(e)
    }
  }

  def product[T1, T2, T3, T4, T5, T6, T7](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3], 
    t4: JsResult[T4], 
    t5: JsResult[T5], 
    t6: JsResult[T6], 
    t7: JsResult[T7]
  ): JsResult[(T1, T2, T3, T4, T5, T6, T7)] = {
    (t1 prod t2 prod t3 prod t4 prod t5 prod t6 prod t7) match {
      case JsSuccess(((((((t1, t2), t3), t4), t5), t6), t7)) => JsSuccess(t1, t2, t3, t4, t5, t6, t7)
      case JsError(e) => JsError(e)
    }
  }

  def product[T1, T2, T3, T4, T5, T6, T7, T8](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3], 
    t4: JsResult[T4], 
    t5: JsResult[T5], 
    t6: JsResult[T6], 
    t7: JsResult[T7], 
    t8: JsResult[T8]
  ): JsResult[(T1, T2, T3, T4, T5, T6, T7, T8)] = {
    (t1 prod t2 prod t3 prod t4 prod t5 prod t6 prod t7 prod t8) match {
      case JsSuccess((((((((t1, t2), t3), t4), t5), t6), t7), t8)) => JsSuccess(t1, t2, t3, t4, t5, t6, t7, t8)
      case JsError(e) => JsError(e)
    }
  }

  def product[T1, T2, T3, T4, T5, T6, T7, T8, T9](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3], 
    t4: JsResult[T4], 
    t5: JsResult[T5], 
    t6: JsResult[T6], 
    t7: JsResult[T7], 
    t8: JsResult[T8], 
    t9: JsResult[T9]
  ): JsResult[(T1, T2, T3, T4, T5, T6, T7, T8, T9)] = {
    (t1 prod t2 prod t3 prod t4 prod t5 prod t6 prod t7 prod t8 prod t9) match {
      case JsSuccess(((((((((t1, t2), t3), t4), t5), t6), t7), t8), t9)) => JsSuccess(t1, t2, t3, t4, t5, t6, t7, t8, t9)
      case JsError(e) => JsError(e)
    }
  }

  def product[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3], 
    t4: JsResult[T4], 
    t5: JsResult[T5], 
    t6: JsResult[T6], 
    t7: JsResult[T7], 
    t8: JsResult[T8], 
    t9: JsResult[T9], 
    t10: JsResult[T10]
  ): JsResult[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10)] = {
    (t1 prod t2 prod t3 prod t4 prod t5 prod t6 prod t7 prod t8 prod t9 prod t10) match {
      case JsSuccess((((((((((t1, t2), t3), t4), t5), t6), t7), t8), t9), t10)) => JsSuccess(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10)
      case JsError(e) => JsError(e)
    }
  }

  def product[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3], 
    t4: JsResult[T4], 
    t5: JsResult[T5], 
    t6: JsResult[T6], 
    t7: JsResult[T7], 
    t8: JsResult[T8], 
    t9: JsResult[T9], 
    t10: JsResult[T10], 
    t11: JsResult[T11]
  ): JsResult[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11)] = {
    (t1 prod t2 prod t3 prod t4 prod t5 prod t6 prod t7 prod t8 prod t9 prod t10 prod t11) match {
      case JsSuccess(((((((((((t1, t2), t3), t4), t5), t6), t7), t8), t9), t10), t11)) => JsSuccess(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11)
      case JsError(e) => JsError(e)
    }
  }

  def product[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3], 
    t4: JsResult[T4], 
    t5: JsResult[T5], 
    t6: JsResult[T6], 
    t7: JsResult[T7], 
    t8: JsResult[T8], 
    t9: JsResult[T9], 
    t10: JsResult[T10], 
    t11: JsResult[T11], 
    t12: JsResult[T12]
  ): JsResult[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12)] = {
    (t1 prod t2 prod t3 prod t4 prod t5 prod t6 prod t7 prod t8 prod t9 prod t10 prod t11 prod t12) match {
      case JsSuccess((((((((((((t1, t2), t3), t4), t5), t6), t7), t8), t9), t10), t11), t12)) => JsSuccess(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12)
      case JsError(e) => JsError(e)
    }
  }

  def product[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3], 
    t4: JsResult[T4], 
    t5: JsResult[T5], 
    t6: JsResult[T6], 
    t7: JsResult[T7], 
    t8: JsResult[T8], 
    t9: JsResult[T9], 
    t10: JsResult[T10], 
    t11: JsResult[T11], 
    t12: JsResult[T12], 
    t13: JsResult[T13]
  ): JsResult[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13)] = {
    (t1 prod t2 prod t3 prod t4 prod t5 prod t6 prod t7 prod t8 prod t9 prod t10 prod t11 prod t12 prod t13) match {
      case JsSuccess(((((((((((((t1, t2), t3), t4), t5), t6), t7), t8), t9), t10), t11), t12), t13)) => JsSuccess(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13)
      case JsError(e) => JsError(e)
    }
  }

  def product[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3], 
    t4: JsResult[T4], 
    t5: JsResult[T5], 
    t6: JsResult[T6], 
    t7: JsResult[T7], 
    t8: JsResult[T8], 
    t9: JsResult[T9], 
    t10: JsResult[T10], 
    t11: JsResult[T11], 
    t12: JsResult[T12], 
    t13: JsResult[T13], 
    t14: JsResult[T14]
  ): JsResult[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14)] = {
    (t1 prod t2 prod t3 prod t4 prod t5 prod t6 prod t7 prod t8 prod t9 prod t10 prod t11 prod t12 prod t13 prod t14) match {
      case JsSuccess((((((((((((((t1, t2), t3), t4), t5), t6), t7), t8), t9), t10), t11), t12), t13), t14)) => JsSuccess(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14)
      case JsError(e) => JsError(e)
    }
  }

  def product[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3], 
    t4: JsResult[T4], 
    t5: JsResult[T5], 
    t6: JsResult[T6], 
    t7: JsResult[T7], 
    t8: JsResult[T8], 
    t9: JsResult[T9], 
    t10: JsResult[T10], 
    t11: JsResult[T11], 
    t12: JsResult[T12], 
    t13: JsResult[T13], 
    t14: JsResult[T14], 
    t15: JsResult[T15]
  ): JsResult[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15)] = {
    (t1 prod t2 prod t3 prod t4 prod t5 prod t6 prod t7 prod t8 prod t9 prod t10 prod t11 prod t12 prod t13 prod t14 prod t15) match {
      case JsSuccess(((((((((((((((t1, t2), t3), t4), t5), t6), t7), t8), t9), t10), t11), t12), t13), t14), t15)) => JsSuccess(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15)
      case JsError(e) => JsError(e)
    }
  }

  def product[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3], 
    t4: JsResult[T4], 
    t5: JsResult[T5], 
    t6: JsResult[T6], 
    t7: JsResult[T7], 
    t8: JsResult[T8], 
    t9: JsResult[T9], 
    t10: JsResult[T10], 
    t11: JsResult[T11], 
    t12: JsResult[T12], 
    t13: JsResult[T13], 
    t14: JsResult[T14], 
    t15: JsResult[T15], 
    t16: JsResult[T16]
  ): JsResult[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16)] = {
    (t1 prod t2 prod t3 prod t4 prod t5 prod t6 prod t7 prod t8 prod t9 prod t10 prod t11 prod t12 prod t13 prod t14 prod t15 prod t16) match {
      case JsSuccess((((((((((((((((t1, t2), t3), t4), t5), t6), t7), t8), t9), t10), t11), t12), t13), t14), t15), t16)) => JsSuccess(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16)
      case JsError(e) => JsError(e)
    }
  }

  def product[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3], 
    t4: JsResult[T4], 
    t5: JsResult[T5], 
    t6: JsResult[T6], 
    t7: JsResult[T7], 
    t8: JsResult[T8], 
    t9: JsResult[T9], 
    t10: JsResult[T10], 
    t11: JsResult[T11], 
    t12: JsResult[T12], 
    t13: JsResult[T13], 
    t14: JsResult[T14], 
    t15: JsResult[T15], 
    t16: JsResult[T16], 
    t17: JsResult[T17]
  ): JsResult[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17)] = {
    (t1 prod t2 prod t3 prod t4 prod t5 prod t6 prod t7 prod t8 prod t9 prod t10 prod t11 prod t12 prod t13 prod t14 prod t15 prod t16 prod t17) match {
      case JsSuccess(((((((((((((((((t1, t2), t3), t4), t5), t6), t7), t8), t9), t10), t11), t12), t13), t14), t15), t16), t17)) => JsSuccess(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17)
      case JsError(e) => JsError(e)
    }
  } 

  def product[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3], 
    t4: JsResult[T4], 
    t5: JsResult[T5], 
    t6: JsResult[T6], 
    t7: JsResult[T7], 
    t8: JsResult[T8], 
    t9: JsResult[T9], 
    t10: JsResult[T10], 
    t11: JsResult[T11], 
    t12: JsResult[T12], 
    t13: JsResult[T13], 
    t14: JsResult[T14], 
    t15: JsResult[T15], 
    t16: JsResult[T16], 
    t17: JsResult[T17], 
    t18: JsResult[T18]
  ): JsResult[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18)] = {
    (t1 prod t2 prod t3 prod t4 prod t5 prod t6 prod t7 prod t8 prod t9 prod t10 prod t11 prod t12 prod t13 prod t14 prod t15 prod t16 prod t17 prod t18) match {
      case JsSuccess((((((((((((((((((t1, t2), t3), t4), t5), t6), t7), t8), t9), t10), t11), t12), t13), t14), t15), t16), t17), t18)) => JsSuccess(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18)
      case JsError(e) => JsError(e)
    }
  } 

  def product[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3], 
    t4: JsResult[T4], 
    t5: JsResult[T5], 
    t6: JsResult[T6], 
    t7: JsResult[T7], 
    t8: JsResult[T8], 
    t9: JsResult[T9], 
    t10: JsResult[T10], 
    t11: JsResult[T11], 
    t12: JsResult[T12], 
    t13: JsResult[T13], 
    t14: JsResult[T14], 
    t15: JsResult[T15], 
    t16: JsResult[T16], 
    t17: JsResult[T17], 
    t18: JsResult[T18], 
    t19: JsResult[T19]
  ): JsResult[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19)] = {
    (t1 prod t2 prod t3 prod t4 prod t5 prod t6 prod t7 prod t8 prod t9 prod t10 prod t11 prod t12 prod t13 prod t14 prod t15 prod t16 prod t17 prod t18 prod t19) match {
      case JsSuccess(((((((((((((((((((t1, t2), t3), t4), t5), t6), t7), t8), t9), t10), t11), t12), t13), t14), t15), t16), t17), t18), t19)) => JsSuccess(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18, t19)
      case JsError(e) => JsError(e)
    }
  } 

  def product[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3], 
    t4: JsResult[T4], 
    t5: JsResult[T5], 
    t6: JsResult[T6], 
    t7: JsResult[T7], 
    t8: JsResult[T8], 
    t9: JsResult[T9], 
    t10: JsResult[T10], 
    t11: JsResult[T11], 
    t12: JsResult[T12], 
    t13: JsResult[T13], 
    t14: JsResult[T14], 
    t15: JsResult[T15], 
    t16: JsResult[T16], 
    t17: JsResult[T17], 
    t18: JsResult[T18], 
    t19: JsResult[T19], 
    t20: JsResult[T20]
  ): JsResult[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20)] = {
    (t1 prod t2 prod t3 prod t4 prod t5 prod t6 prod t7 prod t8 prod t9 prod t10 prod t11 prod t12 prod t13 prod t14 prod t15 prod t16 prod t17 prod t18 prod t19 prod t20) match {
      case JsSuccess((((((((((((((((((((t1, t2), t3), t4), t5), t6), t7), t8), t9), t10), t11), t12), t13), t14), t15), t16), t17), t18), t19), t20)) => JsSuccess(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18, t19, t20)
      case JsError(e) => JsError(e)
    }
  } 

  def product[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3], 
    t4: JsResult[T4], 
    t5: JsResult[T5], 
    t6: JsResult[T6], 
    t7: JsResult[T7], 
    t8: JsResult[T8], 
    t9: JsResult[T9], 
    t10: JsResult[T10], 
    t11: JsResult[T11], 
    t12: JsResult[T12], 
    t13: JsResult[T13], 
    t14: JsResult[T14], 
    t15: JsResult[T15], 
    t16: JsResult[T16], 
    t17: JsResult[T17], 
    t18: JsResult[T18], 
    t19: JsResult[T19], 
    t20: JsResult[T20], 
    t21: JsResult[T21]
  ): JsResult[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21)] = {
    (t1 prod t2 prod t3 prod t4 prod t5 prod t6 prod t7 prod t8 prod t9 prod t10 prod t11 prod t12 prod t13 prod t14 prod t15 prod t16 prod t17 prod t18 prod t19 prod t20 prod t21) match {
      case JsSuccess(((((((((((((((((((((t1, t2), t3), t4), t5), t6), t7), t8), t9), t10), t11), t12), t13), t14), t15), t16), t17), t18), t19), t20), t21)) => JsSuccess(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18, t19, t20, t21)
      case JsError(e) => JsError(e)
    }
  } 

  def product[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3], 
    t4: JsResult[T4], 
    t5: JsResult[T5], 
    t6: JsResult[T6], 
    t7: JsResult[T7], 
    t8: JsResult[T8], 
    t9: JsResult[T9], 
    t10: JsResult[T10], 
    t11: JsResult[T11], 
    t12: JsResult[T12], 
    t13: JsResult[T13], 
    t14: JsResult[T14], 
    t15: JsResult[T15], 
    t16: JsResult[T16], 
    t17: JsResult[T17], 
    t18: JsResult[T18], 
    t19: JsResult[T19], 
    t20: JsResult[T20], 
    t21: JsResult[T21], 
    t22: JsResult[T22]
  ): JsResult[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22)] = {
    (t1 prod t2 prod t3 prod t4 prod t5 prod t6 prod t7 prod t8 prod t9 prod t10 prod t11 prod t12 prod t13 prod t14 prod t15 prod t16 prod t17 prod t18 prod t19 prod t20 prod t21 prod t22) match {
      case JsSuccess((((((((((((((((((((((t1, t2), t3), t4), t5), t6), t7), t8), t9), t10), t11), t12), t13), t14), t15), t16), t17), t18), t19), t20), t21), t22)) => JsSuccess(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18, t19, t20, t21, t22)
      case JsError(e) => JsError(e)
    }
  } 

}