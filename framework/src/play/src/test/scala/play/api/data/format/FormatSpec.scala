/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.data.format

import java.sql
import java.sql.Timestamp
import java.time.{ LocalDate, LocalDateTime }

import org.specs2.mutable.Specification
import java.util.{ Date, TimeZone, UUID }

import play.api.data._
import play.api.data.Forms._

class FormatSpec extends Specification {

  "A java.sql.Date format" should {
    "support formatting with a pattern" in {
      val data = Map("date" -> "04-07-2017")
      val format = Formats.sqlDateFormat("dd-MM-yyyy")
      val bindResult = format.bind("date", data)

      bindResult.right.map(_.toLocalDate.getDayOfMonth) should beRight(4)
      bindResult.right.map(_.toLocalDate.getMonth) should beRight(java.time.Month.JULY)
      bindResult.right.map(_.toLocalDate.getYear) should beRight(2017)
    }

    "use yyyy-MM-dd as the default format" in {
      val data = Map("date" -> "2017-07-04")
      val format = Formats.sqlDateFormat
      val bindResult = format.bind("date", data)

      bindResult.right.map(_.toLocalDate.getDayOfMonth) should beRight(4)
      bindResult.right.map(_.toLocalDate.getMonth) should beRight(java.time.Month.JULY)
      bindResult.right.map(_.toLocalDate.getYear) should beRight(2017)
    }

    "fails when form data is using the wrong pattern" in {
      val data = Map("date" -> "04-07-2017") // default pattern is yyyy-MM-dd, so this is wrong
      val format = Formats.sqlDateFormat

      format.bind("date", data) should beLeft
    }

    "fails with the correct message key when using the wrong pattern" in {
      val data = Map("date" -> "04-07-2017") // default pattern is yyyy-MM-dd, so this is wrong
      val format = Formats.sqlDateFormat

      format.bind("date", data) should beLeft.which(_.exists(_.message.equals("error.date")))
    }

    "convert raw data to form data using the given pattern" in {
      val format = Formats.sqlDateFormat("dd-MM-yyyy")
      val localDate = LocalDate.of(2017, java.time.Month.JULY, 4)
      format.unbind("date", java.sql.Date.valueOf(localDate)).get("date") must beSome("04-07-2017")
    }

    "convert raw data to form data using the default pattern" in {
      val format = Formats.sqlDateFormat
      val localDate = LocalDate.of(2017, java.time.Month.JULY, 4)
      format.unbind("date", java.sql.Date.valueOf(localDate)).get("date") must beSome("2017-07-04")
    }
  }

  "A java.sql.Timestamp format" should {
    "support formatting with a pattern" in {
      val data = Map("date" -> "04-07-2017 10:11:12")
      val format = Formats.sqlTimestampFormat("dd-MM-yyyy HH:mm:ss")
      val bindResult = format.bind("date", data)

      bindResult.right.map(_.toLocalDateTime.getDayOfMonth) should beRight(4)
      bindResult.right.map(_.toLocalDateTime.getMonth) should beRight(java.time.Month.JULY)
      bindResult.right.map(_.toLocalDateTime.getYear) should beRight(2017)
      bindResult.right.map(_.toLocalDateTime.getHour) should beRight(10)
      bindResult.right.map(_.toLocalDateTime.getMinute) should beRight(11)
      bindResult.right.map(_.toLocalDateTime.getSecond) should beRight(12)
    }

    "use yyyy-MM-dd HH:ss:mm as the default format" in {
      val data = Map("date" -> "2017-07-04 10:11:12")
      val format = Formats.sqlTimestampFormat
      val bindResult = format.bind("date", data)

      bindResult.right.map(_.toLocalDateTime.getDayOfMonth) should beRight(4)
      bindResult.right.map(_.toLocalDateTime.getMonth) should beRight(java.time.Month.JULY)
      bindResult.right.map(_.toLocalDateTime.getYear) should beRight(2017)
      bindResult.right.map(_.toLocalDateTime.getHour) should beRight(10)
      bindResult.right.map(_.toLocalDateTime.getMinute) should beRight(11)
      bindResult.right.map(_.toLocalDateTime.getSecond) should beRight(12)
    }

    "fails when form data is using the wrong pattern" in {
      val data = Map("date" -> "04-07-2017") // default pattern is yyyy-MM-dd, so this is wrong
      val format = Formats.sqlTimestampFormat

      format.bind("date", data) should beLeft
    }

    "fails with the correct message key when using the wrong pattern" in {
      val data = Map("date" -> "04-07-2017") // default pattern is yyyy-MM-dd, so this is wrong
      val format = Formats.sqlTimestampFormat

      format.bind("date", data) should beLeft.which(_.exists(_.message.equals("error.timestamp")))
    }

    "convert raw data to form data using the given pattern" in {
      val format = Formats.sqlTimestampFormat("dd-MM-yyyy HH:mm:ss")
      val localDateTime = LocalDateTime.of(2017, java.time.Month.JULY, 4, 10, 11, 12)
      format.unbind("date", Timestamp.valueOf(localDateTime)).get("date") must beSome("04-07-2017 10:11:12")
    }

    "convert raw data to form data using the default pattern" in {
      val format = Formats.sqlTimestampFormat
      val localDateTime = LocalDateTime.of(2017, java.time.Month.JULY, 4, 10, 11, 12)
      format.unbind("date", java.sql.Timestamp.valueOf(localDateTime)).get("date") must beSome("2017-07-04 10:11:12")
    }
  }

