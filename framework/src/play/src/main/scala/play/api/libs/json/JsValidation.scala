package play.api.libs.json

import play.api.data.validation.ValidationError
import JsResultHelpers._
import Json._

object Constraint {
  val defaultJs = JsUndefined("default")

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

  def pruned[T](implicit w: Writes[T]): Writes[T] = new Writes[T] {
    def writes(t: T): JsValue = JsUndefined("pruned")
  }
}

object JsValidator {
  import Constraint.defaultJs

  case class JsFlow[T] private[JsValidator](in: Option[Reads[T]] = None, out: Option[Writes[T]] = None)
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

  def in[T](implicit fmt: Format[T]): JsFlow[T] = JsFlow(Some(fmt), None)
  def in[T](r: Reads[T])(implicit fmt: Format[T]): JsFlow[T] = JsFlow(Some(r), None)

  def out[T](w: Writes[T])(implicit fmt: Format[T]): JsFlow[T] = JsFlow(None, Some(w))

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
  import Constraint.defaultJs

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