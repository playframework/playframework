package play.api.data

import format._
import validation._

/**
 * Helper to manage HTML form description, submission and validation.
 *
 * For example, a form handling a `User` case class submission:
 * {{{
 * import play.api.data._
 * import play.api.data.format.Formats._
 *
 * val userForm = Form(
 *   of(User)(
 *     "name" -> of[String],
 *     "age" -> of[Int],
 *     "email" -> of[String]
 *   )
 * )
 * }}}
 *
 * @tparam T the type managed by this form
 * @param mapping the form mapping, which describes all form fields
 * @param data the current form data, used to display the form
 * @param errors the collection of errors associated with this form
 * @param value a concrete value of type `T` if the form submission was successful
 */
case class Form[T](mapping: Mapping[T], data: Map[String, String], errors: Seq[FormError], value: Option[T]) {

  /** Constraints associated with this form, indexed by field name. */
  val constraints: Map[String, List[(String, Seq[Any])]] = mapping.mappings.map { m =>
    m.key -> m.constraints.collect { case Constraint(Some(name), args) => name -> args }.toSeq
  }.collect {
    case (k, c @ List(_, _*)) => k -> c
  }.toMap

  /** Formats associated to this form, indexed by field name. */
  val formats: Map[String, (String, Seq[Any])] = mapping.mappings.map { m =>
    m.key -> m.format
  }.collect {
    case (k, Some(f)) => k -> f
  }.toMap

  /**
   * Binds data to this form, i.e. handles form submission.
   *
   * @param data the data to submit
   * @return a copy of this form, filled with the new data
   */
  def bind(data: Map[String, String]): Form[T] = mapping.bind(data).fold(
    errors => this.copy(data = data, errors = errors, value = None),
    value => this.copy(data = data, errors = Nil, value = Some(value)))

  /**
   * Binds request data to this form, i.e. handles form submission.
   *
   * @return a copy of this form filled with the new data
   */
  def bindFromRequest()(implicit request: play.api.mvc.Request[play.api.mvc.AnyContent]): Form[T] = {
    val data = request.body.asUrlFormEncoded.getOrElse(Map.empty) ++ request.queryString
    bind(data.mapValues(_.headOption.getOrElse("")))
  }

  /**
   * Fills this form with a existing value, used for edit forms.
   *
   * @param value an existing value of type `T`, used to fill this form
   * @return a copy of this form filled with the new data
   */
  def fill(value: T) = {
    val result = mapping.unbind(value)
    this.copy(data = result._1, errors = result._2, value = Some(value))
  }

  /**
   * Handles form results. Either the form has errors, or the submission was a success and a
   * concrete value is available.
   *
   * For example:
   * {{{
   *   anyForm.bindFromRequest().fold(
   *      f => redisplayForm(f),
   *      t => handleValidFormSubmission(t)
   *   )
   * }}}
   *
   * @tparam R common result type
   * @param hasErrors a function to handle forms with errors
   * @param success a function to handle form submission success
   * @return a result `R`.
   */
  def fold[R](hasErrors: (Form[T]) => R, success: (T) => R) = value.map(success(_)).getOrElse(hasErrors(this))

  /**
   * Retrieves a field.
   *
   * For example:
   * {{{
   * val usernameField = userForm("username")
   * }}}
   *
   * @param key the field name
   * @return the field, returned even if the field does not exist
   */
  def apply(key: String): Field = Field(
    key,
    constraints.get(key).getOrElse(Nil),
    formats.get(key),
    errors.collect { case e if e.key == key => e },
    data.get(key))

  /**
   * Retrieves the first global error, if it exists, i.e. an error without any key.
   *
   * @return an error
   */
  def globalError: Option[FormError] = globalErrors.headOption

  /**
   * Retrieves all global errors, i.e. errors without a key.
   *
   * @return all global errors
   */
  def globalErrors: Seq[FormError] = errors.filter(_.key.isEmpty)

  /**
   * Applies a function for a field.
   *
   * For example:
   * {{{
   * userForm.forField("username") { field =>
   *   <input type="text" name={field.name} value={field.value.getOrElse("")} />
   * }
   * }}}
   *
   * @tparam R result type
   * @param key field name
   * @param handler field handler (transform the field to `R`)
   */
  def forField[R](key: String)(handler: Field => R): R = handler(this(key))

  /** Returns `true` if there is an error related to this form. */
  def hasErrors = !errors.isEmpty

  /** Returns `true` if there is a global error related to this form. */
  def hasGlobalErrors = !globalErrors.isEmpty

  /** Returns the concrete value, if the submission was a success. */
  def get = value.get

}

