/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.data.format

import org.specs2.mutable.Specification
import java.util.{ UUID, Date, TimeZone }

import play.api.data._
import play.api.data.Forms._

class FormatSpec extends Specification {
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
