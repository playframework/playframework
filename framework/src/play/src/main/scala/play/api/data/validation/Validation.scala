/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.data.validation

import play.api.data._

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
 */
trait Constraints {

  /**
   * Defines an ‘emailAddress’ constraint for `String` values which will validate email addresses.
   *
   * '''name'''[constraint.email]
   * '''error'''[error.email]
   */
  private val emailRegex = """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r
  def emailAddress: Constraint[String] = Constraint[String]("constraint.email") { e =>
    if (e == null) Invalid(ValidationError("error.email"))
    else if (e.trim.isEmpty) Invalid(ValidationError("error.email"))
    else emailRegex.findFirstMatchIn(e)
      .map(_ => Valid)
      .getOrElse(Invalid(ValidationError("error.email")))
  }

  /**
   * Defines a ‘required’ constraint for `String` values, i.e. one in which empty strings are invalid.
   *
   * '''name'''[constraint.required]
   * '''error'''[error.required]
   */
  def nonEmpty: Constraint[String] = Constraint[String]("constraint.required") { o =>
    if (o == null) Invalid(ValidationError("error.required")) else if (o.trim.isEmpty) Invalid(ValidationError("error.required")) else Valid
  }

  /**
   * Defines a minimum value for `Ordered` values, by default the value must be greater than or equal to the constraint parameter
   *
   * '''name'''[constraint.min(minValue)]
   * '''error'''[error.min(minValue)] or [error.min.strict(minValue)]
   */
  def min[T](minValue: T, strict: Boolean = false)(implicit ordering: scala.math.Ordering[T]): Constraint[T] = Constraint[T]("constraint.min", minValue) { o =>
    (ordering.compare(o, minValue).signum, strict) match {
      case (1, _) | (0, false) => Valid
      case (_, false) => Invalid(ValidationError("error.min", minValue))
      case (_, true) => Invalid(ValidationError("error.min.strict", minValue))
    }
  }

  /**
   * Defines a maximum value for `Ordered` values, by default the value must be less than or equal to the constraint parameter
   *
   * '''name'''[constraint.max(maxValue)]
   * '''error'''[error.max(maxValue)] or [error.max.strict(maxValue)]
   */
  def max[T](maxValue: T, strict: Boolean = false)(implicit ordering: scala.math.Ordering[T]): Constraint[T] = Constraint[T]("constraint.max", maxValue) { o =>
    (ordering.compare(o, maxValue).signum, strict) match {
      case (-1, _) | (0, false) => Valid
      case (_, false) => Invalid(ValidationError("error.max", maxValue))
      case (_, true) => Invalid(ValidationError("error.max.strict", maxValue))
    }
  }

  /**
   * Defines a minimum length constraint for `String` values, i.e. the string’s length must be greater than or equal to the constraint parameter
   *
   * '''name'''[constraint.minLength(length)]
   * '''error'''[error.minLength(length)]
   */
  def minLength(length: Int): Constraint[String] = Constraint[String]("constraint.minLength", length) { o =>
    require(length >= 0, "string minLength must not be negative")
    if (o == null) Invalid(ValidationError("error.minLength", length)) else if (o.size >= length) Valid else Invalid(ValidationError("error.minLength", length))
  }

  /**
   * Defines a maximum length constraint for `String` values, i.e. the string’s length must be less than or equal to the constraint parameter
   *
   * '''name'''[constraint.maxLength(length)]
   * '''error'''[error.maxLength(length)]
   */
  def maxLength(length: Int): Constraint[String] = Constraint[String]("constraint.maxLength", length) { o =>
    require(length >= 0, "string maxLength must not be negative")
    if (o == null) Invalid(ValidationError("error.maxLength", length)) else if (o.size <= length) Valid else Invalid(ValidationError("error.maxLength", length))
  }

  /**
   * Defines a regular expression constraint for `String` values, i.e. the string must match the regular expression pattern
   *
   * '''name'''[constraint.pattern(regex)] or defined by the name parameter.
   * '''error'''[error.pattern(regex)] or defined by the error parameter.
   */
  def pattern(regex: => scala.util.matching.Regex, name: String = "constraint.pattern", error: String = "error.pattern"): Constraint[String] = Constraint[String](name, () => regex) { o =>
    require(regex != null, "regex must not be null")
    require(name != null, "name must not be null")
    require(error != null, "error must not be null")

    if (o == null) Invalid(ValidationError(error, regex)) else regex.unapplySeq(o).map(_ => Valid).getOrElse(Invalid(ValidationError(error, regex)))
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
   * @param another validation failure
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
            case _ => None
          }
        }
      }
    }.flatten match {
      case Nil => Valid
      case invalids => invalids.reduceLeft {
        (a, b) => a ++ b
      }
    }
}
