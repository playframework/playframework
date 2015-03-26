/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.json

import java.time.{
  Instant,
  LocalDate,
  LocalDateTime,
  ZoneId,
  ZonedDateTime
}
import java.time.temporal.Temporal
import java.time.format.DateTimeFormatter

import scala.annotation.implicitNotFound
import scala.collection._
import scala.reflect.ClassTag

import com.fasterxml.jackson.databind.JsonNode

import Json._

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
  "No Json serializer as JsObject found for type ${A}. Try to implement an implicit OWrites or OFormat for this type."
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
  import scala.language.implicitConversions

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
   * Serializer for Byte types.
   */
  implicit object ByteWrites extends Writes[Byte] {
    def writes(o: Byte) = JsNumber(o)
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
   * Serializer for Jackson JsonNode
   */
  implicit object JsonNodeWrites extends Writes[JsonNode] {
    def writes(o: JsonNode): JsValue = JacksonJson.jsonNodeToJsValue(o)
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
   * Default Serializer java.util.Date -> JsNumber(d.getTime (nb of ms))
   */
  implicit object DefaultDateWrites extends Writes[java.util.Date] {
    def writes(d: java.util.Date): JsValue = JsNumber(d.getTime)
  }

  /** Typeclass to implement way of formatting of Java8 temporal types. */
  trait TemporalFormatter[T <: Temporal] {
    def format(temporal: T): String
  }

  /** Formatting companion */
  object TemporalFormatter {
    implicit def DefaultLocalDateTimeFormatter(formatter: DateTimeFormatter): TemporalFormatter[LocalDateTime] = new TemporalFormatter[LocalDateTime] {
      def format(temporal: LocalDateTime): String = formatter.format(temporal)
    }

    implicit def PatternLocalDateTimeFormatter(pattern: String): TemporalFormatter[LocalDateTime] = new TemporalFormatter[LocalDateTime] {
      def format(temporal: LocalDateTime): String =
        DateTimeFormatter.ofPattern(pattern).format(temporal)
    }

    implicit def DefaultZonedDateTimeFormatter(formatter: DateTimeFormatter): TemporalFormatter[ZonedDateTime] = new TemporalFormatter[ZonedDateTime] {
      def format(temporal: ZonedDateTime): String = formatter.format(temporal)
    }

    implicit def PatternZonedDateTimeFormatter(pattern: String): TemporalFormatter[ZonedDateTime] = new TemporalFormatter[ZonedDateTime] {
      def format(temporal: ZonedDateTime): String =
        DateTimeFormatter.ofPattern(pattern).format(temporal)
    }

    implicit def DefaultDateFormatter(formatter: DateTimeFormatter): TemporalFormatter[LocalDate] = new TemporalFormatter[LocalDate] {
      def format(temporal: LocalDate): String = formatter.format(temporal)
    }

    implicit def PatternDateFormatter(pattern: String): TemporalFormatter[LocalDate] = new TemporalFormatter[LocalDate] {
      def format(temporal: LocalDate): String =
        DateTimeFormatter.ofPattern(pattern).format(temporal)
    }

    implicit def DefaultInstantFormatter(formatter: DateTimeFormatter): TemporalFormatter[Instant] = new TemporalFormatter[Instant] {
      def format(temporal: Instant): String =
        formatter format LocalDateTime.ofInstant(temporal, ZoneId.systemDefault)
    }

    implicit def PatternInstantFormatter(pattern: String): TemporalFormatter[Instant] = new TemporalFormatter[Instant] {
      def format(temporal: Instant): String =
        DateTimeFormatter.ofPattern(pattern).
          format(LocalDateTime.ofInstant(temporal, ZoneId.systemDefault))
    }
  }

  /**
   * Serializer for Java8 temporal types (e.g. `java.time.LocalDateTime`)
   * to be written as JSON string, using the default time zone.
   *
   * @tparam T Type of formatting argument
   * @param formating an argument to instantiate formatter
   *
   * {{{
   * import java.time.LocalDateTime
   * import play.api.libs.json.Writes
   * import play.api.libs.json.Java8Writes
   *
   * implicit val Writes: Writes[LocalDateTime] =
   *   temporalWrites[LocalDateTime, DateTimeFormatter](
   *     DateTimeFormatter.ISO_LOCAL_DATE_TIME)
   * }}}
   */
  def temporalWrites[A <: Temporal, B](formatting: B)(implicit f: B => TemporalFormatter[A]): Writes[A] = new Writes[A] {
    def writes(temporal: A): JsValue = JsString(f(formatting) format temporal)
  }

  /**
   * The default typeclass to write a `java.time.LocalDateTime`,
   * using '2011-12-03T10:15:30' format, and default time zone.
   */
  implicit val DefaultLocalDateTimeWrites =
    temporalWrites[LocalDateTime, DateTimeFormatter](
      DateTimeFormatter.ISO_LOCAL_DATE_TIME)

  /**
   * The default typeclass to write a `java.time.ZonedDateTime`,
   * using '2011-12-03T10:15:30' format, and default time zone.
   */
  implicit val DefaultZonedDateTimeWrites =
    temporalWrites[ZonedDateTime, DateTimeFormatter](
      DateTimeFormatter.ISO_LOCAL_DATE_TIME)

  /**
   * The default typeclass to write a `java.time.LocalDate`,
   * using '2011-12-03' format, and default time zone.
   */
  implicit val DefaultLocalDateWrites =
    temporalWrites[LocalDate, DateTimeFormatter](
      DateTimeFormatter.ISO_LOCAL_DATE)

  /**
   * The default typeclass to write a `java.time.Instant`,
   * using '2011-12-03T10:15:30' format, and default time zone.
   */
  implicit val DefaultInstantWrites =
    temporalWrites[Instant, DateTimeFormatter](
      DateTimeFormatter.ISO_LOCAL_DATE_TIME)

  /**
   * Serializer for `java.time.LocalDateTime` as JSON number.
   *
   * {{{
   * import java.time.LocalDateTime
   * import play.api.libs.json.Writes
   * import play.api.libs.json.Java8Writes
   *
   * implicit val Writes = LocalDateTimeNumberWrites[LocalDateTime]
   * }}}
   */
  val LocalDateTimeNumberWrites: Writes[LocalDateTime] =
    new Writes[LocalDateTime] {
      lazy val formatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

      def writes(t: LocalDateTime): JsValue = JsNumber(BigDecimal.valueOf(
        Instant.parse(formatter format t).toEpochMilli))
    }

  /**
   * Serializer for `java.time.ZonedDateTime` as JSON number.
   *
   * {{{
   * import java.time.ZonedDateTime
   * import play.api.libs.json.Writes
   * import play.api.libs.json.Java8Writes
   *
   * implicit val Writes = ZonedDateTimeNumberWrites[ZonedDateTime]
   * }}}
   */
  val ZonedDateTimeNumberWrites: Writes[ZonedDateTime] =
    new Writes[ZonedDateTime] {
      def writes(t: ZonedDateTime): JsValue =
        JsNumber(BigDecimal valueOf t.toInstant.toEpochMilli)
    }

  /**
   * Serializer for `java.time.LocalDate` as JSON number.
   *
   * {{{
   * import java.time.LocalDate
   * import play.api.libs.json.Writes
   * import play.api.libs.json.Java8Writes
   *
   * implicit val Writes = LocalDateNumberWrites[LocalDate]
   * }}}
   */
  val LocalDateNumberWrites: Writes[LocalDate] = new Writes[LocalDate] {
    lazy val formatter =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'00:00:00'Z'")

    def writes(t: LocalDate): JsValue = JsNumber(BigDecimal.valueOf(
      Instant.parse(formatter format t).toEpochMilli))
  }

  /**
   * Serializer for `java.time.Instant` as JSON number.
   *
   * {{{
   * import java.time.Instant
   * import play.api.libs.json.Writes
   * import play.api.libs.json.Java8Writes
   *
   * implicit val Writes = InstantNumberWrites[Instant]
   * }}}
   */
  val InstantNumberWrites: Writes[Instant] = new Writes[Instant] {
    def writes(t: Instant): JsValue =
      JsNumber(BigDecimal valueOf t.toEpochMilli)
  }

  /**
   * Serializer for org.joda.time.DateTime
   * @param pattern the pattern used by SimpleDateFormat
   */
  def jodaDateWrites(pattern: String): Writes[org.joda.time.DateTime] = new Writes[org.joda.time.DateTime] {
    val df = org.joda.time.format.DateTimeFormat.forPattern(pattern)
    def writes(d: org.joda.time.DateTime): JsValue = JsString(d.toString(df))
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
    val df = org.joda.time.format.DateTimeFormat.forPattern(pattern)
    def writes(d: org.joda.time.LocalDate): JsValue = JsString(d.toString(df))
  }

  /**
   * Default Serializer org.joda.time.LocalDate -> JsString(ISO8601 format (yyyy-MM-dd))
   */
  implicit object DefaultJodaLocalDateWrites extends Writes[org.joda.time.LocalDate] {
    def writes(d: org.joda.time.LocalDate): JsValue = JsString(d.toString)
  }

  /**
   * Serializer for org.joda.time.LocalTime
   * @param pattern the pattern used by org.joda.time.format.DateTimeFormat
   */
  def jodaLocalTimeWrites(pattern: String): Writes[org.joda.time.LocalTime] = new Writes[org.joda.time.LocalTime] {
    def writes(d: org.joda.time.LocalTime): JsValue = JsString(d.toString(pattern))
  }

  /**
   * Default Serializer org.joda.time.LocalDate -> JsString(ISO8601 format (HH:mm:ss.SSS))
   */
  implicit object DefaultJodaLocalTimeWrites extends Writes[org.joda.time.LocalTime] {
    def writes(d: org.joda.time.LocalTime): JsValue = JsString(d.toString)
  }

  /**
   * Serializer for java.sql.Date
   * @param pattern the pattern used by SimpleDateFormat
   */
  def sqlDateWrites(pattern: String): Writes[java.sql.Date] = new Writes[java.sql.Date] {
    def writes(d: java.sql.Date): JsValue = JsString(new java.text.SimpleDateFormat(pattern).format(d))
  }

  /**
   * Serializer for java.util.UUID
   */
  implicit object UuidWrites extends Writes[java.util.UUID] {
    def writes(u: java.util.UUID) = JsString(u.toString())
  }

  /**
   * Serializer for scala.Enumeration by name.
   */
  implicit def enumNameWrites[E <: Enumeration]: Writes[E#Value] = new Writes[E#Value] {
    def writes(value: E#Value): JsValue = JsString(value.toString)
  }

  /**
   * Serializer for Any, used for the args of ValidationErrors.
   */
  private[json] object anyWrites extends Writes[Any] {
    def writes(a: Any): JsValue = a match {
      case s: String => JsString(s)
      case nb: Int => JsNumber(nb)
      case nb: Short => JsNumber(nb)
      case nb: Long => JsNumber(nb)
      case nb: Double => JsNumber(nb)
      case nb: Float => JsNumber(nb)
      case b: Boolean => JsBoolean(b)
      case js: JsValue => js
      case x => JsString(x.toString)
    }
  }
}