/**
 * A form field.
 *
 * @param name the field name
 * @param constraints the constraints associated with the field
 * @param format the format expected for this field
 * @param errors the errors associated to this field
 * @param value the field value, if any
 */
case class Field(name: String, constraints: Seq[(String, Seq[Any])], format: Option[(String, Seq[Any])], errors: Seq[FormError], value: Option[String]) {

  /** The field ID - the same as the field name but with '.' replaced by '_'. */
  lazy val id: String = name.replace('.', '_')

  /**
   * Returns the first error associated with this field, if it exists.
   *
   * @return an error
   */
  def error: Option[FormError] = errors.headOption

  /**
   * Check if this field has errors.
   */
  def hasErrors: Boolean = !errors.isEmpty
}

/** Provides a set of operations for creating `Form` values. */
object Form {

  /**
   * Creates a new form from a mapping.
   *
   * For example:
   * {{{
   * import play.api.data._
   * import play.api.data.format.Formats._
   *
   * val userForm = Form(
   *   of(User)(
   *     "name" -> of[String],
   *     "age" -> of[Int],
   *     "email" -> of[String]
   *   )
   * )
   * }}}
   *
   * @param mapping the form mapping
   * @return a form definition
   */
  def apply[T](mapping: Mapping[T]): Form[T] = Form(mapping, Map.empty, Nil, None)

  /**
   * Creates a new form from a mapping, with a root key.
   *
   * For example:
   * {{{
   * val userForm = Form(
   *   "user" -> of(User)(
   *     "name" -> of[String],
   *     "age" -> of[Int],
   *     "email" -> of[String]
   *   )
   * )
   * }}}
   *
   * @param mapping the root key, form mapping association
   * @return a form definition
   */
  def apply[T](mapping: (String, Mapping[T])): Form[T] = Form(mapping._2.withPrefix(mapping._1), Map.empty, Nil, None)
}

/**
 * A form error.
 *
 * @param key The error key (should be associated with a field using the same key).
 * @param message The form message (often a simple message key needing to be translated).
 * @param args Arguments used to format the message.
 */
case class FormError(key: String, message: String, args: Seq[Any])

/** A mapping is a two-way binder to handle a form field. */
trait Mapping[T] {

  /** The field key. */
  val key: String

  /** Sub-mappings (these can be seen as sub-keys). */
  val mappings: Seq[Mapping[_]]

  /** The Format expected for this field, if it exists. */
  val format: Option[(String, Seq[Any])] = None

  /** The constraints associated with this field. */
  val constraints: Seq[Constraint[T]]

  /**
   * Binds this field, i.e. construct a concrete value from submitted data.
   *
   * @param data the submitted data
   * @return either a concrete value of type `T` or a set of errors, if the binding failed
   */
  def bind(data: Map[String, String]): Either[Seq[FormError], T]

  /**
   * Unbinds this field, i.e. transforms a concrete value to plain data.
   *
   * @param value the value to unbind
   * @return either the plain data or a set of errors, if the unbinding failed
   */
  def unbind(value: T): (Map[String, String], Seq[FormError])

  /**
   * Constructs a new Mapping based on this one, adding a prefix to the key.
   *
   * @param prefix the prefix to add to the key
   * @return the same mapping, with only the key changed
   */
  def withPrefix(prefix: String): Mapping[T]

  /**
   * Constructs a new Mapping based on this one, by adding new constraints.
   *
   * For example:
   * {{{
   *   import play.api.data._
   *   import play.api.data.validation.Constraints._
   *
   *   Form("phonenumber" -> text.verifying(required) )
   * }}}
   *
   * @param constraints the constraints to add
   * @return the new mapping
   */
  def verifying(constraints: Constraint[T]*): Mapping[T]

  /**
   * Constructs a new Mapping based on this one, by adding a new ad-hoc constraint.
   *
   * For example:
   * {{{
   *   import play.api.data._
   *   import play.api.data.validation.Constraints._
   *
   *   Form("phonenumber" -> text.verifying {_.grouped(2).size == 5})
   * }}}
   *
   * @param constraint a function describing the constraint that returns `false` on failure
   * @return the new mapping
   */
  def verifying(constraint: (T => Boolean)): Mapping[T] = verifying("error.unknown", constraint)

