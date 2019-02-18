/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.forms.scalaforms {

  import play.api.data._
  import play.api.data.Forms._
  import play.api.data.validation._

  import org.junit.runner.RunWith

  import org.specs2.runner.JUnitRunner

  import org.specs2.mutable._

  @RunWith(classOf[JUnitRunner])
  class CustomValidationsSpec extends Specification {

    // #passwordcheck-constraint
    val allNumbers = """\d*""".r
    val allLetters = """[A-Za-z]*""".r

    val passwordCheckConstraint: Constraint[String] = Constraint("constraints.passwordcheck")({ plainText =>
      val errors = plainText match {
        case allNumbers() => Seq(ValidationError("Password is all numbers"))
        case allLetters() => Seq(ValidationError("Password is all letters"))
        case _            => Nil
      }
      if (errors.isEmpty) {
        Valid
      } else {
        Invalid(errors)
      }
    })
    // #passwordcheck-constraint

    val MIN_PASSWORD_LENGTH = 10

    // #passwordcheck-mapping
    val passwordCheck: Mapping[String] = nonEmptyText(minLength = 10)
      .verifying(passwordCheckConstraint)
    // #passwordcheck-mapping

    "password check" should {

      "return invalid with all letters" in {
        passwordCheckConstraint("abcdef").must(be_==(Invalid(ValidationError("Password is all letters"))))
      }

      "return invalid with all numbers" in {
        passwordCheckConstraint("12324").must(be_==(Invalid(ValidationError("Password is all numbers"))))
      }

      "return valid with both letters and numbers" in {
        passwordCheckConstraint("abc123").must(be_==(Valid))
      }

    }

  }

}
