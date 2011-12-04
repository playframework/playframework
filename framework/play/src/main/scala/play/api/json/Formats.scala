package play.api.json

import play.api.Json._
import AST._

object Formats {

  implicit object IntFormat extends Format[Int] {
    def writes(o: Int) = JsNumber(o)
    def reads(json: JsValue) = json match {
      case JsNumber(n) => n.toInt
      case _ => throw new RuntimeException("Int expected")
    }
  }

  implicit object ShortFormat extends Format[Short] {
    def writes(o: Short) = JsNumber(o)
    def reads(json: JsValue) = json match {
      case JsNumber(n) => n.toShort
      case _ => throw new RuntimeException("Short expected")
    }
  }

  implicit object LongFormat extends Format[Long] {
    def writes(o: Long) = JsNumber(o)
    def reads(json: JsValue) = json match {
      case JsNumber(n) => n.toLong
      case _ => throw new RuntimeException("Long expected")
    }
  }

  implicit object FloatFormat extends Format[Float] {
    def writes(o: Float) = JsNumber(o)
    def reads(json: JsValue) = json match {
      case JsNumber(n) => n.toFloat
      case _ => throw new RuntimeException("Float expected")
    }
  }

  implicit object DoubleFormat extends Format[Double] {
    def writes(o: Double) = JsNumber(o)
    def reads(json: JsValue) = json match {
      case JsNumber(n) => n.toDouble
      case _ => throw new RuntimeException("Double expected")
    }
  }

  implicit object BooleanFormat extends Format[Boolean] {
    def writes(o: Boolean) = JsBoolean(o)
    def reads(json: JsValue) = json match {
      case JsBoolean(b) => b
      case _ => throw new RuntimeException("Boolean expected")
    }
  }

  implicit object StringFormat extends Format[String] {
    def writes(o: String) = JsString(o)
    def reads(json: JsValue) = json match {
      case JsString(s) => s
      case _ => throw new RuntimeException("String expected")
    }
  }

  implicit def listFormat[T](implicit fmt: Format[T]): Format[List[T]] = new Format[List[T]] {
    def writes(ts: List[T]) = JsArray(ts.map(t => tojson(t)(fmt)))
    def reads(json: JsValue) = json match {
      case JsArray(ts) => ts.map(t => fromjson(t)(fmt))
      case _ => throw new RuntimeException("List expected")
    }
  }

  implicit def seqFormat[T](implicit fmt: Format[T]): Format[Seq[T]] = new Format[Seq[T]] {
    def writes(ts: Seq[T]) = JsArray(ts.toList.map(t => tojson(t)(fmt)))
    def reads(json: JsValue) = json match {
      case JsArray(ts) => ts.map(t => fromjson(t)(fmt))
      case _ => throw new RuntimeException("Seq expected")
    }
  }

  import scala.reflect.Manifest
  implicit def arrayFormat[T](implicit fmt: Format[T], mf: Manifest[T]): Format[Array[T]] = new Format[Array[T]] {
    def writes(ts: Array[T]) = JsArray((ts.map(t => tojson(t)(fmt))).toList)
    def reads(json: JsValue) = json match {
      case JsArray(ts) => listToArray(ts.map(t => fromjson(t)(fmt)))
      case _ => throw new RuntimeException("Array expected")
    }
  }
  def listToArray[T: Manifest](ls: List[T]): Array[T] = ls.toArray

  implicit object JsValueFormat extends Format[JsValue] {
    def writes(o: JsValue) = o
    def reads(json: JsValue) = json
  }

}

