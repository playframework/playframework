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
trait Writes[-T] {

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
   * Serializer for BigDecimal types.
   */
  implicit object BigDecimalWrites extends Writes[BigDecimal] {
    def writes(o: BigDecimal) = JsNumber(o)
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
   * Serializer for Array[T] types.
   */
  implicit def arrayWrites[T](implicit fmt: Writes[T], mf: Manifest[T]): Writes[Array[T]] = new Writes[Array[T]] {
    def writes(ts: Array[T]) = JsArray((ts.map(t => toJson(t)(fmt))).toList)
  }

  /**
   * Serializer for Map[String,V] types.
   */
  implicit def mapWrites[V](implicit fmtv: Writes[V]): Writes[collection.immutable.Map[String, V]] = new Writes[collection.immutable.Map[String, V]] {
    def writes(ts: collection.immutable.Map[String, V]) = JsObject(ts.map { case (k, v) => (k, toJson(v)(fmtv)) }.toList)
  }

  /**
   * Serializer for Traversables types.
   */
  implicit def traversableWrites[A: Writes] = new Writes[Traversable[A]] {
    def writes(as: Traversable[A]) = JsArray(as.map(toJson(_)).toSeq)
  }

  /**
   * Serializer for JsValues.
   */
  implicit object JsValueWrites extends Writes[JsValue] {
    def writes(o: JsValue) = o
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

