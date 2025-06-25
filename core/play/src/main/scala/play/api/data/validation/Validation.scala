/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.data.validation

import play.api.libs.json.JsonValidationError

/**
 * A form constraint.
 *
 * @tparam T type of values handled by this constraint
 * @param name the constraint name, to be displayed to final user
 * @param args the message arguments, to format the constraint name
 * @param f the validation function
 */
case class Constraint[-T](name: Option[String], args: Seq[Any])(f: (T => ValidationResult)) {

  /**
   * Run the constraint validation.
   *
   * @param t the value to validate
   * @return the validation result
   */
  def apply(t: T): ValidationResult = f(t)
}

/**
 * This object provides helpers for creating `Constraint` values.
 *
 * For example:
 * {{{
 *   val negative = Constraint[Int] {
 *     case i if i < 0 => Valid
 *     case _ => Invalid("Must be a negative number.")
 *   }
 * }}}
 */
object Constraint {

  /**
   * Creates a new anonymous constraint from a validation function.
   *
   * @param f the validation function
   * @return a constraint
   */
  def apply[T](f: (T => ValidationResult)): Constraint[T] = apply(None, Nil)(f)

  /**
   * Creates a new named constraint from a validation function.
   *
   * @param name the constraint name
   * @param args the constraint arguments, used to format the constraint name
   * @param f the validation function
   * @return a constraint
   */
  def apply[T](name: String, args: Any*)(f: (T => ValidationResult)): Constraint[T] = apply(Some(name), args.toSeq)(f)
}

/**
 * Defines a set of built-in constraints.
 */
object Constraints extends Constraints

/**
 * Defines a set of built-in constraints.
 *
 * @define emailAddressDoc Defines an ‘emailAddress’ constraint for `String` values which will validate email addresses.
 *
 * '''name'''[constraint.email]
 * '''error'''[error.email]
 *
 * @define nonEmptyDoc Defines a ‘required’ constraint for `String` values, i.e. one in which empty strings are invalid.
 *
 * '''name'''[constraint.required]
 * '''error'''[error.required]
 */
