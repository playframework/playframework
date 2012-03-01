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
   * Default formatter for the `Long` type.
   */
  implicit def longFormat: Formatter[Long] = new Formatter[Long] {

    override val format = Some("format.numeric", Nil)

    def bind(key: String, data: Map[String, String]) = {
      stringFormat.bind(key, data).right.flatMap { s =>
        scala.util.control.Exception.allCatch[Long]
          .either(java.lang.Long.parseLong(s))
          .left.map(e => Seq(FormError(key, "error.number", Nil)))
      }
    }

    def unbind(key: String, value: Long) = Map(key -> value.toString)
  }

  /**
   * Default formatter for the `Int` type.
   */
  implicit def intFormat: Formatter[Int] = new Formatter[Int] {

    override val format = Some("format.numeric", Nil)

    def bind(key: String, data: Map[String, String]) = {
      stringFormat.bind(key, data).right.flatMap { s =>
        scala.util.control.Exception.allCatch[Int]
          .either(Integer.parseInt(s))
          .left.map(e => Seq(FormError(key, "error.number", Nil)))
      }
    }

    def unbind(key: String, value: Int) = Map(key -> value.toString)
  }

  /**
   * Default formatter for the `Boolean` type.
   */
  implicit def booleanFormat: Formatter[Boolean] = new Formatter[Boolean] {

    override val format = Some("format.boolean", Nil)

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

    override val format = Some("format.date", Seq(pattern))

    def bind(key: String, data: Map[String, String]) = {
      stringFormat.bind(key, data).right.flatMap { s =>
        scala.util.control.Exception.allCatch[Date]
          .either({
            val sdf = new SimpleDateFormat(pattern)
            sdf.setLenient(false)
            sdf.parse(s)
          }
          )
          .left.map(e => Seq(FormError(key, "error.date", Nil)))
      }
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

    override val format = Some("format.date", Seq(pattern))

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

}

