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
   * Defines a ‘required’ constraint for `String` values, i.e. one in which empty strings are invalid.
   *
   * '''name'''[constraint.required]
   * '''error'''[error.required]
   */
  def nonEmpty: Constraint[String] = Constraint[String]("constraint.required") { o =>
    if (o.isEmpty) Invalid(ValidationError("error.required")) else Valid
  }

  /**
   * Defines a minimum value for `Int` values, i.e. the value must be greater than or equal to the constraint parameter
   *
   * '''name'''[constraint.min(minValue)]
   * '''error'''[error.min(minValue)]
   */
  def min(minValue: Int): Constraint[Int] = Constraint[Int]("constraint.min", minValue) { o =>
    if (o >= minValue) Valid else Invalid(ValidationError("error.min", minValue))
  }

  /**
   * Defines a minimum value for `Long` values, i.e. the value must be greater than or equal to the constraint parameter
   *
   * '''name'''[constraint.min(minValue)]
   * '''error'''[error.min(minValue)]
   */
  def min(minValue: Long): Constraint[Long] = Constraint[Long]("constraint.min", minValue) { o =>
    if (o >= minValue) Valid else Invalid(ValidationError("error.min", minValue))
  }

  /**
   * Defines a maximum value constraint for `Int` values, i.e. value must be less than or equal to the constraint parameter
   *
   * '''name'''[constraint.max(maxValue)]
   * '''error'''[error.max(maxValue)]
   */
  def max(maxValue: Int): Constraint[Int] = Constraint[Int]("constraint.max", maxValue) { o =>
    if (o <= maxValue) Valid else Invalid(ValidationError("error.max", maxValue))
  }

  /**
   * Defines a maximum value constraint for `Long` values, i.e. value must be less than or equal to the constraint parameter
   *
   * '''name'''[constraint.max(maxValue)]
   * '''error'''[error.max(maxValue)]
   */
  def max(maxValue: Long): Constraint[Long] = Constraint[Long]("constraint.max", maxValue) { o =>
    if (o <= maxValue) Valid else Invalid(ValidationError("error.max", maxValue))
  }

  /**
   * Defines a minimum length constraint for `String` values, i.e. the string’s length must be greater than or equal to the constraint parameter
   *
   * '''name'''[constraint.minLength(length)]
   * '''error'''[error.minLength(length)]
   */
  def minLength(length: Int): Constraint[String] = Constraint[String]("constraint.minLength", length) { o =>
    if (o.size >= length) Valid else Invalid(ValidationError("error.minLength", length))
  }

  /**
   * Defines a maximum length constraint for `String` values, i.e. the string’s length must be less than or equal to the constraint parameter
   *
   * '''name'''[constraint.maxLength(length)]
   * '''error'''[error.maxLength(length)]
   */
  def maxLength(length: Int): Constraint[String] = Constraint[String]("constraint.maxLength", length) { o =>
    if (o.size <= length) Valid else Invalid(ValidationError("error.maxLength", length))
  }

  /**
   * Defines a regular expression constraint for `String` values, i.e. the string must match the regular expression pattern
   *
   * '''name'''[constraint.pattern(regex)] or defined by the name parameter.
   * '''error'''[error.pattern(regex)] or defined by the error parameter.
   */
  def pattern(regex: => scala.util.matching.Regex, name: String = "constraint.pattern", error: String = "error.pattern"): Constraint[String] = Constraint[String](name, () => regex) { o =>
    regex.unapplySeq(o).map(_ => Valid).getOrElse(Invalid(ValidationError(error, regex)))
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

/**
 * A validation error.
 *
 * @param message the error message
 * @param args the error message arguments
 */
case class ValidationError(message: String, args: Any*)

