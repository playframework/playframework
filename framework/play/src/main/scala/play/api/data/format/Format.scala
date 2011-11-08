package play.api.data.format

import play.api.data._

/**
 * A Formatter handle field binding/unbinding.
 */
trait Formatter[T] {

  /**
   * The expected format of any.
   */
  val format: Option[(String, Seq[Any])] = None

  /**
   * Bind this field (ie. construct a concrete value from submitted data).
   *
   * @param key The field key.
   * @param data The submitted data.
   * @return Either a concrete value of type T or a set of error if the binding failed.
   */
  def bind(key: String, data: Map[String, String]): Either[Seq[FormError], T]

  /**
   * Unbind this field (ie. transform a concrete value to plain data)
   *
   * @param key The field key.
   * @param value The value to unbind.
   * @return Either the plain data or a set of error if the unbinding failed.
   */
  def unbind(key: String, value: T): Map[String, String]
}

/**
 * This object defines several default formatters.
 */
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
   * Default formatter for String type.
   */
  implicit def stringFormat = new Formatter[String] {
    def bind(key: String, data: Map[String, String]) = data.get(key).toRight(Seq(FormError(key, "error.required", Nil)))
    def unbind(key: String, value: String) = Map(key -> value)
  }

  /**
   * Default formatter for Long type.
   */
  implicit def longFormat = new Formatter[Long] {

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
   * Default formatter for Boolean type.
   */
  implicit def booleanFormat = new Formatter[Boolean] {

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
   * Formatter for java.util.Date type.
   *
   * @param pattern Date pattern as specified in Java SimpleDateFormat.
   */
  def dateFormat(pattern: String): Formatter[Date] = new Formatter[Date] {

    override val format = Some("format.date", Seq(pattern))

    def bind(key: String, data: Map[String, String]) = {
      stringFormat.bind(key, data).right.flatMap { s =>
        scala.util.control.Exception.allCatch[Date]
          .either(new SimpleDateFormat(pattern).parse(s))
          .left.map(e => Seq(FormError(key, "error.date", Nil)))
      }
    }

    def unbind(key: String, value: Date) = Map(key -> new SimpleDateFormat(pattern).format(value))
  }

  /**
   * Default formatter for java.util.Date type with pattern yyyy-MM-dd.
   *
   * @param pattern Date pattern as specified in Java SimpleDateFormat.
   */
  implicit val dateFormat: Formatter[Date] = dateFormat("yyyy-MM-dd")

  /**
   * Formatter for java.sql.Date type.
   *
   * @param pattern Date pattern as specified in Java SimpleDateFormat.
   */
  def sqlDateFormat(pattern: String): Formatter[java.sql.Date] = new Formatter[java.sql.Date] {

    override val format = Some("format.date", Seq(pattern))

    def bind(key: String, data: Map[String, String]) = {
      dateFormat(pattern).bind(key, data).right.map(d => new java.sql.Date(d.getTime))
    }

    def unbind(key: String, value: java.sql.Date) = dateFormat(pattern).unbind(key, value)

  }

  /**
   * Default formatter for java.sql.Date type with pattern yyyy-MM-dd.
   *
   * @param pattern Date pattern as specified in Java SimpleDateFormat.
   */
  implicit val sqlDateFormat: Formatter[java.sql.Date] = sqlDateFormat("yyyy-MM-dd")

}

