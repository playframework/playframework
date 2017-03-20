package play.utils

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}
import scala.reflect.ClassTag
import scala.util.Try


package object string {
  implicit class RichString(str: String) {

    def toOption: Option[String] = if (str.nonEmpty) Some(str) else None

    def toOption[T: ClassTag]: Option[T] = Try(str.asInstanceOf[T]).toOption

    def extractValue[T](f: String => T): Option[T] = Try(f(str)).toOption

    def toByteOption: Option[Byte] = extractValue(_.toByte)

    def toShortOption: Option[Short] = extractValue(_.toShort)

    def toCharOption: Option[Char] = extractValue(_.toCharArray.head)

    def toCharArrayOption: Option[Array[Char]] = extractValue(_.toCharArray)

    def toIntOption: Option[Int] = extractValue(_.toInt)

    def toLongOption: Option[Long] = extractValue(_.toLong)

    def toFloatOption: Option[Float] = extractValue(_.toFloat)

    def toDoubleOption: Option[Double] = extractValue(_.toDouble)

    def toBooleanOption: Option[Boolean] = extractValue(_.toBoolean) match {
      case None => str.toIntOption match {
        case Some(0) => Some(false)
        case Some(1) => Some(true)
        case _ => None
      }
      case v => v
    }

    def toLocalDate(format: String): LocalDate =
      LocalDate.parse(str, DateTimeFormatter.ofPattern(format))

    def toLocalDateOption(format: String): Option[LocalDate] =
      extractValue(LocalDate.parse(_, DateTimeFormatter.ofPattern(format)))

    def toLocalDateTime(format: String): LocalDateTime =
      LocalDateTime.parse(str, DateTimeFormatter.ofPattern(format))

    def toLocalDateTimeOption(format: String): Option[LocalDateTime] =
      extractValue(LocalDateTime.parse(_, DateTimeFormatter.ofPattern(format)))
  }
}
