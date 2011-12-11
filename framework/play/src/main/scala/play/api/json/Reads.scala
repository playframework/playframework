package play.api.json

import scala.collection._

/**
 * Json deserializer: write an implicit to define a deserializer for any type
 */
trait Reads[T] {
  /**
   * Convert the JsValue into a T
   */
  def reads(json: JsValue): T
}

object Reads extends DefaultReads

trait DefaultReads {

  implicit object IntReads extends Reads[Int] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) => n.toInt
      case _ => throw new RuntimeException("Int expected")
    }
  }

  implicit object ShortReads extends Reads[Short] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) => n.toShort
      case _ => throw new RuntimeException("Short expected")
    }
  }

  implicit object LongReads extends Reads[Long] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) => n.toLong
      case _ => throw new RuntimeException("Long expected")
    }
  }

  implicit object FloatReads extends Reads[Float] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) => n.toFloat
      case _ => throw new RuntimeException("Float expected")
    }
  }

  implicit object DoubleReads extends Reads[Double] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) => n.toDouble
      case _ => throw new RuntimeException("Double expected")
    }
  }

  implicit object BooleanReads extends Reads[Boolean] {
    def reads(json: JsValue) = json match {
      case JsBoolean(b) => b
      case _ => throw new RuntimeException("Boolean expected")
    }
  }

  implicit object StringReads extends Reads[String] {
    def reads(json: JsValue) = json match {
      case JsString(s) => s
      case _ => throw new RuntimeException("String expected")
    }
  }

  implicit def listReads[T](implicit fmt: Reads[T]): Reads[List[T]] = new Reads[List[T]] {
    def reads(json: JsValue) = json match {
      case JsArray(ts) => ts.map(t => fromJson(t)(fmt))
      case _ => throw new RuntimeException("List expected")
    }
  }

  implicit def seqReads[T](implicit fmt: Reads[T]): Reads[Seq[T]] = new Reads[Seq[T]] {
    def reads(json: JsValue) = json match {
      case JsArray(ts) => ts.map(t => fromJson(t)(fmt))
      case _ => throw new RuntimeException("Seq expected")
    }
  }

  implicit def arrayReads[T](implicit fmt: Reads[T], mf: Manifest[T]): Reads[Array[T]] = new Reads[Array[T]] {
    def reads(json: JsValue) = json match {
      case JsArray(ts) => listToArray(ts.map(t => fromJson(t)(fmt)))
      case _ => throw new RuntimeException("Array expected")
    }
  }
  def listToArray[T: Manifest](ls: List[T]): Array[T] = ls.toArray

  implicit def mapReads[K, V](implicit fmtk: Reads[K], fmtv: Reads[V]): Reads[Map[K, V]] = new Reads[Map[K, V]] {
    def reads(json: JsValue) = json match {
      case JsObject(m) => Map() ++ m.map { case (k, v) => (fromJson[K](JsString(k))(fmtk), fromJson[V](v)(fmtv)) }
      case _ => throw new RuntimeException("Map expected")
    }
  }

  implicit def mutableSetReads[T](implicit fmt: Reads[T]): Reads[mutable.Set[T]] =
    viaSeq((x: Seq[T]) => mutable.Set(x: _*))

  implicit def immutableSetReads[T](implicit fmt: Reads[T]): Reads[immutable.Set[T]] =
    viaSeq((x: Seq[T]) => immutable.Set(x: _*))

  implicit def immutableSortedSetReads[S](implicit ord: S => Ordered[S], binS: Reads[S]): Reads[immutable.SortedSet[S]] = {
    viaSeq((x: Seq[S]) => immutable.TreeSet[S](x: _*))
  }

  def viaSeq[S <: Iterable[T], T](f: Seq[T] => S)(implicit fmt: Reads[T]): Reads[S] = new Reads[S] {
    def reads(json: JsValue) = json match {
      case JsArray(ts) => f(ts.map(t => fromJson[T](t)))
      case _ => throw new RuntimeException("Collection expected")
    }
  }

  implicit object JsValueReads extends Reads[JsValue] {
    def writes(o: JsValue) = o
    def reads(json: JsValue) = json
  }

}

