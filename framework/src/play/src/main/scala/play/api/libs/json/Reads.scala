package play.api.libs.json

import scala.collection._
import Json._
import scala.annotation.implicitNotFound

/**
 * Json deserializer: write an implicit to define a deserializer for any type.
 */
@implicitNotFound(
  "No Json deserializer found for type ${T}. Try to implement an implicit Reads or Format for this type."
)
trait Reads[T] {

  /**
   * Convert the JsValue into a T
   */
  def reads(json: JsValue): T

}

/**
 * Default deserializer type classes.
 */
object Reads extends DefaultReads

/**
 * Default deserializer type classes.
 */
trait DefaultReads {

  /**
   * Deserializer for Int types.
   */
  implicit object IntReads extends Reads[Int] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) => n.toInt
      case _ => throw new RuntimeException("Int expected")
    }
  }

  /**
   * Deserializer for Short types.
   */
  implicit object ShortReads extends Reads[Short] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) => n.toShort
      case _ => throw new RuntimeException("Short expected")
    }
  }

  /**
   * Deserializer for Long types.
   */
  implicit object LongReads extends Reads[Long] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) => n.toLong
      case _ => throw new RuntimeException("Long expected")
    }
  }

  /**
   * Deserializer for Float types.
   */
  implicit object FloatReads extends Reads[Float] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) => n.toFloat
      case _ => throw new RuntimeException("Float expected")
    }
  }

  /**
   * Deserializer for Double types.
   */
  implicit object DoubleReads extends Reads[Double] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) => n.toDouble
      case _ => throw new RuntimeException("Double expected")
    }
  }

  /**
   * Deserializer for Boolean types.
   */
  implicit object BooleanReads extends Reads[Boolean] {
    def reads(json: JsValue) = json match {
      case JsBoolean(b) => b
      case _ => throw new RuntimeException("Boolean expected")
    }
  }

  /**
   * Deserializer for String types.
   */
  implicit object StringReads extends Reads[String] {
    def reads(json: JsValue) = json match {
      case JsString(s) => s
      case _ => throw new RuntimeException("String expected")
    }
  }

  /**
   * Deserializer for List[T] types.
   */
  implicit def listReads[T](implicit fmt: Reads[T]): Reads[List[T]] = new Reads[List[T]] {
    def reads(json: JsValue) = json match {
      case JsArray(ts) => ts.map(t => fromJson(t)(fmt)).toList
      case _ => throw new RuntimeException("List expected")
    }
  }

  /**
   * Deserializer for Seq[T] types.
   */
  implicit def seqReads[T](implicit fmt: Reads[T]): Reads[Seq[T]] = new Reads[Seq[T]] {
    def reads(json: JsValue) = json match {
      case JsArray(ts) => ts.map(t => fromJson(t)(fmt))
      case _ => throw new RuntimeException("Seq expected")
    }
  }

  /**
   * Deserializer for Array[T] types.
   */
  implicit def arrayReads[T](implicit fmt: Reads[T], mf: Manifest[T]): Reads[Array[T]] = new Reads[Array[T]] {
    def reads(json: JsValue) = json match {
      case JsArray(ts) => listToArray(ts.map(t => fromJson(t)(fmt)).toList)
      case _ => throw new RuntimeException("Array expected")
    }
  }

  private[this] def listToArray[T: Manifest](ls: List[T]): Array[T] = ls.toArray

  /**
   * Deserializer for Map[String,V] types.
   */
  implicit def mapReads[V](implicit fmtv: Reads[V]): Reads[collection.immutable.Map[String, V]] = new Reads[collection.immutable.Map[String, V]] {
    def reads(json: JsValue) = json match {
      case JsObject(m) => m.map { case (k, v) => (k -> fromJson[V](v)(fmtv)) }.toMap
      case _ => throw new RuntimeException("Map expected")
    }
  }

  /**
   * Deserializer for Set[T] types.
   */
  implicit def mutableSetReads[T](implicit fmt: Reads[T]): Reads[mutable.Set[T]] = {
    viaSeq((x: Seq[T]) => mutable.Set(x: _*))
  }

  /**
   * Deserializer for Set[T] types.
   */
  implicit def immutableSetReads[T](implicit fmt: Reads[T]): Reads[immutable.Set[T]] = {
    viaSeq((x: Seq[T]) => immutable.Set(x: _*))
  }

  /**
   * Deserializer for SortedSet[T] types.
   */
  implicit def immutableSortedSetReads[S](implicit ord: S => Ordered[S], binS: Reads[S]): Reads[immutable.SortedSet[S]] = {
    viaSeq((x: Seq[S]) => immutable.TreeSet[S](x: _*))
  }

  private def viaSeq[S <: Iterable[T], T](f: Seq[T] => S)(implicit fmt: Reads[T]): Reads[S] = new Reads[S] {
    def reads(json: JsValue) = json match {
      case JsArray(ts) => f(ts.map(t => fromJson[T](t)))
      case _ => throw new RuntimeException("Collection expected")
    }
  }

  /**
   * Deserializer for JsValue.
   */
  implicit object JsValueReads extends Reads[JsValue] {
    def writes(o: JsValue) = o
    def reads(json: JsValue) = json
  }

  /**
   * Deserializer for JsObject.
   */
  implicit object JsObjectReads extends Reads[JsObject] {
    def reads(json: JsValue) = json match {
      case o @ JsObject(_) => o
      case _ => throw new RuntimeException("JsObject expected")
    }
  }

  implicit def OptionReads[T](implicit fmt: Reads[T]): Reads[Option[T]] = new Reads[Option[T]] {
    import scala.util.control.Exception._
    def reads(json: JsValue) = catching(classOf[RuntimeException]).opt(fmt.reads(json))
  }

}

