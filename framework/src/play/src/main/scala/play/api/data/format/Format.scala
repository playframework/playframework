/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.data.format

import java.sql.Timestamp
import java.time._
import java.time.format.DateTimeFormatter
import java.util.UUID

import play.api.data._

import annotation.implicitNotFound

/**
 * Handles field binding and unbinding.
 */
@implicitNotFound(
  msg = "Cannot find Formatter type class for ${T}. Perhaps you will need to import play.api.data.format.Formats._ "
)
trait Formatter[T] {

  /**
   * The expected format of `Any`.
   */
  val format: Option[(String, Seq[Any])] = None

  /**
   * Binds this field, i.e. constructs a concrete value from submitted data.
   *
   * @param key the field key
   * @param data the submitted data
   * @return Either a concrete value of type T or a set of error if the binding failed.
   */
  def bind(key: String, data: Map[String, String]): Either[Seq[FormError], T]

  /**
   * Unbinds this field, i.e. transforms a concrete value to plain data.
   *
   * @param key the field ke
   * @param value the value to unbind
   * @return either the plain data or a set of errors if unbinding failed
   */
  def unbind(key: String, value: T): Map[String, String]
}

/** This object defines several default formatters. */
object Formats {

  /**
   * Formatter for ignored values.
   *
   * @param value As we ignore this parameter in binding/unbinding we have to provide a default value.
   */
  def ignoredFormat[A](value: A): Formatter[A] = new Formatter[A] {
    def bind(key: String, data: Map[String, String]) = Right(value)
    def unbind(key: String, value: A) = Map.empty
  }

  /**
   * Default formatter for the `String` type.
   */
  implicit def stringFormat: Formatter[String] = new Formatter[String] {
    def bind(key: String, data: Map[String, String]) = data.get(key).toRight(Seq(FormError(key, "error.required", Nil)))
    def unbind(key: String, value: String) = Map(key -> value)
  }

  /**
   * Default formatter for the `Char` type.
   */
  implicit def charFormat: Formatter[Char] = new Formatter[Char] {
    def bind(key: String, data: Map[String, String]) =
      data.get(key).filter(s => s.length == 1 && s != " ").map(s => Right(s.charAt(0))).getOrElse(
        Left(Seq(FormError(key, "error.required", Nil)))
      )
    def unbind(key: String, value: Char) = Map(key -> value.toString)
  }

  /**
   * Helper for formatters binders
   * @param parse Function parsing a String value into a T value, throwing an exception in case of failure
   * @param errArgs Error to set in case of parsing failure
   * @param key Key name of the field to parse
   * @param data Field data
   */
  def parsing[T](parse: String => T, errMsg: String, errArgs: Seq[Any])(key: String, data: Map[String, String]): Either[Seq[FormError], T] = {
    stringFormat.bind(key, data).right.flatMap { s =>
      scala.util.control.Exception.allCatch[T]
        .either(parse(s))
        .left.map(e => Seq(FormError(key, errMsg, errArgs)))
    }
  }

  private def numberFormatter[T](convert: String => T, real: Boolean = false): Formatter[T] = {
    val (formatString, errorString) = if (real) ("format.real", "error.real") else ("format.numeric", "error.number")
    new Formatter[T] {
      override val format = Some(formatString -> Nil)
      def bind(key: String, data: Map[String, String]) =
        parsing(convert, errorString, Nil)(key, data)
      def unbind(key: String, value: T) = Map(key -> value.toString)
    }
  }

  /**
   * Default formatter for the `Long` type.
   */
  implicit def longFormat: Formatter[Long] = numberFormatter(_.toLong)

  /**
   * Default formatter for the `Int` type.
   */
  implicit def intFormat: Formatter[Int] = numberFormatter(_.toInt)

  /**
   * Default formatter for the `Short` type.
   */
  implicit def shortFormat: Formatter[Short] = numberFormatter(_.toShort)

  /**
   * Default formatter for the `Byte` type.
   */
  implicit def byteFormat: Formatter[Byte] = numberFormatter(_.toByte)
  /**
   * Default formatter for the `Float` type.
   */
  implicit def floatFormat: Formatter[Float] = numberFormatter(_.toFloat, real = true)

  /**
   * Default formatter for the `Double` type.
   */
  implicit def doubleFormat: Formatter[Double] = numberFormatter(_.toDouble, real = true)

