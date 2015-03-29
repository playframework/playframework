/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.data.validation

import org.specs2.mutable._

import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.data.validation.Constraints._

object ValidationSpec extends Specification {

  "text" should {
    "throw an IllegalArgumentException if maxLength is negative" in {
      {
        Form(
          "value" -> Forms.text(maxLength = -1)
        ).bind(Map("value" -> "hello"))
      }.must(throwAn[IllegalArgumentException])
    }

    "return a bound form with error if input is null, even if maxLength=0 " in {
      Form("value" -> Forms.text(maxLength = 0)).bind(Map("value" -> null)).fold(
        formWithErrors => { formWithErrors.errors.head.message must equalTo("error.maxLength") },
        { textData => "The mapping should fail." must equalTo("Error") }
      )
    }

    "throw an IllegalArgumentException if minLength is negative" in {
      {
        Form(
          "value" -> Forms.text(minLength = -1)
        ).bind(Map("value" -> "hello"))
      }.must(throwAn[IllegalArgumentException])
    }

    "return a bound form with error if input is null, even if minLength=0" in {
      Form("value" -> Forms.text(minLength = 0)).bind(Map("value" -> null)).fold(
        formWithErrors => { formWithErrors.errors.head.message must equalTo("error.minLength") },
        { textData => "The mapping should fail." must equalTo("Error") }
      )
    }
  }

  "nonEmptyText" should {
    "return a bound form with error if input is null" in {
      Form("value" -> nonEmptyText).bind(Map("value" -> null)).fold(
        formWithErrors => { formWithErrors.errors.head.message must equalTo("error.required") },
        { textData => "The mapping should fail." must equalTo("Error") }
      )
    }
  }

  "Constraints.pattern" should {
    "throw an IllegalArgumentException if regex is null" in {
      {
        Form(
          "value" -> Forms.text.verifying(Constraints.pattern(null, "nullRegex", "error"))
        ).bind(Map("value" -> "hello"))
      }.must(throwAn[IllegalArgumentException])
    }

    "throw an IllegalArgumentException if name is null" in {
      {
        Form(
          "value" -> Forms.text.verifying(Constraints.pattern(".*".r, null, "error"))
        ).bind(Map("value" -> "hello"))
      }.must(throwAn[IllegalArgumentException])
    }

    "throw an IllegalArgumentException if error is null" in {
      {
        Form(
          "value" -> Forms.text.verifying(pattern(".*".r, "nullRegex", null))
        ).bind(Map("value" -> "hello"))
      }.must(throwAn[IllegalArgumentException])
    }

  }

  "Email constraint" should {
    val valid = Seq(
      """simple@example.com""",
      """customer/department=shipping@example.com""",
      """$A12345@example.com""",
      """!def!xyz%abc@example.com""",
      """_somename@example.com""",
      """Ken.O'Brian@company.com"""
    )
    "validate valid addresses" in {
      valid.map { addr =>
        Form("value" -> email).bind(Map("value" -> addr)).fold(
          formWithErrors => false,
          { _ => true }
        )
      }.exists(_.unary_!) must beFalse
    }

    val invalid = Seq(
      "NotAnEmail",
      "@NotAnEmail",
      "\"\"test\blah\"\"@example.com",
      "\"test\rblah\"@example.com",
      "\"\"test\"\"blah\"\"@example.com",
      "Ima Fool@example.com"
    )
    "invalidate invalid addresses" in {
      invalid.map { addr =>
        Form("value" -> email).bind(Map("value" -> addr)).fold(
          formWithErrors => true,
          { _ => false }
        )
      }.exists(_.unary_!) must beFalse
    }
  }

  "Min and max constraint on an Int" should {
    "5 must be a valid number(1,10)" in {
      Form("value" -> number(1, 10)).bind(Map("value" -> "5")).fold(
        formWithErrors => { "The mapping should not fail." must equalTo("Error") },
        { number => number must equalTo(5) }
      )
    }

    "15 must not be a valid number(1,10)" in {
      Form("value" -> number(1, 10)).bind(Map("value" -> "15")).fold(
        formWithErrors => { formWithErrors.errors.head.message must equalTo("error.max") },
        { number => "The mapping should fail." must equalTo("Error") }
      )
    }
  }

