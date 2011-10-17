package play.api.data

package validation {

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

}

package format {

  trait Formatter[T] {
    val format: Option[(String, Seq[Any])] = None
    def bind(key: String, data: Map[String, String]): Either[Seq[FormError], T]
    def unbind(key: String, value: T): Map[String, String]
  }

  object Formats {

    implicit def stringFormat = new Formatter[String] {
      def bind(key: String, data: Map[String, String]) = data.get(key).toRight(Seq(FormError(key, "error.required", Nil)))
      def unbind(key: String, value: String) = Map(key -> value)
    }

    implicit def longFormat = new Formatter[Long] {

      override val format = Some("format.numeric", Nil)

      def bind(key: String, data: Map[String, String]) = {
        stringFormat.bind(key, data).right.flatMap { s =>
          scala.util.control.Exception.allCatch[Long]
            .either(java.lang.Long.parseLong(s))
            .left.map(e => Seq(FormError(key, "error.number", Nil)))
        }
      }

      def unbind(key: String, value: Long) = Map(key -> value.toString)
    }

    implicit def booleanFormat = new Formatter[Boolean] {

      override val format = Some("format.boolean", Nil)

      def bind(key: String, data: Map[String, String]) = {
        Right(data.get(key).getOrElse("false")).right.flatMap {
          case "true" => Right(true)
          case "false" => Right(false)
          case _ => Left(Seq(FormError(key, "error.boolean", Nil)))
        }
      }

      def unbind(key: String, value: Boolean) = Map(key -> value.toString)
    }

    import java.util.Date
    import java.text.SimpleDateFormat

    def dateFormat(pattern: String): Formatter[Date] = new Formatter[Date] {

      override val format = Some("format.date", Seq(pattern))

      def bind(key: String, data: Map[String, String]) = {
        stringFormat.bind(key, data).right.flatMap { s =>
          scala.util.control.Exception.allCatch[Date]
            .either(new SimpleDateFormat(pattern).parse(s))
            .left.map(e => Seq(FormError(key, "error.date", Nil)))
        }
      }

      def unbind(key: String, value: Date) = Map(key -> new SimpleDateFormat(pattern).format(value))
    }

    implicit val dateFormat: Formatter[Date] = dateFormat("yyyy-MM-dd")

    def sqlDateFormat(pattern: String): Formatter[java.sql.Date] = new Formatter[java.sql.Date] {

      override val format = Some("format.date", Seq(pattern))

      def bind(key: String, data: Map[String, String]) = {
        dateFormat(pattern).bind(key, data).right.map(d => new java.sql.Date(d.getTime))
      }

      def unbind(key: String, value: java.sql.Date) = dateFormat(pattern).unbind(key, value)

    }

    implicit val sqlDateFormat: Formatter[java.sql.Date] = sqlDateFormat("yyyy-MM-dd")

  }

}

import format._
import validation._

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

case class ObjectMapping1[T <: Product, A](apply: Function1[A, T], fa: (String, Mapping[A]), val key: String = "", val constraints: Seq[Constraint[T]] = Nil) extends Mapping[T] with ObjectMapping {

  val fieldA = fa._2.withPrefix(fa._1).withPrefix(key)

  def bind(data: Map[String, String]) = {
    merge(fieldA.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(
          values(0).asInstanceOf[A]))
      }
    }
  }

  def unbind(value: T) = {
    val a = fieldA.unbind(value.productElement(0).asInstanceOf[A])
    (a._1) -> (a._2)
  }

  def withPrefix(prefix: String) = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[T]*) = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ fieldA.mappings

}

case class ObjectMapping2[T <: Product, A, B](apply: Function2[A, B, T], fa: (String, Mapping[A]), fb: (String, Mapping[B]), val key: String = "", val constraints: Seq[Constraint[T]] = Nil) extends Mapping[T] with ObjectMapping {

  val fieldA = fa._2.withPrefix(fa._1).withPrefix(key)
  val fieldB = fb._2.withPrefix(fb._1).withPrefix(key)

  def bind(data: Map[String, String]) = {
    merge(fieldA.bind(data), fieldB.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(
          values(0).asInstanceOf[A],
          values(1).asInstanceOf[B]))
      }
    }
  }

  def unbind(value: T) = {
    val a = fieldA.unbind(value.productElement(0).asInstanceOf[A])
    val b = fieldB.unbind(value.productElement(1).asInstanceOf[B])
    (a._1 ++ b._1) -> (a._2 ++ b._2)
  }

  def withPrefix(prefix: String) = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[T]*) = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ fieldA.mappings ++ fieldB.mappings

}

case class ObjectMapping3[T <: Product, A, B, C](apply: Function3[A, B, C, T], fa: (String, Mapping[A]), fb: (String, Mapping[B]), fc: (String, Mapping[C]), val key: String = "", val constraints: Seq[Constraint[T]] = Nil) extends Mapping[T] with ObjectMapping {

  val fieldA = fa._2.withPrefix(fa._1).withPrefix(key)
  val fieldB = fb._2.withPrefix(fb._1).withPrefix(key)
  val fieldC = fc._2.withPrefix(fc._1).withPrefix(key)

  def bind(data: Map[String, String]) = {

    merge(fieldA.bind(data), fieldB.bind(data), fieldC.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(
          values(0).asInstanceOf[A],
          values(1).asInstanceOf[B],
          values(2).asInstanceOf[C]))
      }
    }

  }

  def unbind(value: T) = {
    val a = fieldA.unbind(value.productElement(0).asInstanceOf[A])
    val b = fieldB.unbind(value.productElement(1).asInstanceOf[B])
    val c = fieldC.unbind(value.productElement(2).asInstanceOf[C])
    (a._1 ++ b._1 ++ c._1) -> (a._2 ++ b._2 ++ c._2)
  }

  def withPrefix(prefix: String) = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[T]*) = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ fieldA.mappings ++ fieldB.mappings ++ fieldC.mappings

}

case class ObjectMapping4[T <: Product, A, B, C, D](apply: Function4[A, B, C, D, T], fa: (String, Mapping[A]), fb: (String, Mapping[B]), fc: (String, Mapping[C]), fd: (String, Mapping[D]), val key: String = "", val constraints: Seq[Constraint[T]] = Nil) extends Mapping[T] with ObjectMapping {

  val fieldA = fa._2.withPrefix(fa._1).withPrefix(key)
  val fieldB = fb._2.withPrefix(fb._1).withPrefix(key)
  val fieldC = fc._2.withPrefix(fc._1).withPrefix(key)
  val fieldD = fd._2.withPrefix(fd._1).withPrefix(key)

  def bind(data: Map[String, String]) = {

    merge(fieldA.bind(data), fieldB.bind(data), fieldC.bind(data), fieldD.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(
          values(0).asInstanceOf[A],
          values(1).asInstanceOf[B],
          values(2).asInstanceOf[C],
          values(3).asInstanceOf[D]))
      }
    }

  }

  def unbind(value: T) = {
    val a = fieldA.unbind(value.productElement(0).asInstanceOf[A])
    val b = fieldB.unbind(value.productElement(1).asInstanceOf[B])
    val c = fieldC.unbind(value.productElement(2).asInstanceOf[C])
    val d = fieldD.unbind(value.productElement(3).asInstanceOf[D])
    (a._1 ++ b._1 ++ c._1 ++ d._1) -> (a._2 ++ b._2 ++ c._2 ++ d._2)
  }

  def withPrefix(prefix: String) = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[T]*) = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ fieldA.mappings ++ fieldB.mappings ++ fieldC.mappings ++ fieldD.mappings

}

