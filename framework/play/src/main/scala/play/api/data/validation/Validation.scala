package play.api.data.validation

import play.api.data._

case class Constraint[T](name: Option[String], args: Seq[Any])(f: (T => ValidationResult)) {
  def apply(t: T) = f(t)
}

object Constraint {
  def apply[T](f: (T => ValidationResult)): Constraint[T] = apply(None, Nil)(f)
  def apply[T](name: String, args: Any*)(f: (T => ValidationResult)): Constraint[T] = apply(Some(name), args.toSeq)(f)
}

object Constraints extends Constraints

trait Constraints {

  def required = Constraint[String]("constraint.required") { o =>
    if (o.isEmpty) Invalid(ValidationError("error.required")) else Valid
  }

  def min(l: Long) = Constraint[Long]("constraint.min", l) { o =>
    if (o >= l) Valid else Invalid(ValidationError("error.min", l))
  }

  def max(l: Long) = Constraint[Long]("constraint.max", l) { o =>
    if (o <= l) Valid else Invalid(ValidationError("error.max", l))
  }

  def min(l: Int) = Constraint[Int]("constraint.min", l) { o =>
    if (o >= l) Valid else Invalid(ValidationError("error.min", l))
  }

  def max(l: Int) = Constraint[Int]("constraint.max", l) { o =>
    if (o <= l) Valid else Invalid(ValidationError("error.max", l))
  }

  def minLength(l: Int) = Constraint[String]("constraint.minLength", l) { o =>
    if (o.size >= l) Valid else Invalid(ValidationError("error.minLength", l))
  }

  def maxLength(l: Int) = Constraint[String]("constraint.maxLength", l) { o =>
    if (o.size <= l) Valid else Invalid(ValidationError("error.maxLength", l))
  }

  def pattern(regex: scala.util.matching.Regex, name: String = "", error: String = "error.pattern") = Constraint[String](name, regex) { o =>
    regex.unapplySeq(o).map(_ => Valid).getOrElse(Invalid(ValidationError(error, regex)))
  }

}

trait ValidationResult

case object Valid extends ValidationResult
case class Invalid(errors: Seq[ValidationError]) extends ValidationResult {
  def ++(other: Invalid) = Invalid(this.errors ++ other.errors)
}

object Invalid {
  def apply(error: ValidationError): Invalid = Invalid(Seq(error))
}

case class ValidationError(msg: String, args: Any*)

