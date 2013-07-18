package play.api.data.format

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
   * Helper for formatters binders
   * @param parse Function parsing a String value into a T value, throwing an exception in case of failure
   * @param error Error to set in case of parsing failure
   * @param key Key name of the field to parse
   * @param data Field data
   */
  private def parsing[T](parse: String => T, errMsg: String, errArgs: Seq[Any])(key: String, data: Map[String, String]): Either[Seq[FormError], T] = {
    stringFormat.bind(key, data).right.flatMap { s =>
      scala.util.control.Exception.allCatch[T]
        .either(parse(s))
        .left.map(e => Seq(FormError(key, errMsg, errArgs)))
    }
  }

  /**
   * Default formatter for the `Long` type.
   */
  implicit def longFormat: Formatter[Long] = new Formatter[Long] {

    override val format = Some(("format.numeric", Nil))

    def bind(key: String, data: Map[String, String]) =
      parsing(_.toLong, "error.number", Nil)(key, data)

    def unbind(key: String, value: Long) = Map(key -> value.toString)
  }

  /**
   * Default formatter for the `Int` type.
   */
  implicit def intFormat: Formatter[Int] = new Formatter[Int] {

    override val format = Some(("format.numeric", Nil))

    def bind(key: String, data: Map[String, String]) =
      parsing(_.toInt, "error.number", Nil)(key, data)

    def unbind(key: String, value: Int) = Map(key -> value.toString)
  }

  /**
   * Default formatter for the `Float` type.
   */
  implicit def floatFormat: Formatter[Float] = new Formatter[Float] {

    override val format = Some(("format.real", Nil))

    def bind(key: String, data: Map[String, String]) =
      parsing(_.toFloat, "error.real", Nil)(key, data)

    def unbind(key: String, value: Float) = Map(key -> value.toString)
  }

  /**
   * Default formatter for the `Double` type.
   */
  implicit def doubleFormat: Formatter[Double] = new Formatter[Double] {

    override val format = Some(("format.real", Nil))

    def bind(key: String, data: Map[String, String]) =
      parsing(_.toDouble, "error.real", Nil)(key, data)

    def unbind(key: String, value: Double) = Map(key -> value.toString)
  }

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
      Right(data.get(key).getOrElse("false")).right.flatMap {
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
   * @param pattern a date pattern, as specified in `org.joda.time.format.DateTimeFormat`.
   * @param timeZone the `java.util.TimeZone` to use for parsing and formatting
   */
  def dateFormat(pattern: String, timeZone: TimeZone = TimeZone.getDefault): Formatter[Date] = new Formatter[Date] {

    val jodaTimeZone = org.joda.time.DateTimeZone.forTimeZone(timeZone)
    val formatter = org.joda.time.format.DateTimeFormat.forPattern(pattern).withZone(jodaTimeZone)
    def dateParse(data: String) = formatter.parseDateTime(data).toDate

    override val format = Some(("format.date", Seq(pattern)))

    def bind(key: String, data: Map[String, String]) = parsing(dateParse, "error.date", Nil)(key, data)

    def unbind(key: String, value: Date) = Map(key -> formatter.print(new org.joda.time.DateTime(value).withZone(jodaTimeZone)))
  }

  /**
   * Default formatter for the `java.util.Date` type with pattern `yyyy-MM-dd`.
   */
  implicit val dateFormat: Formatter[Date] = dateFormat("yyyy-MM-dd")

  /**
   * Formatter for the `java.sql.Date` type.
   *
   * @param pattern a date pattern as specified in `org.joda.time.format.DateTimeFormat`.
   * @param timeZone the `java.util.TimeZone` to use for parsing and formatting
   */
  def sqlDateFormat(pattern: String, timeZone: TimeZone = TimeZone.getDefault): Formatter[java.sql.Date] = new Formatter[java.sql.Date] {

    val dateFormatter = dateFormat(pattern, timeZone)

    override val format = Some(("format.date", Seq(pattern)))

    def bind(key: String, data: Map[String, String]) = {
      dateFormatter.bind(key, data).right.map(d => new java.sql.Date(d.getTime))
    }

    def unbind(key: String, value: java.sql.Date) = dateFormatter.unbind(key, value)
  }

  /**
   * Default formatter for `java.sql.Date` type with pattern `yyyy-MM-dd`.
   */
  implicit val sqlDateFormat: Formatter[java.sql.Date] = sqlDateFormat("yyyy-MM-dd")

  /**
   * Formatter for the `org.joda.time.DateTime` type.
   *
   * @param pattern a date pattern as specified in `org.joda.time.format.DateTimeFormat`.
   * @param timeZone the `org.joda.time.DateTimeZone` to use for parsing and formatting
   */
  def jodaDateTimeFormat(pattern: String, timeZone: org.joda.time.DateTimeZone = org.joda.time.DateTimeZone.getDefault): Formatter[org.joda.time.DateTime] = new Formatter[org.joda.time.DateTime] {

    val formatter = org.joda.time.format.DateTimeFormat.forPattern(pattern).withZone(timeZone)

    override val format = Some(("format.date", Seq(pattern)))

    def bind(key: String, data: Map[String, String]) = parsing(formatter.parseDateTime, "error.date", Nil)(key, data)

    def unbind(key: String, value: org.joda.time.DateTime) = Map(key -> value.withZone(timeZone).toString(pattern))
  }

  /**
   * Default formatter for `org.joda.time.DateTime` type with pattern `yyyy-MM-dd`.
   *
   * @param pattern a date pattern as specified in `org.joda.time.format.DateTimeFormat`.
   */
  implicit val jodaDateTimeFormat: Formatter[org.joda.time.DateTime] = jodaDateTimeFormat("yyyy-MM-dd")

  /**
   * Formatter for the `org.joda.time.LocalDate` type.
   *
   * @param pattern a date pattern as specified in `org.joda.time.format.DateTimeFormat`.
   */
  def jodaLocalDateFormat(pattern: String): Formatter[org.joda.time.LocalDate] = new Formatter[org.joda.time.LocalDate] {

    import org.joda.time.LocalDate

    val formatter = org.joda.time.format.DateTimeFormat.forPattern(pattern)
    def jodaLocalDateParse(data: String) = LocalDate.parse(data, formatter)

    override val format = Some(("format.date", Seq(pattern)))

    def bind(key: String, data: Map[String, String]) = parsing(jodaLocalDateParse, "error.date", Nil)(key, data)

    def unbind(key: String, value: LocalDate) = Map(key -> value.toString(pattern))
  }

  /**
   * Default formatter for `org.joda.time.LocalDate` type with pattern `yyyy-MM-dd`.
   *
   * @param pattern a date pattern as specified in `org.joda.time.format.DateTimeFormat`.
   */
  implicit val jodaLocalDateFormat: Formatter[org.joda.time.LocalDate] = jodaLocalDateFormat("yyyy-MM-dd")

}
