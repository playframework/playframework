package play.api.data.validation

import org.specs2.mutable._

import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.data.validation.Constraints._

object ValidationSpec extends Specification {

  "Min and max constraint on an Int" should {
    "5 must be a valid number(1,10)" in {
      Form( "value" -> number(1,10) ).bind( Map( "value" -> "5") ).fold(
        formWithErrors => { "The mapping should not fail." must equalTo("Error") },
        { number => number must equalTo(5) }
      )
    }

    "15 must not be a valid number(1,10)" in {
      Form( "value" -> number(1,10) ).bind( Map( "value" -> "15") ).fold(
        formWithErrors => { formWithErrors.errors.head.message must equalTo("error.max") },
        { number => "The mapping should fail." must equalTo("Error") }
      )
    }
  }

  "Min and max constraint on a Long" should {
    "12345678902 must be a valid longNumber(1,10)" in {
      Form( "value" -> longNumber(1,123456789023L) ).bind( Map( "value" -> "12345678902") ).fold(
        formWithErrors => { "The mapping should not fail." must equalTo("Error") },
        { number => number must equalTo(12345678902L) }
      )
    }

    "-12345678902 must not be a valid longNumber(1,10)" in {
      Form( "value" -> longNumber(1,10) ).bind( Map( "value" -> "-12345678902") ).fold(
        formWithErrors => { formWithErrors.errors.head.message must equalTo("error.min") },
        { number => "The mapping should fail." must equalTo("Error") }
      )
    }
  }

  "Min constraint should now work on a String" should {
    "Toto must be over CC" in {
      Form( "value" -> ( nonEmptyText verifying min("CC") ) ).bind( Map( "value" -> "Toto") ).fold(
        formWithErrors => { "The mapping should not fail." must equalTo("Error") },
        { str => str must equalTo("Toto") }
      )
    }

    "AA must not be over CC" in {
      Form( "value" -> ( nonEmptyText verifying min("CC") ) ).bind( Map( "value" -> "AA") ).fold(
        formWithErrors => { formWithErrors.errors.head.message must equalTo("error.min") },
        { str => "The mapping should fail." must equalTo("Error") }
      )
    }
  }

  "Max constraint should now work on a Double" should {
    "10.2 must be under 100.1" in {
      Form( "value" -> ( of[Double] verifying max(100.1) ) ).bind( Map( "value" -> "10.2") ).fold(
        formWithErrors => { "The mapping should not fail." must equalTo("Error") },
        { number => number must equalTo(10.2) }
      )
    }

    "110.3 must not be over 100.1" in {
      Form( "value" -> ( of[Double] verifying max(100.1) ) ).bind( Map( "value" -> "110.3") ).fold(
        formWithErrors => { formWithErrors.errors.head.message must equalTo("error.max") },
        { number => "The mapping should fail." must equalTo("Error") }
      )
    }
  }

  "Min and max can now be strict" should {
    "5 must be a valid number(1,10, strict = true)" in {
      Form( "value" -> number(1,10, strict = true) ).bind( Map( "value" -> "5") ).fold(
        formWithErrors => { "The mapping should not fail." must equalTo("Error") },
        { number => number must equalTo(5) }
      )
    }

    "5 must still be a valid number(5,10)" in {
      Form( "value" -> number(5,10) ).bind( Map( "value" -> "5") ).fold(
        formWithErrors => { "The mapping should not fail." must equalTo("Error") },
        { number => number must equalTo(5) }
      )
    }

    "5 must not be a valid number(5,10, strict = true)" in {
      Form( "value" -> number(5,10, strict = true) ).bind( Map( "value" -> "5") ).fold(
        formWithErrors => { formWithErrors.errors.head.message must equalTo("error.min.strict") },
        { number => "The mapping should fail." must equalTo("Error") }
      )
    }

    "Text containing whitespace only should be rejected by nonEmptyText" in {
      Form( "value" -> nonEmptyText ).bind( Map( "value" -> " ") ).fold(
        formWithErrors => { formWithErrors.errors.head.message must equalTo("error.required") },
        { text => "The mapping should fail." must equalTo("Error") }
      )
    }
  }

}