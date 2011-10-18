package play.api.data.format

import play.api.data._

trait Formatter[T] {
  val format: Option[(String, Seq[Any])] = None
  def bind(key: String, data: Map[String, String]): Either[Seq[FormError], T]
  def unbind(key: String, value: T): Map[String, String]
}

object Formats {

  implicit def stringFormat = new Formatter[String] {
    def bind(key: String, data: Map[String, String]) = data.get(key).toRight(Seq(FormError(key, "error.required", Nil)))
    def unbind(key: String, value: String) = Map(key -> value)
  }

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

  implicit val dateFormat: Formatter[Date] = dateFormat("yyyy-MM-dd")

  def sqlDateFormat(pattern: String): Formatter[java.sql.Date] = new Formatter[java.sql.Date] {

    override val format = Some("format.date", Seq(pattern))

    def bind(key: String, data: Map[String, String]) = {
      dateFormat(pattern).bind(key, data).right.map(d => new java.sql.Date(d.getTime))
    }

    def unbind(key: String, value: java.sql.Date) = dateFormat(pattern).unbind(key, value)

  }

  implicit val sqlDateFormat: Formatter[java.sql.Date] = sqlDateFormat("yyyy-MM-dd")

}

