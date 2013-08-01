package play.api.data.format

import org.specs2.mutable.Specification
import java.util.{Date, TimeZone}

import play.api.data._
import play.api.data.Forms._

object FormatSpec extends Specification {
  "dateFormat" should {
    "support custom time zones" in {
      val data = Map("date" -> "00:00")

      val format = Formats.dateFormat("HH:mm", TimeZone.getTimeZone("America/Los_Angeles"))
      format.bind("date", data).right.map(_.getTime) should beRight (28800000L)
      format.unbind("date", new Date(28800000L)) should equalTo (data)

      val format2 = Formats.dateFormat("HH:mm", TimeZone.getTimeZone("GMT+0000"))
      format2.bind("date", data).right.map(_.getTime) should beRight (0L)
      format2.unbind("date", new Date(0L)) should equalTo (data)
    }
  }

  "A simple mapping of BigDecimalFormat" should {
    "return a BigDecimal" in {
      Form( "value" -> bigDecimal ).bind( Map( "value" -> "10.23") ).fold(
        formWithErrors => { "The mapping should not fail." must equalTo("Error") },
        { number => number must equalTo(BigDecimal("10.23")) }
      )
    }
  }

  "A complex mapping of BigDecimalFormat" should {
    "12.23 must be a valid bigDecimal(10,2)" in {
      Form( "value" -> bigDecimal(10,2) ).bind( Map( "value" -> "10.23") ).fold(
        formWithErrors => { "The mapping should not fail." must equalTo("Error") },
        { number => number must equalTo(BigDecimal("10.23")) }
      )
    }

    "12.23 must not be a valid bigDecimal(10,1) : Too many decimals" in {
      Form( "value" -> bigDecimal(10,1) ).bind( Map( "value" -> "10.23") ).fold(
        formWithErrors => { formWithErrors.errors.head.message must equalTo("error.real.precision") },
        { number => "The mapping should fail." must equalTo("Error") }
      )
    }

    "12111.23 must not be a valid bigDecimal(5,2) : Too many digits" in {
      Form( "value" -> bigDecimal(5,2) ).bind( Map( "value" -> "12111.23") ).fold(
        formWithErrors => { formWithErrors.errors.head.message must equalTo("error.real.precision") },
        { number => "The mapping should fail." must equalTo("Error") }
      )
    }
  }

}