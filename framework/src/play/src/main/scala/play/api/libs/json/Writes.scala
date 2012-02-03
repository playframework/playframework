package play.api.libs.json

import Json._
import scala.collection._
import scala.annotation.implicitNotFound

/**
 * Json serializer: write an implicit to define a serializer for any type
 */
@implicitNotFound(
  "No Json deserializer found for type ${T}. Try to implement an implicit Writes or Format for this type."
)
trait Writes[T] {

  /**
   * Convert the object into a JsValue
   */
  def writes(o: T): JsValue

}

/**
 * Default Serializers.
 */
object Writes extends DefaultWrites

/**
 * Default Serializers.
 */
trait DefaultWrites {

  /**
   * Serializer for Int types.
   */
  implicit object IntWrites extends Writes[Int] {
    def writes(o: Int) = JsNumber(o)
  }

  /**
   * Serializer for Short types.
   */
  implicit object ShortWrites extends Writes[Short] {
    def writes(o: Short) = JsNumber(o)
  }

  /**
   * Serializer for Long types.
   */
  implicit object LongWrites extends Writes[Long] {
    def writes(o: Long) = JsNumber(o)
  }

  /**
   * Serializer for Float types.
   */
  implicit object FloatWrites extends Writes[Float] {
    def writes(o: Float) = JsNumber(o)
  }

  /**
   * Serializer for Double types.
   */
  implicit object DoubleWrites extends Writes[Double] {
    def writes(o: Double) = JsNumber(o)
  }

  /**
   * Serializer for Boolean types.
   */
  implicit object BooleanWrites extends Writes[Boolean] {
    def writes(o: Boolean) = JsBoolean(o)
  }

  /**
   * Serializer for String types.
   */
  implicit object StringWrites extends Writes[String] {
    def writes(o: String) = JsString(o)
  }

  /**
   * Serializer for List[T] types.
   */
  implicit def listWrites[T](implicit fmt: Writes[T]): Writes[List[T]] = new Writes[List[T]] {
    def writes(ts: List[T]) = JsArray(ts.map(t => toJson(t)(fmt)))
  }

  /**
   * Serializer for Seq[T] types.
   */
  implicit def seqWrites[T](implicit fmt: Writes[T]): Writes[Seq[T]] = new Writes[Seq[T]] {
    def writes(ts: Seq[T]) = JsArray(ts.toList.map(t => toJson(t)(fmt)))
  }

  /**
   * Serializer for Array[T] types.
   */
  implicit def arrayWrites[T](implicit fmt: Writes[T], mf: Manifest[T]): Writes[Array[T]] = new Writes[Array[T]] {
    def writes(ts: Array[T]) = JsArray((ts.map(t => toJson(t)(fmt))).toList)
  }

  private def listToArray[T: Manifest](ls: List[T]): Array[T] = ls.toArray

  /**
   * Serializer for Map[String,V] types.
   */
  implicit def mapWrites[V](implicit fmtv: Writes[V]): Writes[collection.immutable.Map[String, V]] = new Writes[collection.immutable.Map[String, V]] {
    def writes(ts: collection.immutable.Map[String, V]) = JsObject(ts.map { case (k, v) => (k, toJson(v)(fmtv)) }.toList)
  }

  /**
   * Serializer for Set[T] types.
   */
  implicit def mutableSetWrites[T](implicit fmt: Writes[T]): Writes[mutable.Set[T]] = {
    viaSeq((x: Seq[T]) => mutable.Set(x: _*))
  }

  /**
   * Serializer for Set[T] types.
   */
  implicit def immutableSetWrites[T](implicit fmt: Writes[T]): Writes[immutable.Set[T]] = {
    viaSeq((x: Seq[T]) => immutable.Set(x: _*))
  }

  /**
   * Serializer for Set[T] types.
   */
  implicit def immutableSortedSetWrites[S](implicit ord: S => Ordered[S], binS: Writes[S]): Writes[immutable.SortedSet[S]] = {
    viaSeq((x: Seq[S]) => immutable.TreeSet[S](x: _*))
  }

  private def viaSeq[S <: Iterable[T], T](f: Seq[T] => S)(implicit fmt: Writes[T]): Writes[S] = new Writes[S] {
    def writes(ts: S) = JsArray(ts.map(t => toJson(t)(fmt)).toList)
  }

  /**
   * Serializer for JsValues.
   */
  implicit object JsValueWrites extends Writes[JsValue] {
    def writes(o: JsValue) = o
    def reads(json: JsValue) = json
  }

  /**
   * Serializer for JsObjects.
   */
  implicit object JsObjectWrites extends Writes[JsObject] {
    def writes(o: JsObject) = o
  }

  /**
   * Serializer for Option.
   */
  implicit def OptionWrites[T](implicit fmt: Writes[T]): Writes[Option[T]] = new Writes[Option[T]] {
    import scala.util.control.Exception._
    def writes(o: Option[T]) = o match {
      case Some(value) => fmt.writes(value)
      case None => JsNull
    }
  }

}