  /**
   * Default formatter for the `BigDecimal` type.
   */
  def bigDecimalFormat(precision: Option[(Int, Int)]): Formatter[BigDecimal] = new Formatter[BigDecimal] {

    override val format = Some(("format.real", Nil))

    def bind(key: String, data: Map[String, String]) = {
      Formats.stringFormat.bind(key, data).right.flatMap { s =>
        scala.util.control.Exception.allCatch[BigDecimal]
          .either {
            val bd = BigDecimal(s)
            precision.map({
              case (p, s) =>
                if (bd.precision - bd.scale > p - s) {
                  throw new java.lang.ArithmeticException("Invalid precision")
                }
                bd.setScale(s)
            }).getOrElse(bd)
          }
          .left.map { e =>
            Seq(
              precision match {
                case Some((p, s)) => FormError(key, "error.real.precision", Seq(p, s))
                case None => FormError(key, "error.real", Nil)
              }
            )
          }
      }
    }

    def unbind(key: String, value: BigDecimal) = Map(key -> precision.map({ p => value.setScale(p._2) }).getOrElse(value).toString)
  }

  /**
   * Default formatter for the `BigDecimal` type with no precision
   */
  implicit val bigDecimalFormat: Formatter[BigDecimal] = bigDecimalFormat(None)

  /**
   * Default formatter for the `Boolean` type.
   */
  implicit def booleanFormat: Formatter[Boolean] = new Formatter[Boolean] {

    override val format = Some(("format.boolean", Nil))

    def bind(key: String, data: Map[String, String]) = {
      Right(data.getOrElse(key, "false")).right.flatMap {
        case "true" => Right(true)
        case "false" => Right(false)
        case _ => Left(Seq(FormError(key, "error.boolean", Nil)))
      }
    }

    def unbind(key: String, value: Boolean) = Map(key -> value.toString)
  }

  import java.util.{ Date, TimeZone }

  /**
   * Formatter for the `java.util.Date` type.
   *
   * @param pattern a date pattern, as specified in `java.time.format.DateTimeFormatter`.
   * @param timeZone the `java.util.TimeZone` to use for parsing and formatting
   */
  def dateFormat(pattern: String, timeZone: TimeZone = TimeZone.getDefault): Formatter[Date] = new Formatter[Date] {
    val javaTimeZone = timeZone.toZoneId
    val formatter = DateTimeFormatter.ofPattern(pattern)

    def dateParse(data: String) = {
      val instant = PlayDate.parse(data, formatter).toZonedDateTime(ZoneOffset.UTC)
      Date.from(instant.withZoneSameLocal(javaTimeZone).toInstant)
    }

    override val format = Some(("format.date", Seq(pattern)))

    def bind(key: String, data: Map[String, String]) = parsing(dateParse, "error.date", Nil)(key, data)

    def unbind(key: String, value: Date) = Map(key -> formatter.format(value.toInstant.atZone(javaTimeZone)))
  }

  /**
   * Default formatter for the `java.util.Date` type with pattern `yyyy-MM-dd`.
   */
  implicit val dateFormat: Formatter[Date] = dateFormat("yyyy-MM-dd")

  @deprecated("Use sqlDateFormat(pattern). SQL dates do not have time zones.", "2.6.2")
  def sqlDateFormat(pattern: String, timeZone: java.util.TimeZone): Formatter[java.sql.Date] = sqlDateFormat(pattern)

  // Added for bincompat
  @deprecated("This method will be removed when sqlDateFormat(pattern, timeZone) is removed.", "2.6.2")
  private[format] def sqlDateFormat$default$2: java.util.TimeZone = java.util.TimeZone.getDefault

  /**
   * Formatter for the `java.sql.Date` type.
   *
   * @param pattern a date pattern as specified in `java.time.DateTimeFormatter`.
   */
  def sqlDateFormat(pattern: String): Formatter[java.sql.Date] = new Formatter[java.sql.Date] {

    private val dateFormatter: Formatter[LocalDate] = localDateFormat(pattern)

    override val format = Some(("format.date", Seq(pattern)))

    def bind(key: String, data: Map[String, String]) = {
      dateFormatter.bind(key, data).right.map(d => java.sql.Date.valueOf(d))
    }

    def unbind(key: String, value: java.sql.Date) = dateFormatter.unbind(key, value.toLocalDate)
  }

  /**
   * Default formatter for `java.sql.Date` type with pattern `yyyy-MM-dd`.
   */
  implicit val sqlDateFormat: Formatter[java.sql.Date] = sqlDateFormat("yyyy-MM-dd")

