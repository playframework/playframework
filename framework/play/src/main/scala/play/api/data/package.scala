package play.api

import data.format._
import data.validation._

/**
 * Contains data manipulation helpers (typically HTTP form handling)
 *
 * {{{
 * val taskForm = Form(
 *   of(Task)(
 *     "name" -> text(minLength = 3),
 *     "dueDate" -> date("yyyy-MM-dd"),
 *     "done" -> boolean
 *   )
 * )
 * }}}
 *
 */
package object data {

  /**
   * Create a Mapping of type T.
   *
   * Example:
   * {{{
   *   Form("email" -> of[String])
   * }}}
   *
   * @tparam T The mapping type.
   * @return A mapping for a simple field.
   */
  def of[T](implicit binder: Formatter[T]) = FieldMapping[T]()(binder)

  /**
   * Create a Mapping of type T.
   *
   * Example:
   * {{{
   *   Form(
   *     of(User)("email" -> of[String])
   *   )
   * }}}
   *
   * @tparam T The mapping type.
   * @return A mapping for type T.
   */
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

  def of[T <: Product, A, B, C, D, E](apply: Function5[A, B, C, D, E, T])(a: (String, Mapping[A]), b: (String, Mapping[B]), c: (String, Mapping[C]), d: (String, Mapping[D]), e: (String, Mapping[E])): Mapping[T] = {
    ObjectMapping5(apply, a, b, c, d, e)
  }

  /**
   * Create a Mapping of tuple (A,B).
   *
   * Example:
   * {{{
   *   Form(
   *     of(
   *       "email" -> of[String],
   *       "password" -> of[String]
   *     )
   *   )
   * }}}
   *
   * @return A mapping for a tuple (A,B).
   */
  def of[A, B](a: (String, Mapping[A]), b: (String, Mapping[B])): Mapping[(A, B)] = of((a: A, b: B) => (a, b))(a, b)
  def of[A, B, C](a: (String, Mapping[A]), b: (String, Mapping[B]), c: (String, Mapping[C])): Mapping[(A, B, C)] = of((a: A, b: B, c: C) => (a, b, c))(a, b, c)
  def of[A, B, C, D](a: (String, Mapping[A]), b: (String, Mapping[B]), c: (String, Mapping[C]), d: (String, Mapping[D])): Mapping[(A, B, C, D)] = of((a: A, b: B, c: C, d: D) => (a, b, c, d))(a, b, c, d)

  // --

  import Form._
  import Formats._

  /**
   * Construct a simple mapping for text field.
   *
   * Example:
   * {{{
   *   Form("username" -> text)
   * }}}
   */
  val text: Mapping[String] = of[String]

  /**
   * Construct a simple mapping for required text field.
   *
   * Note that all field are always required to be present in the form unless
   * there are marked as optional explicitely. But a requiredText defines text
   * field that must not be empty, even if present in the form.
   *
   * Example:
   * {{{
   *   Form("username" -> requiredText)
   * }}}
   */
  val requiredText: Mapping[String] = text verifying Constraints.required

  /**
   * Construct a simple mapping for text field.
   *
   * Example:
   * {{{
   *   Form("username" -> text(minLength=3))
   * }}}
   *
   * @param minLength Text min length.
   * @param maxLength Text max length.
   */
  def text(minLength: Int = 0, maxLength: Int = Int.MaxValue): Mapping[String] = (minLength, maxLength) match {
    case (0, Int.MaxValue) => text
    case (min, Int.MaxValue) => text verifying Constraints.minLength(min)
    case (0, max) => text verifying Constraints.maxLength(max)
    case (min, max) => text verifying (Constraints.minLength(min), Constraints.maxLength(max))
  }

  /**
   * Construct a simple mapping for required text field.
   *
   * Example:
   * {{{
   *   Form("username" -> requiredText(minLength=3))
   * }}}
   *
   * @param minLength Text min length.
   * @param maxLength Text max length.
   */
  def requiredText(minLength: Int = 0, maxLength: Int = Int.MaxValue): Mapping[String] = text(minLength, maxLength) verifying Constraints.required

  /**
   * Construct a simple mapping for numeric field.
   *
   * Example:
   * {{{
   *   Form("size" -> number)
   * }}}
   */
  val number: Mapping[Long] = of[Long]

  /**
   * Construct a simple mapping for date field.
   *
   * Example:
   * {{{
   *   Form("birtdate" -> date)
   * }}}
   */
  val date: Mapping[java.util.Date] = of[java.util.Date]

  /**
   * Define a fixed value in a mapping.
   * This mapping will not participate to the binding.
   *
   * @param value As we ignore this parameter in binding/unbinding we have to provide a default value.
   */
  def ignored[A](value: A): Mapping[A] = of(ignoredFormat(value))

  /**
   * Define a optional mapping.
   */
  def optional[A](mapping: Mapping[A]): Mapping[Option[A]] = OptionalMapping(mapping)

  /**
   * Construct a simple mapping for date field.
   *
   * Example:
   * {{{
   *   Form("birtdate" -> date("dd-MM-yyyy"))
   * }}}
   *
   * @param pattern The date pattern as defined in Java SimpleDateFormat
   */
  def date(pattern: String): Mapping[java.util.Date] = of[java.util.Date] as dateFormat(pattern)

  /**
   * Construct a simple mapping for date field (mapped as sql.Date type).
   *
   * Example:
   * {{{
   *   Form("birtdate" -> sqlDate)
   * }}}
   */
  val sqlDate: Mapping[java.sql.Date] = of[java.sql.Date]

  /**
   * Construct a simple mapping for date field (mapped as sql.Date type).
   *
   * Example:
   * {{{
   *   Form("birtdate" -> sqlDate("dd-MM-yyyy"))
   * }}}
   *
   * @param pattern The date pattern as defined in Java SimpleDateFormat
   */
  def sqlDate(pattern: String): Mapping[java.sql.Date] = of[java.sql.Date] as sqlDateFormat(pattern)

  /**
   * Construct a simple mapping for email field.
   *
   * Example:
   * {{{
   *   Form("email" -> email)
   * }}}
   */
  val email: Mapping[String] = of[String] verifying Constraints.pattern(
    """\b[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,4}\b""".r,
    "constraint.email",
    "error.email")

  /**
   * Construct a simple mapping for boolean field (for example checkbox).
   *
   * Example:
   * {{{
   *   Form("accepted" -> boolean)
   * }}}
   */
  val boolean: Mapping[Boolean] = of[Boolean]

}