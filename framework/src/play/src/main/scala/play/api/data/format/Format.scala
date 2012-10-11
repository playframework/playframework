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
      util.control.Exception.allCatch[T]
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

  import java.util.Date
  import java.text.SimpleDateFormat

  /**
   * Formatter for the `java.util.Date` type.
   *
   * @param pattern a date pattern, as specified in `java.text.SimpleDateFormat`.
   */
  def dateFormat(pattern: String): Formatter[Date] = new Formatter[Date] {

    override val format = Some(("format.date", Seq(pattern)))

    def bind(key: String, data: Map[String, String]) = {
      def dateParser = { s: String =>
        val sdf = new SimpleDateFormat(pattern)
        sdf.setLenient(false)
        sdf.parse(s)
      }
      parsing(dateParser, "error.date", Nil)(key, data)
    }

    def unbind(key: String, value: Date) = Map(key -> new SimpleDateFormat(pattern).format(value))
  }

  /**
   * Default formatter for the `java.util.Date` type with pattern `yyyy-MM-dd`.
   *
   * @param pattern a date pattern as specified in `java.text.SimpleDateFormat`.
   */
  implicit val dateFormat: Formatter[Date] = dateFormat("yyyy-MM-dd")

  /**
   * Formatter for the `java.sql.Date` type.
   *
   * @param pattern a date pattern as specified in `java.text.SimpleDateFormat`.
   */
  def sqlDateFormat(pattern: String): Formatter[java.sql.Date] = new Formatter[java.sql.Date] {

    override val format = Some(("format.date", Seq(pattern)))

    def bind(key: String, data: Map[String, String]) = {
      dateFormat(pattern).bind(key, data).right.map(d => new java.sql.Date(d.getTime))
    }

    def unbind(key: String, value: java.sql.Date) = dateFormat(pattern).unbind(key, value)

  }

  /**
   * Default formatter for `java.sql.Date` type with pattern `yyyy-MM-dd`.
   *
   * @param pattern a date pattern as specified in `java.text.SimpleDateFormat`.
   */
  implicit val sqlDateFormat: Formatter[java.sql.Date] = sqlDateFormat("yyyy-MM-dd")

  /**
   * Formatter for the `org.joda.time.DateTime` type.
   *
   * @param pattern a date pattern as specified in `org.joda.time.format.DateTimeFormat`.
   */
  def jodaDateTimeFormat(pattern: String): Formatter[org.joda.time.DateTime] = new Formatter[org.joda.time.DateTime] {

    import org.joda.time.DateTime

    override val format = Some(("format.date", Seq(pattern)))

    def bind(key: String, data: Map[String, String]) = {

      stringFormat.bind(key, data).right.flatMap { s =>
        scala.util.control.Exception.allCatch[DateTime]
          .either(DateTime.parse(s, org.joda.time.format.DateTimeFormat.forPattern(pattern)))
          .left.map(e => Seq(FormError(key, "error.date", Nil)))
      }
    }

    def unbind(key: String, value: DateTime) = Map(key -> value.toString(pattern))
  }

  /**
   * Default formatter for `org.joda.time.DateTime` type with pattern `yyyy-MM-dd`.
   *
   * @param pattern a date pattern as specified in `org.joda.time.format.DateTimeFormat`.
   */
  implicit val jodaDateTimeFormat: Formatter[org.joda.time.DateTime] = jodaDateTimeFormat("yyyy-MM-dd")

}