trait Constraints {
  private val emailRegex =
    """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r

  /**
   * $emailAddressDoc
   */
  def emailAddress(errorMessage: String = "error.email"): Constraint[String] = Constraint[String]("constraint.email") {
    e =>
      if (e == null) Invalid(ValidationError(errorMessage))
      else if (e.trim.isEmpty) Invalid(ValidationError(errorMessage))
      else
        emailRegex
          .findFirstMatchIn(e)
          .map(_ => Valid)
          .getOrElse(Invalid(ValidationError(errorMessage)))
  }

  /**
   * $emailAddressDoc
   */
  def emailAddress: Constraint[String] = emailAddress()

  /**
   * $nonEmptyDoc
   */
  def nonEmpty(errorMessage: String = "error.required"): Constraint[String] =
    Constraint[String]("constraint.required") { o =>
      if (o == null) Invalid(ValidationError(errorMessage))
      else if (o.trim.isEmpty) Invalid(ValidationError(errorMessage))
      else Valid
    }

  /**
   * $nonEmptyDoc
   */
  def nonEmpty: Constraint[String] = nonEmpty()

  /**
   * Defines a minimum value for `Ordered` values, by default the value must be greater than or equal to the constraint parameter
   *
   * '''name'''[constraint.min(minValue)]
   * '''error'''[error.min(minValue)] or [error.min.strict(minValue)]
   */
  def min[T](
      minValue: T,
      strict: Boolean = false,
      errorMessage: String = "error.min",
      strictErrorMessage: String = "error.min.strict"
  )(implicit ordering: scala.math.Ordering[T]): Constraint[T] = Constraint[T]("constraint.min", minValue) { o =>
    (ordering.compare(o, minValue).sign, strict) match {
      case (1, _) | (0, false) => Valid
      case (_, false)          => Invalid(ValidationError(errorMessage, minValue))
      case (_, true)           => Invalid(ValidationError(strictErrorMessage, minValue))
    }
  }

  /**
   * Defines a maximum value for `Ordered` values, by default the value must be less than or equal to the constraint parameter
   *
   * '''name'''[constraint.max(maxValue)]
   * '''error'''[error.max(maxValue)] or [error.max.strict(maxValue)]
   */
  def max[T](
      maxValue: T,
      strict: Boolean = false,
      errorMessage: String = "error.max",
      strictErrorMessage: String = "error.max.strict"
  )(implicit ordering: scala.math.Ordering[T]): Constraint[T] = Constraint[T]("constraint.max", maxValue) { o =>
    (ordering.compare(o, maxValue).sign, strict) match {
      case (-1, _) | (0, false) => Valid
      case (_, false)           => Invalid(ValidationError(errorMessage, maxValue))
      case (_, true)            => Invalid(ValidationError(strictErrorMessage, maxValue))
    }
  }

  /**
   * Defines a minimum length constraint for `String` values, i.e. the string’s length must be greater than or equal to the constraint parameter
   *
   * '''name'''[constraint.minLength(length)]
   * '''error'''[error.minLength(length)]
   */
  def minLength(length: Int, errorMessage: String = "error.minLength"): Constraint[String] =
    Constraint[String]("constraint.minLength", length) { o =>
      require(length >= 0, "string minLength must not be negative")
      if (o == null) Invalid(ValidationError(errorMessage, length))
      else if (o.size >= length) Valid
      else Invalid(ValidationError(errorMessage, length))
    }

  /**
   * Defines a maximum length constraint for `String` values, i.e. the string’s length must be less than or equal to the constraint parameter
   *
   * '''name'''[constraint.maxLength(length)]
   * '''error'''[error.maxLength(length)]
   */
  def maxLength(length: Int, errorMessage: String = "error.maxLength"): Constraint[String] =
    Constraint[String]("constraint.maxLength", length) { o =>
      require(length >= 0, "string maxLength must not be negative")
      if (o == null) Invalid(ValidationError(errorMessage, length))
      else if (o.size <= length) Valid
      else Invalid(ValidationError(errorMessage, length))
    }

  /**
   * Defines a regular expression constraint for `String` values, i.e. the string must match the regular expression pattern
   *
   * '''name'''[constraint.pattern(regex)] or defined by the name parameter.
   * '''error'''[error.pattern(regex)] or defined by the error parameter.
   */
  def pattern(
      regex: => scala.util.matching.Regex,
      name: String = "constraint.pattern",
      error: String = "error.pattern"
  ): Constraint[String] = Constraint[String](name, () => regex) { o =>
    require(regex != null, "regex must not be null")
    require(name != null, "name must not be null")
    require(error != null, "error must not be null")

    if (o == null) Invalid(ValidationError(error, regex))
    else regex.unapplySeq(o).map(_ => Valid).getOrElse(Invalid(ValidationError(error, regex)))
  }
}

/**
 * A validation result.
 */
sealed trait ValidationResult

/**
 * Validation was a success.
 */
case object Valid extends ValidationResult

/**
 * Validation was a failure.
 *
 * @param errors the resulting errors
 */
case class Invalid(errors: Seq[ValidationError]) extends ValidationResult {

  /**
   * Combines these validation errors with another validation failure.
   *
   * @param other validation failure
   * @return a new merged `Invalid`
   */
  def ++(other: Invalid): Invalid = Invalid(this.errors ++ other.errors)
}

/**
 * This object provides helper methods to construct `Invalid` values.
 */
object Invalid {

  /**
   * Creates an `Invalid` value with a single error.
   *
   * @param error the validation error
   * @return an `Invalid` value
   */
  def apply(error: ValidationError): Invalid = Invalid(Seq(error))

  /**
   * Creates an `Invalid` value with a single error.
   *
   * @param error the validation error message
   * @param args the validation error message arguments
   * @return an `Invalid` value
   */
  def apply(error: String, args: Any*): Invalid = Invalid(Seq(ValidationError(error, args: _*)))
}

object ParameterValidator {
  def apply[T](constraints: Iterable[Constraint[T]], optionalParam: Option[T]*) =
    optionalParam.flatMap {
      _.map { param =>
        constraints.flatMap {
          _(param) match {
            case i: Invalid => Some(i)
            case _          => None
          }
        }
      }
    }.flatten match {
      case Nil      => Valid
      case invalids =>
        invalids.reduceLeft { (a, b) => a ++ b }
    }
}

/**
 * A validation error.
 *
 * @param messages the error message, if more then one message is passed it will use the last one
 * @param args the error message arguments
 */
case class ValidationError(messages: Seq[String], args: Any*) {
  lazy val message = messages.last
}

object ValidationError {

  /**
   * Conversion from a JsonValidationError to a Play ValidationError.
   */
  def fromJsonValidationError(jve: JsonValidationError): ValidationError = {
    ValidationError(jve.message, jve.args: _*)
  }

  def apply(message: String, args: Any*) = new ValidationError(Seq(message), args: _*)
}
