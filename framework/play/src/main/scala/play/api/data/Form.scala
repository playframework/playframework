package play.api.data

import format._
import validation._

/**
 * An helper to manage HTML form description, submission, validation.
 *
 * @param mapping The mapping
 */
case class Form[T](mapping: Mapping[T], data: Map[String, String], errors: Seq[FormError], value: Option[T]) {

  val constraints = mapping.mappings.map { m =>
    m.key -> m.constraints.collect { case Constraint(Some(name), args) => name -> args }.toSeq
  }.collect {
    case (k, c @ List(_, _*)) => k -> c
  }.toMap

  val formats = mapping.mappings.map { m =>
    m.key -> m.format
  }.collect {
    case (k, Some(f)) => k -> f
  }.toMap

  def bind(data: Map[String, String]): Form[T] = mapping.bind(data).fold(
    errors => this.copy(data = data, errors = errors, value = None),
    value => this.copy(data = data, errors = Nil, value = Some(value)))

  def bind()(implicit request: play.api.mvc.Request[play.api.mvc.AnyContent]): Form[T] = {
    bind(request.body.urlFormEncoded.mapValues(_.headOption.getOrElse("")))
  }

  def fill(value: T) = {
    val result = mapping.unbind(value)
    this.copy(data = result._1, errors = result._2, value = Some(value))
  }

  def fold[X](fa: (Form[T]) => X, fb: (T) => X) = value.map(fb(_)).getOrElse(fa(this))

  def apply(key: String): Field = Field(
    key,
    constraints.get(key).getOrElse(Nil),
    formats.get(key),
    errors.collect { case e if e.key == key => e },
    data.get(key))

  def globalError: Option[FormError] = globalErrors.headOption
  def globalErrors: Seq[FormError] = errors.filter(_.key.isEmpty)

  def forField[R](key: String)(handler: Field => R): R = handler(this(key))

}

case class Field(name: String, constraints: Seq[(String, Seq[Any])], format: Option[(String, Seq[Any])], errors: Seq[FormError], value: Option[String]) {
  lazy val id = name.replace('.', '_')
  def error = errors.headOption
}

object Form {
  def apply[T](m: Mapping[T]): Form[T] = Form(m, Map.empty, Nil, None)
  def apply[T](m: (String, Mapping[T])): Form[T] = Form(m._2.withPrefix(m._1), Map.empty, Nil, None)
}

object `package` {

  def of[T](implicit binder: Formatter[T]) = FieldMapping[T]()(binder)

  def of[T <: Product, A](apply: Function1[A, T])(a: (String, Mapping[A])): Mapping[T] = {
    ObjectMapping1(apply, a)
  }

  def of[T <: Product, A, B](apply: Function2[A, B, T])(a: (String, Mapping[A]), b: (String, Mapping[B])): Mapping[T] = {
    ObjectMapping2(apply, a, b)
  }

  def of[T <: Product, A, B, C](apply: Function3[A, B, C, T])(a: (String, Mapping[A]), b: (String, Mapping[B]), c: (String, Mapping[C])): Mapping[T] = {
    ObjectMapping3(apply, a, b, c)
  }

  def of[T <: Product, A, B, C, D](apply: Function4[A, B, C, D, T])(a: (String, Mapping[A]), b: (String, Mapping[B]), c: (String, Mapping[C]), d: (String, Mapping[D])): Mapping[T] = {
    ObjectMapping4(apply, a, b, c, d)
  }

  def of[A, B](a: (String, Mapping[A]), b: (String, Mapping[B])): Mapping[(A, B)] = of((a: A, b: B) => (a, b))(a, b)
  def of[A, B, C](a: (String, Mapping[A]), b: (String, Mapping[B]), c: (String, Mapping[C])): Mapping[(A, B, C)] = of((a: A, b: B, c: C) => (a, b, c))(a, b, c)
  def of[A, B, C, D](a: (String, Mapping[A]), b: (String, Mapping[B]), c: (String, Mapping[C]), d: (String, Mapping[D])): Mapping[(A, B, C, D)] = of((a: A, b: B, c: C, d: D) => (a, b, c, d))(a, b, c, d)

