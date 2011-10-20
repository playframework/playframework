package play.api.data

import format._
import validation._

/**
 * Helper to manage HTML form description, submission and validation.
 *
 * For example, a form handling a User case class submission:
 * {{{
 * val userForm = Form(
 *   of(User)(
 *     "name" -> of[String],
 *     "age" -> of[Int],
 *     "email" -> of[String]
 *   )
 * )
 * }}}
 *
 * @tparam T Type managed by this form.
 * @param mapping The form mapping (describe all form fields)
 * @param date The current form data (used to display the form)
 * @param errors The collection of errors associated with this form.
 * @param value Maybe a concrete value of type T if the form submission was successful.
 */
case class Form[T](mapping: Mapping[T], data: Map[String, String], errors: Seq[FormError], value: Option[T]) {

  /**
   * Constraints associated to this form (indexed by field name)
   */
  val constraints: Map[String, List[(String, Seq[Any])]] = mapping.mappings.map { m =>
    m.key -> m.constraints.collect { case Constraint(Some(name), args) => name -> args }.toSeq
  }.collect {
    case (k, c @ List(_, _*)) => k -> c
  }.toMap

  /**
   * Formats associated to this form (indexed by field name)
   */
  val formats: Map[String, (String, Seq[Any])] = mapping.mappings.map { m =>
    m.key -> m.format
  }.collect {
    case (k, Some(f)) => k -> f
  }.toMap

  /**
   * Bind data to this form (ie. handle form submission).
   *
   * @param data Data to submit
   * @return A copy of this form filled with the new data.
   */
  def bind(data: Map[String, String]): Form[T] = mapping.bind(data).fold(
    errors => this.copy(data = data, errors = errors, value = None),
    value => this.copy(data = data, errors = Nil, value = Some(value)))

  /**
   * Bind data coming from request to this form (ie. handle form submission).
   *
   * @return A copy of this form filled with the new data.
   */
  def bindFromRequest()(implicit request: play.api.mvc.Request[play.api.mvc.AnyContent]): Form[T] = {
    bind(request.body.urlFormEncoded.mapValues(_.headOption.getOrElse("")))
  }

  /**
   * Fill this form with an existing value (used for edition form).
   *
   * @param value Existing value of type T used to fill this form.
   * @return A copy of this form filled with the new data.
   */
  def fill(value: T) = {
    val result = mapping.unbind(value)
    this.copy(data = result._1, errors = result._2, value = Some(value))
  }

  /**
   * Handle form result. Either the form has errors, or the submission was a success and a
   * concrete value is available.
   *
   * Example:
   * {{{
   *   anyForm.bindFromRequest().fold(
   *      f => redisplayForm(f),
   *      t => handleValidFormSubmission(t)
   *   )
   * }}}
   *
   * @tparam R Common result type.
   * @param hasErrors Function to handle form with errors.
   * @param success Function to handle form submission success.
   * @return A result R.
   */
  def fold[R](hasErrors: (Form[T]) => R, success: (T) => R) = value.map(success(_)).getOrElse(hasErrors(this))

  /**
   * Retrieve a field.
   *
   * Example:
   * {{{
   * val usernameField = userForm("username")
   * }}}
   *
   * @param key Field name.
   * @return The field (even of the field does not exist you get a field).
   */
  def apply(key: String): Field = Field(
    key,
    constraints.get(key).getOrElse(Nil),
    formats.get(key),
    errors.collect { case e if e.key == key => e },
    data.get(key))

  /**
   * Retrieve the first global error if exists (ie. an error without any key)
   *
   * @return Maybe an error.
   */
  def globalError: Option[FormError] = globalErrors.headOption

  /**
   * Retrieve all global errors (ie. errors without any key)
   *
   * @return All global errors.
   */
  def globalErrors: Seq[FormError] = errors.filter(_.key.isEmpty)

  /**
   * Apply a function for a field.
   *
   * Example:
   * {{{
   * userForm.forField("username") { field =>
   *   <input type="text" name={field.name} value={field.value.getOrElse("")} />
   * }
   * }}}
   *
   * @tparam R Result type.
   * @param key Field name.
   * @param handler Field handler (transform the field to R).
   */
  def forField[R](key: String)(handler: Field => R): R = handler(this(key))

}

/**
 * A form field.
 *
 * @param name The field name.
 * @param constraints The constraints associated with the field.
 * @param format The format expected for this field.
 * @param errors The errors associated to this field.
 * @param value The field value if any.
 */