  "dateFormat" should {
    "support custom time zones" in {
      val data = Map("date" -> "00:00")

      val format = Formats.dateFormat("HH:mm", TimeZone.getTimeZone("America/Los_Angeles"))
      format.bind("date", data).right.map(_.getTime) should beRight(28800000L)
      format.unbind("date", new Date(28800000L)) should equalTo(data)

      val format2 = Formats.dateFormat("HH:mm", TimeZone.getTimeZone("GMT+0000"))
      format2.bind("date", data).right.map(_.getTime) should beRight(0L)
      format2.unbind("date", new Date(0L)) should equalTo(data)
    }
  }

  "java.time Types" should {
    import java.time.LocalDateTime
    "support LocalDateTime formatting with a pattern" in {
      val pattern = "yyyy/MM/dd HH:mm:ss"
      val data = Map("localDateTime" -> "2016/06/06 00:30:30")

      val format = Formats.localDateTimeFormat(pattern)
      val bind: Either[Seq[FormError], LocalDateTime] = format.bind("localDateTime", data)
      bind.right.map(dt => {
        (dt.getYear, dt.getMonthValue, dt.getDayOfMonth, dt.getHour, dt.getMinute, dt.getSecond)
      }) should beRight((2016, 6, 6, 0, 30, 30))
    }

    "support LocalDateTime formatting with default pattern" in {
      val data = Map("localDateTime" -> "2016-10-10 11:11:11")
      val format = Formats.localDateTimeFormat
      format.bind("localDateTime", data).right.map { dt =>
        (dt.getYear, dt.getMonthValue, dt.getDayOfMonth, dt.getHour, dt.getMinute, dt.getSecond)
      } should beRight((2016, 10, 10, 11, 11, 11))
    }
  }

  "A simple mapping of BigDecimalFormat" should {
    "return a BigDecimal" in {
      Form("value" -> bigDecimal).bind(Map("value" -> "10.23")).fold(
        formWithErrors => { "The mapping should not fail." must equalTo("Error") },
        { number => number must equalTo(BigDecimal("10.23")) }
      )
    }
  }

  "A complex mapping of BigDecimalFormat" should {
    "12.23 must be a valid bigDecimal(10,2)" in {
      Form("value" -> bigDecimal(10, 2)).bind(Map("value" -> "10.23")).fold(
        formWithErrors => { "The mapping should not fail." must equalTo("Error") },
        { number => number must equalTo(BigDecimal("10.23")) }
      )
    }

    "12.23 must not be a valid bigDecimal(10,1) : Too many decimals" in {
      Form("value" -> bigDecimal(10, 1)).bind(Map("value" -> "10.23")).fold(
        formWithErrors => { formWithErrors.errors.head.message must equalTo("error.real.precision") },
        { number => "The mapping should fail." must equalTo("Error") }
      )
    }

    "12111.23 must not be a valid bigDecimal(5,2) : Too many digits" in {
      Form("value" -> bigDecimal(5, 2)).bind(Map("value" -> "12111.23")).fold(
        formWithErrors => { formWithErrors.errors.head.message must equalTo("error.real.precision") },
        { number => "The mapping should fail." must equalTo("Error") }
      )
    }
  }

  "A UUID mapping" should {

    "return a proper UUID when given one" in {

      val testUUID = UUID.randomUUID()

      Form("value" -> uuid).bind(Map("value" -> testUUID.toString)).fold(
        formWithErrors => { "The mapping should not fail." must equalTo("Error") },
        { uuid => uuid must equalTo(testUUID) }
      )
    }

    "give an error when an invalid UUID is passed in" in {

      Form("value" -> uuid).bind(Map("value" -> "Joe")).fold(
        formWithErrors => { formWithErrors.errors.head.message must equalTo("error.uuid") },
        { uuid => uuid must equalTo(UUID.randomUUID()) }
      )
    }
  }

  "A char mapping" should {

    "return a proper Char when given one" in {

      val testChar = 'M'

      Form("value" -> char).bind(Map("value" -> testChar.toString)).fold(
        formWithErrors => { "The mapping should not fail." must equalTo("Error") },
        { char => char must equalTo(testChar) }
      )
    }

    "give an error when an empty string is passed in" in {

      Form("value" -> char).bind(Map("value" -> " ")).fold(
        formWithErrors => { formWithErrors.errors.head.message must equalTo("error.required") },
        { char => char must equalTo('X') }
      )
    }
  }

  "String parsing utility function" should {

    val errorMessage = "error.parsing"

    def parsingFunction[T](fu: String => T) = Formats.parsing(fu, errorMessage, Nil) _

    val intParse: String => Int = Integer.parseInt

    val testField = "field"
    val testNumber = 1234

    "parse an integer from a string" in {
      parsingFunction(intParse)(testField, Map(testField -> testNumber.toString)).fold(
        errors => "The parsing should not fail" must equalTo("Error"),
        parsedInt => parsedInt mustEqual testNumber
      )
    }

    "register a field error if string not parseable into an Int" in {
      parsingFunction(intParse)(testField, Map(testField -> "notParseable")).fold(
        errors => errors should containTheSameElementsAs(Seq(FormError(testField, errorMessage))),
        parsedInt => "The parsing should fail" must equalTo("Error")
      )
    }

    "register a field error if unexpected exception encountered during parsing" in {
      parsingFunction(_ => throw new AssertionError)(testField, Map(testField -> testNumber.toString)).fold(
        errors => errors should containTheSameElementsAs(Seq(FormError(testField, errorMessage))),
        parsedInt => "The parsing should fail" must equalTo("Error")
      )
    }

  }

}
