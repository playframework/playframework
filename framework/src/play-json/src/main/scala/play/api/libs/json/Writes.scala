package play.api.libs.json

import Json._
import scala.collection._
import scala.annotation.implicitNotFound
import scala.reflect.ClassTag

/**
 * Json serializer: write an implicit to define a serializer for any type
 */
@implicitNotFound(
  "No Json serializer found for type ${A}. Try to implement an implicit Writes or Format for this type."
)
trait Writes[-A] {

  /**
   * Convert the object into a JsValue
   */
  def writes(o: A): JsValue

  /**
   * transforms the resulting JsValue using transformer function
   */
  def transform(transformer: JsValue => JsValue): Writes[A] = Writes[A] { a => transformer(this.writes(a)) }

  /**
   * transforms resulting JsValue using Writes[JsValue]
   */
  def transform(transformer: Writes[JsValue]): Writes[A] = Writes[A] { a => transformer.writes(this.writes(a)) }

}

@implicitNotFound(
  "No Json serializer as JsObject found for type ${A}. Try to implement an implicit OWrites or Format for this type."
)
trait OWrites[-A] extends Writes[A] {

  def writes(o: A): JsObject

}

object OWrites extends PathWrites with ConstraintWrites {
  import play.api.libs.functional._

  implicit val functionalCanBuildOWrites: FunctionalCanBuild[OWrites] = new FunctionalCanBuild[OWrites] {

    def apply[A, B](wa: OWrites[A], wb: OWrites[B]): OWrites[A ~ B] = OWrites[A ~ B] { case a ~ b => wa.writes(a).deepMerge(wb.writes(b)) }

  }

  implicit val contravariantfunctorOWrites: ContravariantFunctor[OWrites] = new ContravariantFunctor[OWrites] {

    def contramap[A, B](wa: OWrites[A], f: B => A): OWrites[B] = OWrites[B](b => wa.writes(f(b)))

  }

  def apply[A](f: A => JsObject): OWrites[A] = new OWrites[A] {
    def writes(a: A): JsObject = f(a)
  }

}

/**
 * Default Serializers.
 */
object Writes extends PathWrites with ConstraintWrites with DefaultWrites {

  val constraints: ConstraintWrites = this
  val path: PathWrites = this

  import play.api.libs.functional._

  /*implicit val contravariantfunctorWrites:ContravariantFunctor[Writes] = new ContravariantFunctor[Writes] {

    def contramap[A,B](wa:Writes[A], f: B => A):Writes[B] = Writes[B]( b => wa.writes(f(b)) )

  }*/

  def apply[A](f: A => JsValue): Writes[A] = new Writes[A] {

    def writes(a: A): JsValue = f(a)

  }

}

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
  implicit def arrayWrites[T: ClassTag](implicit fmt: Writes[T]): Writes[Array[T]] = new Writes[Array[T]] {
    def writes(ts: Array[T]) = JsArray((ts.map(t => toJson(t)(fmt))).toList)
  }

  /**
   * Serializer for Map[String,V] types.
   */
  implicit def mapWrites[V](implicit fmtv: Writes[V]): OWrites[collection.immutable.Map[String, V]] = OWrites[collection.immutable.Map[String, V]] { ts =>
    JsObject(ts.map { case (k, v) => (k, toJson(v)(fmtv)) }.toList)
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

  /**
   * Serializer for java.util.Date
   * @param pattern the pattern used by SimpleDateFormat
   */
  def dateWrites(pattern: String): Writes[java.util.Date] = new Writes[java.util.Date] {
    def writes(d: java.util.Date): JsValue = JsString(new java.text.SimpleDateFormat(pattern).format(d))
  }

  /**
   * Default Serializer java.uti.Date -> JsNumber(d.getTime (nb of ms))
   */
  implicit object DefaultDateWrites extends Writes[java.util.Date] {
    def writes(d: java.util.Date): JsValue = JsNumber(d.getTime)
  }

  /**
   * Serializer for org.joda.time.DateTime
   * @param pattern the pattern used by SimpleDateFormat
   */
  def jodaDateWrites(pattern: String): Writes[org.joda.time.DateTime] = new Writes[org.joda.time.DateTime] {
    def writes(d: org.joda.time.DateTime): JsValue = JsString(d.toString(pattern))
  }

  /**
   * Default Serializer org.joda.time.DateTime -> JsNumber(d.getMillis (nb of ms))
   */
  implicit object DefaultJodaDateWrites extends Writes[org.joda.time.DateTime] {
    def writes(d: org.joda.time.DateTime): JsValue = JsNumber(d.getMillis)
  }

  /**
   * Serializer for org.joda.time.LocalDate
   * @param pattern the pattern used by org.joda.time.format.DateTimeFormat
   */
  def jodaLocalDateWrites(pattern: String): Writes[org.joda.time.LocalDate] = new Writes[org.joda.time.LocalDate] {
    def writes(d: org.joda.time.LocalDate): JsValue = JsString(d.toString(pattern))
  }

  /**
   * Default Serializer org.joda.time.LocalDate -> JsString(ISO8601 format (yyyy-MM-dd))
   */
  implicit object DefaultJodaLocalDateWrites extends Writes[org.joda.time.LocalDate] {
    def writes(d: org.joda.time.LocalDate): JsValue = JsString(d.toString)
  }

  /**
   * Serializer for java.sql.Date
   * @param pattern the pattern used by SimpleDateFormat
   */
  def sqlDateWrites(pattern: String): Writes[java.sql.Date] = new Writes[java.sql.Date] {
    def writes(d: java.sql.Date): JsValue = JsString(new java.text.SimpleDateFormat(pattern).format(d))
  }

}

