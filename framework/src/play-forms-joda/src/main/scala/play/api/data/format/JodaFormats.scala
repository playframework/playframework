/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.data.format

import play.api.data._

object JodaFormats {

  /**
   * Helper for formatters binders
   * @param parse Function parsing a String value into a T value, throwing an exception in case of failure
   * @param errArgs Error to set in case of parsing failure
   * @param key Key name of the field to parse
   * @param data Field data
   */
  private def parsing[T](parse: String => T, errMsg: String, errArgs: Seq[Any])(key: String, data: Map[String, String]): Either[Seq[FormError], T] = {
    Formats.stringFormat.bind(key, data).right.flatMap { s =>
      scala.util.control.Exception.allCatch[T]
        .either(parse(s))
        .left.map(e => Seq(FormError(key, errMsg, errArgs)))
    }
  }

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
   */
  implicit val jodaLocalDateFormat: Formatter[org.joda.time.LocalDate] = jodaLocalDateFormat("yyyy-MM-dd")

}
