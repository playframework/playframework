package play.api.libs.json

import scala.collection._

/**
 * Json serializer: write an implicit to define a serializer for any type
 */
trait Writes[T] {
  /**
   * Convert the object into a JsValue
   */
  def writes(o: T): JsValue
}

object Writes extends DefaultWrites

trait DefaultWrites {

  implicit object IntWrites extends Writes[Int] {
    def writes(o: Int) = JsNumber(o)
  }

  implicit object ShortWrites extends Writes[Short] {
    def writes(o: Short) = JsNumber(o)
  }

  implicit object LongWrites extends Writes[Long] {
    def writes(o: Long) = JsNumber(o)
  }

  implicit object FloatWrites extends Writes[Float] {
    def writes(o: Float) = JsNumber(o)
  }

  implicit object DoubleWrites extends Writes[Double] {
    def writes(o: Double) = JsNumber(o)
  }

  implicit object BooleanWrites extends Writes[Boolean] {
    def writes(o: Boolean) = JsBoolean(o)
  }

  implicit object StringWrites extends Writes[String] {
    def writes(o: String) = JsString(o)
  }

  implicit def listWrites[T](implicit fmt: Writes[T]): Writes[List[T]] = new Writes[List[T]] {
    def writes(ts: List[T]) = JsArray(ts.map(t => toJson(t)(fmt)))
  }

  implicit def seqWrites[T](implicit fmt: Writes[T]): Writes[Seq[T]] = new Writes[Seq[T]] {
    def writes(ts: Seq[T]) = JsArray(ts.toList.map(t => toJson(t)(fmt)))
  }

  implicit def arrayWrites[T](implicit fmt: Writes[T], mf: Manifest[T]): Writes[Array[T]] = new Writes[Array[T]] {
    def writes(ts: Array[T]) = JsArray((ts.map(t => toJson(t)(fmt))).toList)
  }
  def listToArray[T: Manifest](ls: List[T]): Array[T] = ls.toArray

  implicit def mapWrites[V](implicit fmtv: Writes[V]): Writes[collection.immutable.Map[String, V]] = new Writes[collection.immutable.Map[String, V]] {
    def writes(ts: collection.immutable.Map[String, V]) = JsObject(ts.map { case (k, v) => (k, toJson(v)(fmtv)) }.toList)
  }

  implicit def mutableSetWrites[T](implicit fmt: Writes[T]): Writes[mutable.Set[T]] =
    viaSeq((x: Seq[T]) => mutable.Set(x: _*))

  implicit def immutableSetWrites[T](implicit fmt: Writes[T]): Writes[immutable.Set[T]] =
    viaSeq((x: Seq[T]) => immutable.Set(x: _*))

  implicit def immutableSortedSetWrites[S](implicit ord: S => Ordered[S], binS: Writes[S]): Writes[immutable.SortedSet[S]] = {
    viaSeq((x: Seq[S]) => immutable.TreeSet[S](x: _*))
  }

  def viaSeq[S <: Iterable[T], T](f: Seq[T] => S)(implicit fmt: Writes[T]): Writes[S] = new Writes[S] {
    def writes(ts: S) = JsArray(ts.map(t => toJson(t)(fmt)).toList)
  }

  implicit object JsValueWrites extends Writes[JsValue] {
    def writes(o: JsValue) = o
    def reads(json: JsValue) = json
  }

  implicit object JsObjectWrites extends Writes[JsObject] {
    def writes(o: JsObject) = o
  }
}