  // --

  import Form._
  import Formats._
  import validation._

  val text: Mapping[String] = of[String]
  def text(minLength: Int = 0, maxLength: Int = Int.MaxValue): Mapping[String] = (minLength, maxLength) match {
    case (0, Int.MaxValue) => text
    case (min, Int.MaxValue) => text verifying Constraints.minLength(min)
    case (0, max) => text verifying Constraints.maxLength(max)
    case (min, max) => text verifying (Constraints.minLength(min), Constraints.maxLength(max))
  }

  val number: Mapping[Long] = of[Long]

  val date: Mapping[java.util.Date] = of[java.util.Date]
  def date(pattern: String): Mapping[java.util.Date] = of[java.util.Date] as dateFormat(pattern)

  val sqlDate: Mapping[java.sql.Date] = of[java.sql.Date]
  def sqlDate(pattern: String): Mapping[java.sql.Date] = of[java.sql.Date] as sqlDateFormat(pattern)

  val email: Mapping[String] = of[String] verifying Constraints.pattern(
    """\b[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,4}\b""".r,
    "constraint.email",
    "error.email")

  val boolean: Mapping[Boolean] = of[Boolean]

}

case class FormError(key: String, message: String, args: Seq[Any])

trait Mapping[T] {

  val key: String
  val mappings: Seq[Mapping[_]]
  val format: Option[(String, Seq[Any])] = None
  val constraints: Seq[Constraint[T]]

  def bind(data: Map[String, String]): Either[Seq[FormError], T]
  def unbind(value: T): (Map[String, String], Seq[FormError])

  def withPrefix(prefix: String): Mapping[T]

  def verifying(constraints: Constraint[T]*): Mapping[T]
  def verifying(constraint: (T => Boolean)): Mapping[T] = verifying("error.unknown", constraint)
  def verifying(error: String, constraint: (T => Boolean)): Mapping[T] = {
    verifying(Constraint { t: T =>
      if (constraint(t)) Valid else Invalid(Seq(ValidationError(error)))
    })
  }

  def addPrefix(prefix: String) = {
    Option(prefix).filterNot(_.isEmpty).map(p => p + Option(key).filterNot(_.isEmpty).map("." + _).getOrElse(""))
  }

  def applyConstraints(t: T): Either[Seq[FormError], T] = {
    Right(t).right.flatMap { v =>
      Option(collectErrors(v)).filterNot(_.isEmpty).toLeft(v)
    }
  }

  def collectErrors(t: T): Seq[FormError] = {
    constraints.map(_(t)).collect {
      case Invalid(errors) => errors.toSeq
    }.flatten.map(ve => FormError(key, ve.msg, ve.args))
  }

}

case class FieldMapping[T](val key: String = "", val constraints: Seq[Constraint[T]] = Nil)(implicit val binder: Formatter[T]) extends Mapping[T] {

  override val format = binder.format

  def verifying(addConstraints: Constraint[T]*) = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  def as(binder: Formatter[T]) = {
    this.copy()(binder)
  }

  def bind(data: Map[String, String]) = binder.bind(key, data).right.flatMap { applyConstraints(_) }

  def unbind(value: T) = binder.unbind(key, value) -> collectErrors(value)

  def withPrefix(prefix: String) = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  val mappings = Seq(this)

}

trait ObjectMapping {

  def merge2(a: Either[Seq[FormError], Seq[Any]], b: Either[Seq[FormError], Seq[Any]]): Either[Seq[FormError], Seq[Any]] = (a, b) match {
    case (Left(errorsA), Left(errorsB)) => Left(errorsA ++ errorsB)
    case (Left(errorsA), Right(_)) => Left(errorsA)
    case (Right(_), Left(errorsB)) => Left(errorsB)
    case (Right(a), Right(b)) => Right(a ++ b)
  }

  def merge(results: Either[Seq[FormError], Any]*): Either[Seq[FormError], Seq[Any]] = {
    val all: Seq[Either[Seq[FormError], Seq[Any]]] = results.map(_.right.map(Seq(_)))
    all.fold(Right(Nil)) { (s, i) => merge2(s, i) }
  }

}