  /**
   * Constructs a new Mapping based on this one, by adding a new ad-hoc constraint.
   *
   * For example:
   * {{{
   *   import play.api.data._
   *   import play.api.data.validation.Constraints._
   *
   *   Form("phonenumber" -> text.verifying("Bad phone number", {_.grouped(2).size == 5}))
   * }}}
   *
   * @param error The error message used if the constraint fails
   * @param constraint a function describing the constraint that returns `false` on failure
   * @return the new mapping
   */
  def verifying(error: => String, constraint: (T => Boolean)): Mapping[T] = {
    verifying(Constraint { t: T =>
      if (constraint(t)) Valid else Invalid(Seq(ValidationError(error)))
    })
  }

  // Internal utilities

  protected def addPrefix(prefix: String) = {
    Option(prefix).filterNot(_.isEmpty).map(p => p + Option(key).filterNot(_.isEmpty).map("." + _).getOrElse(""))
  }

  protected def applyConstraints(t: T): Either[Seq[FormError], T] = {
    Right(t).right.flatMap { v =>
      Option(collectErrors(v)).filterNot(_.isEmpty).toLeft(v)
    }
  }

  protected def collectErrors(t: T): Seq[FormError] = {
    constraints.map(_(t)).collect {
      case Invalid(errors) => errors.toSeq
    }.flatten.map(ve => FormError(key, ve.message, ve.args))
  }

}

/**
 * A mapping for optional elements
 *
 * @param wrapped the wrapped mapping
 */
case class OptionalMapping[T](wrapped: Mapping[T], val constraints: Seq[Constraint[Option[T]]] = Nil) extends Mapping[Option[T]] {

  override val format: Option[(String, Seq[Any])] = wrapped.format

  val key = wrapped.key

  /**
   * Constructs a new Mapping based on this one, by adding new constraints.
   *
   * For example:
   * {{{
   *   import play.api.data._
   *   import play.api.data.validation.Constraints._
   *
   *   Form("phonenumber" -> text.verifying(required) )
   * }}}
   *
   * @param constraints the constraints to add
   * @return the new mapping
   */
  def verifying(addConstraints: Constraint[Option[T]]*): Mapping[Option[T]] = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  /**
   * Binds this field, i.e. constructs a concrete value from submitted data.
   *
   * @param data the submitted data
   * @return either a concrete value of type `T` or a set of error if the binding failed
   */
  def bind(data: Map[String, String]): Either[Seq[FormError], Option[T]] = {
    data.get(key).filterNot(_.isEmpty).map(_ => wrapped.bind(data).right.map(Some(_))).getOrElse(Right(None)).right.flatMap(applyConstraints)
  }

  /**
   * Unbinds this field, i.e. transforms a concrete value to plain data.
   *
   * @param value The value to unbind.
   * @return Either the plain data or a set of error if the unbinding failed.
   */
  def unbind(value: Option[T]): (Map[String, String], Seq[FormError]) = {
    val errors = collectErrors(value)
    value.map(wrapped.unbind(_)).map(r => r._1 -> (r._2 ++ errors)).getOrElse(Map.empty -> errors)
  }

  /**
   * Constructs a new Mapping based on this one, adding a prefix to the key.
   *
   * @param prefix the prefix to add to the key
   * @return the same mapping, with only the key changed
   */
  def withPrefix(prefix: String): Mapping[Option[T]] = {
    copy(wrapped = wrapped.withPrefix(prefix))
  }

  /** Sub-mappings (these can be seen as sub-keys). */
  val mappings: Seq[Mapping[_]] = wrapped.mappings

}

/**
 * A mapping for a single field.
 *
 * @param key the field key
 * @param constraints the constraints associated with this field.
 */
