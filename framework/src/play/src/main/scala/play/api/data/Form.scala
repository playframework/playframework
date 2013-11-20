package play.api.data

import scala.language.existentials

import format._
import validation._

/**
 * Helper to manage HTML form description, submission and validation.
 *
 * For example, a form handling a `User` case class submission:
 * {{{
 * import play.api.data._
 * import play.api.data.Forms._
 * import play.api.data.format.Formats._
 *
 * val userForm = Form(
 *   mapping(
 *     "name" -> of[String],
 *     "age" -> of[Int],
 *     "email" -> of[String]
 *   )(User.apply)(User.unapply)
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

  /**
   * Constraints associated with this form, indexed by field name.
   */
  val constraints: Map[String, Seq[(String, Seq[Any])]] = mapping.mappings.map { m =>
    m.key -> m.constraints.collect { case Constraint(Some(name), args) => name -> args }
  }.filterNot(_._2.isEmpty).toMap

  /**
   * Formats associated to this form, indexed by field name. *
   */
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
    newErrors => this.copy(data = data, errors = errors ++ newErrors, value = None),
    value => this.copy(data = data, errors = errors, value = Some(value)))

  /**
   * Binds data to this form, i.e. handles form submission.
   *
   * @param data Json data to submit
   * @return a copy of this form, filled with the new data
   */
  def bind(data: play.api.libs.json.JsValue): Form[T] = bind(FormUtils.fromJson(js = data))

  /**
   * Binds request data to this form, i.e. handles form submission.
   *
   * @return a copy of this form filled with the new data
   */
  def bindFromRequest()(implicit request: play.api.mvc.Request[_]): Form[T] = {
    bindFromRequest {
      (request.body match {
        case body: play.api.mvc.AnyContent if body.asFormUrlEncoded.isDefined => body.asFormUrlEncoded.get
        case body: play.api.mvc.AnyContent if body.asMultipartFormData.isDefined => body.asMultipartFormData.get.asFormUrlEncoded
        case body: play.api.mvc.AnyContent if body.asJson.isDefined => FormUtils.fromJson(js = body.asJson.get).mapValues(Seq(_))
        case body: Map[_, _] => body.asInstanceOf[Map[String, Seq[String]]]
        case body: play.api.mvc.MultipartFormData[_] => body.asFormUrlEncoded
        case body: play.api.libs.json.JsValue => FormUtils.fromJson(js = body).mapValues(Seq(_))
        case _ => Map.empty[String, Seq[String]]
      }) ++ request.queryString
    }
  }

  def bindFromRequest(data: Map[String, Seq[String]]): Form[T] = {
    bind {
      data.foldLeft(Map.empty[String, String]) {
        case (s, (key, values)) if key.endsWith("[]") => s ++ values.zipWithIndex.map { case (v, i) => (key.dropRight(2) + "[" + i + "]") -> v }
        case (s, (key, values)) => s + (key -> values.headOption.getOrElse(""))
      }
    }
  }

  /**
   * Fills this form with a existing value, used for edit forms.
   *
   * @param value an existing value of type `T`, used to fill this form
   * @return a copy of this form filled with the new data
   */
  def fill(value: T): Form[T] = {
    val result = mapping.unbind(value)
    this.copy(data = result._1, value = Some(value))
  }

  /**
   * Fills this form with a existing value, and performs a validation.
   *
   * @param value an existing value of type `T`, used to fill this form
   * @return a copy of this form filled with the new data
   */
  def fillAndValidate(value: T): Form[T] = {
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
  def fold[R](hasErrors: Form[T] => R, success: T => R): R = value match {
    case Some(v) if errors.isEmpty => success(v)
    case _ => hasErrors(this)
  }

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
    this,
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

  /**
   * Returns `true` if there is an error related to this form.
   */
  def hasErrors: Boolean = !errors.isEmpty

  /**
   * Retrieve the first error for this key.
   *
   * @param key field name.
   */
  def error(key: String): Option[FormError] = errors.find(_.key == key)

  /**
   * Retrieve all errors for this key.
   *
   * @param key field name.
   */
  def errors(key: String): Seq[FormError] = errors.filter(_.key == key)

  /**
   * Returns `true` if there is a global error related to this form.
   */
  def hasGlobalErrors: Boolean = !globalErrors.isEmpty

  /**
   * Returns the concrete value, if the submission was a success.
   *
   * Note that this method fails with an Exception if this form as errors.
   */
  def get: T = value.get

  /**
   * Returns the form errors serialized as Json.
   */
  def errorsAsJson(implicit lang: play.api.i18n.Lang): play.api.libs.json.JsValue = {

    import play.api.libs.json._

    Json.toJson(
      errors.groupBy(_.key).mapValues { errors =>
        errors.map(e => play.api.i18n.Messages(e.message, e.args: _*))
      }
    )

  }

  /**
   * Adds an error to this form
   * @param error Error to add
   * @return a copy of this form with the added error
   */
  def withError(error: FormError): Form[T] = this.copy(errors = errors :+ error, value = None)

  /**
   * Convenient overloaded method adding an error to this form
   * @param key Key of the field having the error
   * @param message Error message
   * @param args Error message arguments
   * @return a copy of this form with the added error
   */
  def withError(key: String, message: String, args: Any*): Form[T] = withError(FormError(key, message, args))

  /**
   * Adds a global error to this form
   * @param message Error message
   * @param args Error message arguments
   * @return a copy of this form with the added global error
   */
  def withGlobalError(message: String, args: Any*): Form[T] = withError(FormError("", message, args))

  /**
   * Discards this form’s errors
   * @return a copy of this form without errors
   */
  def discardingErrors: Form[T] = this.copy(errors = Seq.empty)
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
case class Field(private val form: Form[_], name: String, constraints: Seq[(String, Seq[Any])], format: Option[(String, Seq[Any])], errors: Seq[FormError], value: Option[String]) {

  /**
   * The field ID - the same as the field name but with '.' replaced by '_'.
   */
  lazy val id: String = name.replace('.', '_').replace('[', '_').replace(']', '_')

  /**
   * Returns the first error associated with this field, if it exists.
   *
   * @return an error
   */
  lazy val error: Option[FormError] = errors.headOption

  /**
   * Check if this field has errors.
   */
  lazy val hasErrors: Boolean = !errors.isEmpty

  /**
   * Retrieve a field from the same form, using a key relative to this field key.
   *
   * @param key Relative key.
   */
  def apply(key: String): Field = {
    form(Option(name).filterNot(_.isEmpty).map(_ + (if (key(0) == '[') "" else ".")).getOrElse("") + key)
  }

  /**
   * Retrieve available indexes defined for this field (if this field is repeated).
   */
  lazy val indexes: Seq[Int] = {
    RepeatedMapping.indexes(name, form.data)
  }

}

/**
 * Provides a set of operations for creating `Form` values.
 */
object Form {

  /**
   * Creates a new form from a mapping.
   *
   * For example:
   * {{{
   * import play.api.data._
   * import play.api.data.Forms._
   * import play.api.data.format.Formats._
   *
   * val userForm = Form(
   *   tuple(
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
   * import play.api.data._
   * import play.api.data.Forms._
   * import play.api.data.format.Formats._
   *
   * val userForm = Form(
   *   "user" -> tuple(
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

private[data] object FormUtils {

  import play.api.libs.json._

  def fromJson(prefix: String = "", js: JsValue): Map[String, String] = js match {
    case JsObject(fields) => {
      fields.map { case (key, value) => fromJson(Option(prefix).filterNot(_.isEmpty).map(_ + ".").getOrElse("") + key, value) }.foldLeft(Map.empty[String, String])(_ ++ _)
    }
    case JsArray(values) => {
      values.zipWithIndex.map { case (value, i) => fromJson(prefix + "[" + i + "]", value) }.foldLeft(Map.empty[String, String])(_ ++ _)
    }
    case JsNull => Map.empty
    case JsUndefined() => Map.empty
    case JsBoolean(value) => Map(prefix -> value.toString)
    case JsNumber(value) => Map(prefix -> value.toString)
    case JsString(value) => Map(prefix -> value.toString)
  }

}

/**
 * A form error.
 *
 * @param key The error key (should be associated with a field using the same key).
 * @param message The form message (often a simple message key needing to be translated).
 * @param args Arguments used to format the message.
 */
case class FormError(key: String, message: String, args: Seq[Any] = Nil) {

  /**
   * Copy this error with a new Message.
   *
   * @param message The new message.
   */
  def withMessage(message: String): FormError = FormError(key, message)

}

/**
 * A mapping is a two-way binder to handle a form field.
 */
trait Mapping[T] {
  self =>

  /**
   * The field key.
   */
  val key: String

  /**
   * Sub-mappings (these can be seen as sub-keys).
   */
  val mappings: Seq[Mapping[_]]

  /**
   * The Format expected for this field, if it exists.
   */
  val format: Option[(String, Seq[Any])] = None

  /**
   * The constraints associated with this field.
   */
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
   *   import validation.Constraints._
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
   *   import validation.Constraints._
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
   *   import validation.Constraints._
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

  /**
   * Transform this Mapping[T] to a Mapping[B].
   *
   * @tparam B The type of the new mapping.
   * @param f1 Transform value of T to a value of B
   * @param f2 Transform value of B to a value of T
   */
  def transform[B](f1: T => B, f2: B => T): Mapping[B] = WrappedMapping(this, f1, f2)

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
 * A mapping wrapping another existing mapping with transformation functions.
 *
 * @param wrapped Existing wrapped mapping
 * @param f1 Transformation function from A to B
 * @param f2 Transformation function from B to A
 * @param additionalConstraints Additional constraints of type B
 */
case class WrappedMapping[A, B](wrapped: Mapping[A], f1: A => B, f2: B => A, val additionalConstraints: Seq[Constraint[B]] = Nil) extends Mapping[B] {

  /**
   * The field key.
   */
  val key = wrapped.key

  /**
   * Sub-mappings (these can be seen as sub-keys).
   */
  val mappings = wrapped.mappings

  /**
   * The Format expected for this field, if it exists.
   */
  override val format = wrapped.format

  /**
   * The constraints associated with this field.
   */
  val constraints: Seq[Constraint[B]] = wrapped.constraints.map { constraintOfT =>
    Constraint[B](constraintOfT.name, constraintOfT.args) { b =>
      constraintOfT(f2(b))
    }
  } ++ additionalConstraints

  /**
   * Binds this field, i.e. construct a concrete value from submitted data.
   *
   * @param data the submitted data
   * @return either a concrete value of type `B` or a set of errors, if the binding failed
   */
  def bind(data: Map[String, String]): Either[Seq[FormError], B] = {
    wrapped.bind(data).right.map(t => f1(t)).right.flatMap(applyConstraints)
  }

  /**
   * Unbinds this field, i.e. transforms a concrete value to plain data.
   *
   * @param value the value to unbind
   * @return either the plain data or a set of errors, if the unbinding failed
   */
  def unbind(value: B): (Map[String, String], Seq[FormError]) = {
    (wrapped.unbind(f2(value))._1, collectErrors(value))
  }

  /**
   * Constructs a new Mapping based on this one, adding a prefix to the key.
   *
   * @param prefix the prefix to add to the key
   * @return the same mapping, with only the key changed
   */
  def withPrefix(prefix: String): Mapping[B] = {
    copy(wrapped = wrapped.withPrefix(prefix))
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
  def verifying(constraints: Constraint[B]*): Mapping[B] = copy(additionalConstraints = additionalConstraints ++ constraints)

}

/**
 * Provides a set of operations related to `RepeatedMapping` values.
 */
object RepeatedMapping {

  /**
   * Computes the available indexes for the given key in this set of data.
   */
  def indexes(key: String, data: Map[String, String]): Seq[Int] = {
    val KeyPattern = ("^" + java.util.regex.Pattern.quote(key) + """\[(\d+)\].*$""").r
    data.toSeq.collect { case (KeyPattern(index), _) => index.toInt }.sorted.distinct
  }

}

/**
 * A mapping for repeated elements.
 *
 * @param wrapped The wrapped mapping
 */
case class RepeatedMapping[T](wrapped: Mapping[T], val key: String = "", val constraints: Seq[Constraint[List[T]]] = Nil) extends Mapping[List[T]] {

  /**
   * The Format expected for this field, if it exists.
   */
  override val format: Option[(String, Seq[Any])] = wrapped.format

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
  def verifying(addConstraints: Constraint[List[T]]*): Mapping[List[T]] = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  /**
   * Binds this field, i.e. construct a concrete value from submitted data.
   *
   * @param data the submitted data
   * @return either a concrete value of type `List[T]` or a set of errors, if the binding failed
   */
  def bind(data: Map[String, String]): Either[Seq[FormError], List[T]] = {
    val allErrorsOrItems: Seq[Either[Seq[FormError], T]] = RepeatedMapping.indexes(key, data).map(i => wrapped.withPrefix(key + "[" + i + "]").bind(data))
    if (allErrorsOrItems.forall(_.isRight)) {
      Right(allErrorsOrItems.map(_.right.get).toList).right.flatMap(applyConstraints)
    } else {
      Left(allErrorsOrItems.collect { case Left(errors) => errors }.flatten)
    }
  }

  /**
   * Unbinds this field, i.e. transforms a concrete value to plain data.
   *
   * @param value the value to unbind
   * @return either the plain data or a set of errors, if the unbinding failed
   */
  def unbind(value: List[T]): (Map[String, String], Seq[FormError]) = {
    val (datas, errors) = value.zipWithIndex.map { case (t, i) => wrapped.withPrefix(key + "[" + i + "]").unbind(t) }.unzip
    (datas.foldLeft(Map.empty[String, String])(_ ++ _), errors.flatten ++ collectErrors(value))
  }

  /**
   * Constructs a new Mapping based on this one, adding a prefix to the key.
   *
   * @param prefix the prefix to add to the key
   * @return the same mapping, with only the key changed
   */
  def withPrefix(prefix: String): Mapping[List[T]] = {
    addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)
  }

  /**
   * Sub-mappings (these can be seen as sub-keys).
   */
  val mappings: Seq[Mapping[_]] = wrapped.mappings

}

/**
 * A mapping for optional elements
 *
 * @param wrapped the wrapped mapping
 */
case class OptionalMapping[T](wrapped: Mapping[T], val constraints: Seq[Constraint[Option[T]]] = Nil) extends Mapping[Option[T]] {

  override val format: Option[(String, Seq[Any])] = wrapped.format

  /**
   * The field key.
   */
  val key = wrapped.key

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
    data.keys.filter(p => p == key || p.startsWith(key + ".") || p.startsWith(key + "[")).map(k => data.get(k).filterNot(_.isEmpty)).collect { case Some(v) => v }.headOption.map { _ =>
      wrapped.bind(data).right.map(Some(_))
    }.getOrElse {
      Right(None)
    }.right.flatMap(applyConstraints)
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

  /**
   * The Format expected for this field, if it exists.
   */
  override val format: Option[(String, Seq[Any])] = binder.format

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

/**
 * Common helper methods for all object mappings - mappings including several fields.
 */
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
case class ObjectMapping1[R, A1](apply: Function1[A1, R], unapply: Function1[R, Option[(A1)]], f1: (String, Mapping[A1]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

  val field1 = f1._2.withPrefix(f1._1).withPrefix(key)

  def bind(data: Map[String, String]) = {
    merge(field1.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(

          values(0).asInstanceOf[A1]))
      }
    }
  }

  def unbind(value: R) = {
    unapply(value).map { fields =>
      val (v1) = fields
      val a1 = field1.unbind(v1)

      (a1._1) ->
        (a1._2)
    }.getOrElse(Map.empty -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String) = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*) = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings

}