  "Min and max constraint on a Long" should {
    "12345678902 must be a valid longNumber(1,10)" in {
      Form("value" -> longNumber(1, 123456789023L)).bind(Map("value" -> "12345678902")).fold(
        formWithErrors => { "The mapping should not fail." must equalTo("Error") },
        { number => number must equalTo(12345678902L) }
      )
    }

    "-12345678902 must not be a valid longNumber(1,10)" in {
      Form("value" -> longNumber(1, 10)).bind(Map("value" -> "-12345678902")).fold(
        formWithErrors => { formWithErrors.errors.head.message must equalTo("error.min") },
        { number => "The mapping should fail." must equalTo("Error") }
      )
    }
  }

  "Min constraint should now work on a String" should {
    "Toto must be over CC" in {
      Form("value" -> (nonEmptyText verifying min("CC"))).bind(Map("value" -> "Toto")).fold(
        formWithErrors => { "The mapping should not fail." must equalTo("Error") },
        { str => str must equalTo("Toto") }
      )
    }

    "AA must not be over CC" in {
      Form("value" -> (nonEmptyText verifying min("CC"))).bind(Map("value" -> "AA")).fold(
        formWithErrors => { formWithErrors.errors.head.message must equalTo("error.min") },
        { str => "The mapping should fail." must equalTo("Error") }
      )
    }
  }

  "Max constraint should now work on a Double" should {
    "10.2 must be under 100.1" in {
      Form("value" -> (of[Double] verifying max(100.1))).bind(Map("value" -> "10.2")).fold(
        formWithErrors => { "The mapping should not fail." must equalTo("Error") },
        { number => number must equalTo(10.2) }
      )
    }

    "110.3 must not be over 100.1" in {
      Form("value" -> (of[Double] verifying max(100.1))).bind(Map("value" -> "110.3")).fold(
        formWithErrors => { formWithErrors.errors.head.message must equalTo("error.max") },
        { number => "The mapping should fail." must equalTo("Error") }
      )
    }
  }

  "Min and max can now be strict" should {
    "5 must be a valid number(1,10, strict = true)" in {
      Form("value" -> number(1, 10, strict = true)).bind(Map("value" -> "5")).fold(
        formWithErrors => { "The mapping should not fail." must equalTo("Error") },
        { number => number must equalTo(5) }
      )
    }

    "5 must still be a valid number(5,10)" in {
      Form("value" -> number(5, 10)).bind(Map("value" -> "5")).fold(
        formWithErrors => { "The mapping should not fail." must equalTo("Error") },
        { number => number must equalTo(5) }
      )
    }

    "5 must not be a valid number(5,10, strict = true)" in {
      Form("value" -> number(5, 10, strict = true)).bind(Map("value" -> "5")).fold(
        formWithErrors => { formWithErrors.errors.head.message must equalTo("error.min.strict") },
        { number => "The mapping should fail." must equalTo("Error") }
      )
    }

    "Text containing whitespace only should be rejected by nonEmptyText" in {
      Form("value" -> nonEmptyText).bind(Map("value" -> " ")).fold(
        formWithErrors => { formWithErrors.errors.head.message must equalTo("error.required") },
        { text => "The mapping should fail." must equalTo("Error") }
      )
    }
  }

  "ParameterValidator" should {
    "accept a valid value" in {
      ParameterValidator(List(Constraints.max(10)), Some(9)) must equalTo(Valid)
    }

    "refuse a value out of range" in {
      val result = Invalid(List(ValidationError("error.max", 10)))
      ParameterValidator(List(Constraints.max(10)), Some(11)) must equalTo(result)
    }

    "validate multiple values" in {
      val constraints = List(Constraints.max(10), Constraints.min(1))
      val values = Seq(Some(9), Some(0), Some(5))
      val expected = Invalid(List(ValidationError("error.min", 1)))

      ParameterValidator(constraints, values: _*) must equalTo(expected)
    }

    "validate multiple string values and multiple validation errors" in {
      val constraints = List(Constraints.maxLength(10), Constraints.minLength(1))
      val values = Seq(Some(""), Some("12345678910"), Some("valid"))
      val expected = Invalid(List(ValidationError("error.minLength", 1), ValidationError("error.maxLength", 10)))

      ParameterValidator(constraints, values: _*) must equalTo(expected)
    }
  }

}