  /**
   * Formatter for the `java.sql.Timestamp` type.
   *
   * @param pattern a date pattern as specified in `java.time.DateTimeFormatter`.
   * @param timeZone the `java.util.TimeZone` to use for parsing and formatting
   */
  def sqlTimestampFormat(pattern: String, timeZone: TimeZone = TimeZone.getDefault): Formatter[java.sql.Timestamp] = new Formatter[java.sql.Timestamp] {

    import java.time.LocalDateTime

    private val formatter = java.time.format.DateTimeFormatter.ofPattern(pattern).withZone(timeZone.toZoneId)
    private def timestampParse(data: String) = java.sql.Timestamp.valueOf(LocalDateTime.parse(data, formatter))

    override val format = Some(("format.timestamp", Seq(pattern)))

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Timestamp] = parsing(timestampParse, "error.timestamp", Nil)(key, data)

    override def unbind(key: String, value: java.sql.Timestamp) = Map(key -> value.toLocalDateTime.format(formatter))
  }

  /**
   * Default formatter for `java.sql.Timestamp` type with pattern `yyyy-MM-dd HH:mm:ss`.
   */
  implicit val sqlTimestampFormat: Formatter[java.sql.Timestamp] = sqlTimestampFormat("yyyy-MM-dd HH:mm:ss")

  /**
   * Formatter for the `java.time.LocalDate` type.
   *
   * @param pattern a date pattern as specified in `java.time.format.DateTimeFormatter`.
   */
  def localDateFormat(pattern: String): Formatter[java.time.LocalDate] = new Formatter[java.time.LocalDate] {

    import java.time.LocalDate

    val formatter = java.time.format.DateTimeFormatter.ofPattern(pattern)
    def localDateParse(data: String) = LocalDate.parse(data, formatter)

    override val format = Some(("format.date", Seq(pattern)))

    def bind(key: String, data: Map[String, String]) = parsing(localDateParse, "error.date", Nil)(key, data)

    def unbind(key: String, value: LocalDate) = Map(key -> value.format(formatter))
  }

  /**
   * Default formatter for `java.time.LocalDate` type with pattern `yyyy-MM-dd`.
   */
  implicit val localDateFormat: Formatter[java.time.LocalDate] = localDateFormat("yyyy-MM-dd")

  /**
   * Formatter for the `java.time.LocalDateTime` type.
   *
   * @param pattern a date pattern as specified in `java.time.format.DateTimeFormatter`.
   * @param zoneId the `java.time.ZoneId` to use for parsing and formatting
   */
  def localDateTimeFormat(pattern: String, zoneId: java.time.ZoneId = java.time.ZoneId.systemDefault()): Formatter[java.time.LocalDateTime] = new Formatter[java.time.LocalDateTime] {

    import java.time.LocalDateTime

    val formatter = java.time.format.DateTimeFormatter.ofPattern(pattern).withZone(zoneId)
    def localDateTimeParse(data: String) = LocalDateTime.parse(data, formatter)

    override val format = Some(("format.localDateTime", Seq(pattern)))

    def bind(key: String, data: Map[String, String]) = parsing(localDateTimeParse, "error.localDateTime", Nil)(key, data)

    def unbind(key: String, value: LocalDateTime) = Map(key -> value.format(formatter))
  }

  /**
   * Default formatter for `java.time.LocalDateTime` type with pattern `yyyy-MM-dd`.
   */
  implicit val localDateTimeFormat: Formatter[java.time.LocalDateTime] = localDateTimeFormat("yyyy-MM-dd HH:mm:ss")

  /**
   * Formatter for the `java.time.LocalTime` type.
   *
   * @param pattern a date pattern as specified in `java.time.format.DateTimeFormatter`.
   */
  def localTimeFormat(pattern: String): Formatter[java.time.LocalTime] = new Formatter[java.time.LocalTime] {

    import java.time.LocalTime

    val formatter = java.time.format.DateTimeFormatter.ofPattern(pattern)
    def localTimeParse(data: String) = LocalTime.parse(data, formatter)

    override val format = Some(("format.localTime", Seq(pattern)))

    def bind(key: String, data: Map[String, String]) = parsing(localTimeParse, "error.localTime", Nil)(key, data)

    def unbind(key: String, value: LocalTime) = Map(key -> value.format(formatter))
  }

  /**
   * Default formatter for `java.time.LocalTime` type with pattern `HH:mm:ss`.
   */
  implicit val localTimeFormat: Formatter[java.time.LocalTime] = localTimeFormat("HH:mm:ss")

  /**
   * Default formatter for the `java.util.UUID` type.
   */
  implicit def uuidFormat: Formatter[UUID] = new Formatter[UUID] {

    override val format = Some(("format.uuid", Nil))

    override def bind(key: String, data: Map[String, String]) = parsing(UUID.fromString, "error.uuid", Nil)(key, data)

    override def unbind(key: String, value: UUID) = Map(key -> value.toString)
  }

}
