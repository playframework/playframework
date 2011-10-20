package play.api.data.validation

import play.api.data._

/**
 * A form constraint.
 *
 * @tparam T type of values handled by this constraint.
 * @param name The constraint name (to be displayed to final user)
 * @param args The message args (to format the constraint name).
 * @param f The validation function.
 */
case class Constraint[T](name: Option[String], args: Seq[Any])(f: (T => ValidationResult)) {

  /**
   * Run the constraint validation.
   *
   * @param t The value to validate.
   * @return The validation result.
   */
  def apply(t: T): ValidationResult = f(t)
}

/**
 * This object provides helper to create Constraint values.
 *
 * Example:
 * {{{
 *   val negative = Constraint[Int] {
 *     case i if i < 0 => Valid
 *     case _ => Invalid("Must be a negative number.")
 *   }
 * }}}
 */
object Constraint {

  /**
   * Create a new anonymous constraint from a validation function.
   *
   * @param f The validation function.
   * @return A constraint.
   */
  def apply[T](f: (T => ValidationResult)): Constraint[T] = apply(None, Nil)(f)

  /**
   * Create a new named constraint from a validation function.
   *
   * @param name The constraint name.
   * @param args The constraint arguments (used to format the constraint name).
   * @param f The validation function.
   * @return A constraint.
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
   * Define a 'required' constraint for String value (ie. empty string are invalid).
   *
   * '''name'''[constraint.required]
   * '''error'''[error.required]
   */
  def required = Constraint[String]("constraint.required") { o =>
    if (o.isEmpty) Invalid(ValidationError("error.required")) else Valid
  }

  /**
   * Define a 'min' constraint for Long value (ie. value must be greater or equals).
   *
   * '''name'''[constraint.min(minValue)]
   * '''error'''[error.min(minValue)]
   */
  def min(minValue: Long) = Constraint[Long]("constraint.min", minValue) { o =>
    if (o >= minValue) Valid else Invalid(ValidationError("error.min", minValue))
  }

  /**
   * Define a 'max' constraint for Long value (ie. value must be less or equals).
   *
   * '''name'''[constraint.max(maxValue)]
   * '''error'''[error.max(maxValue)]
   */
  def max(maxValue: Long) = Constraint[Long]("constraint.max", maxValue) { o =>
    if (o <= maxValue) Valid else Invalid(ValidationError("error.max", maxValue))
  }

  /**
   * Define a 'min' constraint for Int value (ie. value must be greater or equals).
   *
   * '''name'''[constraint.min(minValue)]
   * '''error'''[error.min(minValue)]
   */
  def min(minValue: Int) = Constraint[Int]("constraint.min", minValue) { o =>
    if (o >= minValue) Valid else Invalid(ValidationError("error.min", minValue))
  }

  /**
   * Define a 'max' constraint for Int value (ie. value must be less or equals).
   *
   * '''name'''[constraint.max(maxValue)]
   * '''error'''[error.max(maxValue)]
   */
  def max(maxValue: Int) = Constraint[Int]("constraint.max", maxValue) { o =>
    if (o <= maxValue) Valid else Invalid(ValidationError("error.max", maxValue))
  }

  /**
   * Define a 'min length' constraint for String value (ie. string length must be greater or equals).
   *
   * '''name'''[constraint.minLength(length)]
   * '''error'''[error.minLength(length)]
   */
  def minLength(length: Int) = Constraint[String]("constraint.minLength", length) { o =>
    if (o.size >= length) Valid else Invalid(ValidationError("error.minLength", length))
  }

  /**
   * Define a 'max length' constraint for String value (ie. string length must be less or equals).
   *
   * '''name'''[constraint.maxLength(length)]
   * '''error'''[error.maxLength(length)]
   */
  def maxLength(length: Int) = Constraint[String]("constraint.maxLength", length) { o =>
    if (o.size <= length) Valid else Invalid(ValidationError("error.maxLength", length))
  }

  /**
   * Define a 'pattern' constraint for String value (ie. string must match the pattern regex).
   *
   * '''name'''[constraint.pattern(regex)] or defined by the name parameter.
   * '''error'''[error.pattern(regex)] or defined by the error parameter.
   */
  def pattern(regex: scala.util.matching.Regex, name: String = "constraint.pattern", error: String = "error.pattern") = Constraint[String](name, regex) { o =>
    regex.unapplySeq(o).map(_ => Valid).getOrElse(Invalid(ValidationError(error, regex)))
  }

}

/**
 * A validation result
 */
trait ValidationResult

/**
 * Validation was a success.
 */
case object Valid extends ValidationResult

/**
 * Validation was a failure.
 *
 * @param errors The resulting errors.
 */
case class Invalid(errors: Seq[ValidationError]) extends ValidationResult {

  /**
   * Combine these validation errors with another validation failure.
   *
   * @param Another validation failure.
   * @return A new merged Invalid.
   */
  def ++(other: Invalid) = Invalid(this.errors ++ other.errors)
}

/**
 * This object provides helper method to construct Invalid values.
 */
object Invalid {

  /**
   * Create an Invalid value with a single error.
   *
   * @param error The validation error.
   * @return An Invalid value.
   */
  def apply(error: ValidationError): Invalid = Invalid(Seq(error))

  /**
   * Create an Invalid value with a single error.
   *
   * @param error The validation error message.
   * @param args The validation error message arguments.
   * @return An Invalid value.
   */
  def apply(error: String, args: Any*): Invalid = Invalid(Seq(ValidationError(error, args: _*)))
}

/**
 * A validation error.
 *
 * @param message The error message.
 * @param args The error message arguments.
 */
case class ValidationError(message: String, args: Any*)