case class Field(name: String, constraints: Seq[(String, Seq[Any])], format: Option[(String, Seq[Any])], errors: Seq[FormError], value: Option[String]) {

  /**
   * The field id (same as the field name but with '.' replaced by '_')
   */
  lazy val id: String = name.replace('.', '_')

  /**
   * Get the first error associated to this field if exists.
   *
   * @return Maybe a FormError.
   */
  def error: Option[FormError] = errors.headOption
}

/**
 * This object provides a set of operations to create Form values.
 */
object Form {

  /**
   * Create a new form from a mapping.
   *
   * Example:
   * {{{
   * val userForm = Form(
   *   of(User)(
   *     "name" -> of[String],
   *     "age" -> of[Int],
   *     "email" -> of[String]
   *   )
   * )
   * }}}
   *
   * @param mapping The form mapping.
   * @return A form definition.
   */
  def apply[T](mapping: Mapping[T]): Form[T] = Form(mapping, Map.empty, Nil, None)

  /**
   * Create a new form from a mapping, with a root key.
   *
   * Example:
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
   * @param mapping The root key, form mapping association.
   * @return A form definition.
   */
  def apply[T](mapping: (String, Mapping[T])): Form[T] = Form(mapping._2.withPrefix(mapping._1), Map.empty, Nil, None)
}

/**
 * A Form error.
 *
 * @param key The error key (should be associated with a field using the same key).
 * @param message The form message (often a simple message key needing to be translated).
 * @param args Arguments used to format the message.
 */
case class FormError(key: String, message: String, args: Seq[Any])

/**
 * A mapping is a two way binder to handle a form field.
 */
trait Mapping[T] {

  /**
   * The field key.
   */
  val key: String

  /**
   * Sub mappings (can be seen as sub keys)
   */
  val mappings: Seq[Mapping[_]]

  /**
   * The Format expected for this field if exists.
   */
  val format: Option[(String, Seq[Any])] = None

  /**
   * The constraints associated with this field.
   */
  val constraints: Seq[Constraint[T]]

  /**
   * Bind this field (ie. construct a concrete value from submitted data).
   *
   * @param data The submitted data.
   * @return Either a concrete value of type T or a set of error if the binding failed.
   */
  def bind(data: Map[String, String]): Either[Seq[FormError], T]

  /**
   * Unbind this field (ie. transform a concrete value to plain data)
   *
   * @param value The value to unbind.
   * @return Either the plain data or a set of error if the unbinding failed.
   */
  def unbind(value: T): (Map[String, String], Seq[FormError])

  /**
   * Construct a new Mapping based on this one, adding a prefix to the key.
   *
   * @param prefix The prefix to add to the key.
   * @return The same mapping, only the key changed.
   */
  def withPrefix(prefix: String): Mapping[T]

  /**
   * Construct a new Mapping based on this one, by adding new constraints.
   *
   * Example:
   * {{{
   *   Form("phonenumber" -> text verifying required)
   * }}}
   *
   * @param constraints The constraints to add.
   * @return The new mapping.
   */
  def verifying(constraints: Constraint[T]*): Mapping[T]

  /**
   * Construct a new Mapping based on this one, by adding a new adhoc constraint.
   *
   * Example:
   * {{{
   *   Form("phonenumber" -> text verifying {_.grouped(2).size == 5})
   * }}}
   *
   * @param constraint Function describing the constraint. Return false if failed.
   * @return The new mapping.
   */
  def verifying(constraint: (T => Boolean)): Mapping[T] = verifying("error.unknown", constraint)

