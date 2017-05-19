/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.data

import play.api.data.format._

object JodaForms {

  import JodaFormats._

  /**
   * Constructs a simple mapping for a date field (mapped as `org.joda.time.DateTime type`).
   *
   * For example:
   * {{{
   *   Form("birthdate" -> jodaDate)
   * }}}
   */
  val jodaDate: Mapping[org.joda.time.DateTime] = Forms.of[org.joda.time.DateTime]

  /**
   * Constructs a simple mapping for a date field (mapped as `org.joda.time.DateTime type`).
   *
   * For example:
   * {{{
   *   Form("birthdate" -> jodaDate("dd-MM-yyyy"))
   * }}}
   *
   * @param pattern the date pattern, as defined in `org.joda.time.format.DateTimeFormat`
   * @param timeZone the `org.joda.time.DateTimeZone` to use for parsing and formatting
   */
  def jodaDate(pattern: String, timeZone: org.joda.time.DateTimeZone = org.joda.time.DateTimeZone.getDefault): Mapping[org.joda.time.DateTime] = Forms.of[org.joda.time.DateTime] as jodaDateTimeFormat(pattern, timeZone)

  /**
   * Constructs a simple mapping for a date field (mapped as `org.joda.time.LocalDatetype`).
   *
   * For example:
   * {{{
   * Form("birthdate" -> jodaLocalDate)
   * }}}
   */
  val jodaLocalDate: Mapping[org.joda.time.LocalDate] = Forms.of[org.joda.time.LocalDate]

  /**
   * Constructs a simple mapping for a date field (mapped as `org.joda.time.LocalDate type`).
   *
   * For example:
   * {{{
   * Form("birthdate" -> jodaLocalDate("dd-MM-yyyy"))
   * }}}
   *
   * @param pattern the date pattern, as defined in `org.joda.time.format.DateTimeFormat`
   */
  def jodaLocalDate(pattern: String): Mapping[org.joda.time.LocalDate] = Forms.of[org.joda.time.LocalDate] as jodaLocalDateFormat(pattern)

}