case class FieldMapping[T](val key: String = "", val constraints: Seq[Constraint[T]] = Nil)(implicit val binder: Formatter[T]) extends Mapping[T] {

  override val format: Option[(String, Seq[Any])] = binder.format

  /**
   * Constructs a new Mapping based on this one, by adding new constraints.
   *
   * For example:
   * {{{
   *   import play.api.data._
   *   import play.api.data.validation.Constraints._
   *
   *   Form("phonenumber" -> text.verifying(required) )
   * }}}
   *
   * @param constraints the constraints to add
   * @return the new mapping
   */
  def verifying(addConstraints: Constraint[T]*): Mapping[T] = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  /**
   * Changes the binder used to handle this field.
   *
   * @param binder the new binder to use
   * @return the same mapping with a new binder
   */
  def as(binder: Formatter[T]): Mapping[T] = {
    this.copy()(binder)
  }

  /**
   * Binds this field, i.e. constructs a concrete value from submitted data.
   *
   * @param data the submitted data
   * @return either a concrete value of type `T` or a set of errors, if binding failed
   */
  def bind(data: Map[String, String]): Either[Seq[FormError], T] = {
    binder.bind(key, data).right.flatMap { applyConstraints(_) }
  }

  /**
   * Unbinds this field, i.e. transforms a concrete value to plain data.
   *
   * @param value the value to unbind
   * @return either the plain data or a set of errors, if unbinding failed
   */
  def unbind(value: T): (Map[String, String], Seq[FormError]) = {
    binder.unbind(key, value) -> collectErrors(value)
  }

  /**
   * Constructs a new Mapping based on this one, adding a prefix to the key.
   *
   * @param prefix the prefix to add to the key
   * @return the same mapping, with only the key changed
   */
  def withPrefix(prefix: String): Mapping[T] = {
    addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)
  }

  /** Sub-mappings (these can be seen as sub-keys). */
  val mappings: Seq[Mapping[_]] = Seq(this)

}

/** Common helper methods for all object mappings - mappings including several fields. */
trait ObjectMapping {

  /**
   * Merges the result of two bindings.
   *
   * @see bind()
   */
  def merge2(a: Either[Seq[FormError], Seq[Any]], b: Either[Seq[FormError], Seq[Any]]): Either[Seq[FormError], Seq[Any]] = (a, b) match {
    case (Left(errorsA), Left(errorsB)) => Left(errorsA ++ errorsB)
    case (Left(errorsA), Right(_)) => Left(errorsA)
    case (Right(_), Left(errorsB)) => Left(errorsB)
    case (Right(a), Right(b)) => Right(a ++ b)
  }

  /**
   * Merges the result of multiple bindings.
   *
   * @see bind()
   */
  def merge(results: Either[Seq[FormError], Any]*): Either[Seq[FormError], Seq[Any]] = {
    val all: Seq[Either[Seq[FormError], Seq[Any]]] = results.map(_.right.map(Seq(_)))
    all.fold(Right(Nil)) { (s, i) => merge2(s, i) }
  }

}

/**
 * Represents an object binding (ie. a binding for several fields).
 *
 * This is used for objects with one field. Other versions exist, e.g. `ObjectMapping2`, `ObjectMapping3`, etc.
 *
 * @tparam T the complex object type
 * @tparam A the first field type
 * @param apply a constructor function that creates a instance of `T` using field `A`
 * @param fa a mapping for field `A`
 * @param constraints constraints associated with this mapping
 */
case class ObjectMapping1[T <: Product, A](apply: Function1[A, T], fa: (String, Mapping[A]), val key: String = "", val constraints: Seq[Constraint[T]] = Nil) extends Mapping[T] with ObjectMapping {

  val fieldA = fa._2.withPrefix(fa._1).withPrefix(key)

  /**
   * Binds this object, i.e. constructs a concrete value from submitted data.
   *
   * @param data the submitted data
   * @return either a concrete value of type `T` or a set of errors, if the binding failed
   */
  def bind(data: Map[String, String]): Either[Seq[FormError], T] = {
    merge(fieldA.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(
          values(0).asInstanceOf[A]))
      }
    }
  }

  /**
   * Unbinds this object, i.e. transforms a concrete value to plain data.
   *
   * @param value the value to unbind
   * @return either the plain data or a set of errors, if the unbinding failed
   */
  def unbind(value: T): (Map[String, String], Seq[FormError]) = {
    val a = fieldA.unbind(value.productElement(0).asInstanceOf[A])
    (a._1) -> (a._2)
  }

  /**
   * Constructs a new Mapping based on this one, adding a prefix to the key.
   *
   * @param prefix the prefix to add to the key
   * @return the same mapping, with only the key changed
   */
  def withPrefix(prefix: String): Mapping[T] = {
    addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)
  }

  /**
   * Constructs a new Mapping based on this one, by adding new constraints.
   *
   * For example:
   * {{{
   *   import play.api.data._
   *   import validation.Constraints._
   *
   *   Form("phonenumber" -> text.verifying(required) )
   * }}}
   *
   * @param constraints the constraints to add
   * @return the new mapping
   */
  def verifying(addConstraints: Constraint[T]*): Mapping[T] = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  /** Sub-mappings (these can be seen as sub-keys). */
  val mappings: Seq[Mapping[_]] = Seq(this) ++ fieldA.mappings

}