  /**
   * Construct a new Mapping based on this one, by adding a new adhoc constraint.
   *
   * Example:
   * {{{
   *   Form("phonenumber" -> text verifying("Bad phone number", {_.grouped(2).size == 5}))
   * }}}
   *
   * @param error The error message used if the constraint fail.
   * @param constraint Function describing the constraint. Return false if failed.
   * @return The new mapping.
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
 * A mapping for a single field.
 *
 * @param key The field key
 * @param constraints The constraints associated with this field.
 */
case class FieldMapping[T](val key: String = "", val constraints: Seq[Constraint[T]] = Nil)(implicit val binder: Formatter[T]) extends Mapping[T] {

  override val format: Option[(String, Seq[Any])] = binder.format

  /**
   * Construct a new Mapping based on this one, by adding new constraints.
   *
   * Example:
   * {{{
   *   Form("phonenumber" -> text verifying required)
   * }}}
   *
   * @param constraints The constraints to add.
   * @return The new mapping.
   */
  def verifying(addConstraints: Constraint[T]*): Mapping[T] = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  /**
   * Change the binder used to handle this field.
   *
   * @param binder The new binder to use.
   * @return Same mapping with a new binder.
   */
  def as(binder: Formatter[T]): Mapping[T] = {
    this.copy()(binder)
  }

  /**
   * Bind this field (ie. construct a concrete value from submitted data).
   *
   * @param data The submitted data.
   * @return Either a concrete value of type T or a set of error if the binding failed.
   */
  def bind(data: Map[String, String]): Either[Seq[FormError], T] = {
    binder.bind(key, data).right.flatMap { applyConstraints(_) }
  }

  /**
   * Unbind this field (ie. transform a concrete value to plain data)
   *
   * @param value The value to unbind.
   * @return Either the plain data or a set of error if the unbinding failed.
   */
  def unbind(value: T): (Map[String, String], Seq[FormError]) = {
    binder.unbind(key, value) -> collectErrors(value)
  }

  /**
   * Construct a new Mapping based on this one, adding a prefix to the key.
   *
   * @param prefix The prefix to add to the key.
   * @return The same mapping, only the key changed.
   */
  def withPrefix(prefix: String): Mapping[T] = {
    addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)
  }

  /**
   * Sub mappings (can be seen as sub keys)
   */
  val mappings: Seq[Mapping[_]] = Seq(this)

}

/**
 * Common helper for all object mappings (mappings including several fields)
 */
trait ObjectMapping {

  /**
   * Merge the result of 2 bindings.
   * @see bind()
   */
  def merge2(a: Either[Seq[FormError], Seq[Any]], b: Either[Seq[FormError], Seq[Any]]): Either[Seq[FormError], Seq[Any]] = (a, b) match {
    case (Left(errorsA), Left(errorsB)) => Left(errorsA ++ errorsB)
    case (Left(errorsA), Right(_)) => Left(errorsA)
    case (Right(_), Left(errorsB)) => Left(errorsB)
    case (Right(a), Right(b)) => Right(a ++ b)
  }

  /**
   * Merge the result of n bindings.
   * @see bind()
   */
  def merge(results: Either[Seq[FormError], Any]*): Either[Seq[FormError], Seq[Any]] = {
    val all: Seq[Either[Seq[FormError], Seq[Any]]] = results.map(_.right.map(Seq(_)))
    all.fold(Right(Nil)) { (s, i) => merge2(s, i) }
  }

}

/**
 * Represents an object binding (ie. a binding for several fields)
 * This one is used for object with 1 field. Other versions exists (ObjectMapping2, ObjectMapping3, ...)
 *
 * @tparam T The complex object type.
 * @tparam A First field type.
 * @param apply Constructor function. Create a instance of T using field A.
 * @param fa Mapping for field A.
 * @param constraints Constraints associated to this mapping.
 */
case class ObjectMapping1[T <: Product, A](apply: Function1[A, T], fa: (String, Mapping[A]), val key: String = "", val constraints: Seq[Constraint[T]] = Nil) extends Mapping[T] with ObjectMapping {

  val fieldA = fa._2.withPrefix(fa._1).withPrefix(key)

  /**
   * Bind this object (ie. construct a concrete value from submitted data).
   *
   * @param data The submitted data.
   * @return Either a concrete value of type T or a set of error if the binding failed.
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
   * Unbind this object (ie. transform a concrete value to plain data)
   *
   * @param value The value to unbind.
   * @return Either the plain data or a set of error if the unbinding failed.
   */
  def unbind(value: T): (Map[String, String], Seq[FormError]) = {
    val a = fieldA.unbind(value.productElement(0).asInstanceOf[A])
    (a._1) -> (a._2)
  }

  /**
   * Construct a new Mapping based on this one, adding a prefix to the key.
   *
   * @param prefix The prefix to add to the key.
   * @return The same mapping, only the key changed.
   */
  def withPrefix(prefix: String): Mapping[T] = {
    addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)
  }

  /**
   * Construct a new Mapping based on this one, by adding new constraints.
   *
   * Example:
   * {{{
   *   Form("phonenumber" -> text verifying required)
   * }}}
   *
   * @param constraints The constraints to add.
   * @return The new mapping.
   */
  def verifying(addConstraints: Constraint[T]*): Mapping[T] = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  /**
   * Sub mappings (can be seen as sub keys)
   */
  val mappings: Seq[Mapping[_]] = Seq(this) ++ fieldA.mappings

}

