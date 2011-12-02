package play.api

import com.codahale.jerkson.AST._

object Json {

  def parse(input: String): JValue = com.codahale.jerkson.Json.parse[JValue](input)

  def tojson[T](o: T)(implicit tjs: Writes[T]): JValue = tjs.writes(o)

  def fromjson[T](json: JValue)(implicit fjs: Reads[T]): T = fjs.reads(json)

  trait Writes[T] {
    def writes(o: T): JValue
  }

  trait Reads[T] {
    def reads(json: JValue): T
  }

  trait Format[T] extends Writes[T] with Reads[T]

  implicit object IntFormat extends Format[Int] {
    def writes(o: Int) = JInt(o)
    def reads(json: JValue) = json match {
      case JInt(n) => n.toInt
      case _ => throw new RuntimeException("Int expected")
    }
  }

  implicit object ShortFormat extends Format[Short] {
    def writes(o: Short) = JInt(o)
    def reads(json: JValue) = json match {
      case JInt(n) => n.toShort
      case _ => throw new RuntimeException("Short expected")
    }
  }

  implicit object LongFormat extends Format[Long] {
    def writes(o: Long) = JInt(o)
    def reads(json: JValue) = json match {
      case JInt(n) => n.toLong
      case _ => throw new RuntimeException("Long expected")
    }
  }

  implicit object FloatFormat extends Format[Float] {
    def writes(o: Float) = JFloat(o)
    def reads(json: JValue) = json match {
      case JFloat(n) => n.toFloat
      case _ => throw new RuntimeException("Float expected")
    }
  }

  implicit object DoubleFormat extends Format[Double] {
    def writes(o: Double) = JFloat(o)
    def reads(json: JValue) = json match {
      case JInt(n) => n.toDouble
      case _ => throw new RuntimeException("Double expected")
    }
  }

  implicit object BooleanFormat extends Format[Boolean] {
    def writes(o: Boolean) = JBoolean(o)
    def reads(json: JValue) = json match {
      case JBoolean(b) => b
      case _ => throw new RuntimeException("Boolean expected")
    }
  }

  implicit object StringFormat extends Format[String] {
    def writes(o: String) = JString(o)
    def reads(json: JValue) = json match {
      case JString(s) => s
      case _ => throw new RuntimeException("String expected")
    }
  }

  implicit def listFormat[T](implicit fmt: Format[T]): Format[List[T]] = new Format[List[T]] {
    def writes(ts: List[T]) = JArray(ts.map(t => tojson(t)(fmt)))
    def reads(json: JValue) = json match {
      case JArray(ts) => ts.map(t => fromjson(t)(fmt))
      case _ => throw new RuntimeException("List expected")
    }
  }

  implicit def seqFormat[T](implicit fmt: Format[T]): Format[Seq[T]] = new Format[Seq[T]] {
    def writes(ts: Seq[T]) = JArray(ts.toList.map(t => tojson(t)(fmt)))
    def reads(json: JValue) = json match {
      case JArray(ts) => ts.map(t => fromjson(t)(fmt))
      case _ => throw new RuntimeException("Seq expected")
    }
  }

  import scala.reflect.Manifest
  implicit def arrayFormat[T](implicit fmt: Format[T], mf: Manifest[T]): Format[Array[T]] = new Format[Array[T]] {
    def writes(ts: Array[T]) = JArray((ts.map(t => tojson(t)(fmt))).toList)
    def reads(json: JValue) = json match {
      case JArray(ts) => listToArray(ts.map(t => fromjson(t)(fmt)))
      case _ => throw new RuntimeException("Array expected")
    }
  }
  def listToArray[T: Manifest](ls: List[T]): Array[T] = ls.toArray

  implicit object JValueFormat extends Format[JValue] {
    def writes(o: JValue) = o
    def reads(json: JValue) = json
  }

}

