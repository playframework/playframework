package play.api

import data.format._
import data.validation._

/** Contains data manipulation helpers (typically HTTP form handling)
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

  /** Creates a Mapping of type `T`.
    *
    * For example:
    * {{{
    *   Form("email" -> of[String])
    * }}}
    *
    * @tparam T the mapping type
    * @return a mapping for a simple field
    */
  def of[T](implicit binder: Formatter[T]) = FieldMapping[T]()(binder)

  /** Creates a Mapping of type `T`.
    *
    * For example:
    * {{{
    *   Form(
    *     of(User)("email" -> of[String])
    *   )
    * }}}
    *
    * @tparam T the mapping type
    * @return a mapping for type `T`
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

  /** Creates a Mapping of tuple `(A,B)`.
    *
    * For example:
    * {{{
    *   Form(
    *     of(
    *       "email" -> of[String],
    *       "password" -> of[String]
    *     )
    *   )
    * }}}
    *
    * @return a mapping for a tuple `(A,B)`
    */
  def of[A, B](a: (String, Mapping[A]), b: (String, Mapping[B])): Mapping[(A, B)] = of((a: A, b: B) => (a, b))(a, b)
  def of[A, B, C](a: (String, Mapping[A]), b: (String, Mapping[B]), c: (String, Mapping[C])): Mapping[(A, B, C)] = of((a: A, b: B, c: C) => (a, b, c))(a, b, c)
  def of[A, B, C, D](a: (String, Mapping[A]), b: (String, Mapping[B]), c: (String, Mapping[C]), d: (String, Mapping[D])): Mapping[(A, B, C, D)] = of((a: A, b: B, c: C, d: D) => (a, b, c, d))(a, b, c, d)

  // --

  import Form._
  import Formats._

  /** Constructs a simple mapping for a text field.
    *
    * For example:
    * {{{
    *   Form("username" -> text)
    * }}}
    */
  val text: Mapping[String] = of[String]

  /** Constructs a simple mapping for a text field.
    *
    * For example:
    * {{{
    *   Form("username" -> text(minLength=3))
    * }}}
    *
    * @param minLength minimum text length
    * @param maxLength maximum text length
    */
  def text(minLength: Int = 0, maxLength: Int = Int.MaxValue): Mapping[String] = (minLength, maxLength) match {
    case (0, Int.MaxValue) => text
    case (min, Int.MaxValue) => text verifying Constraints.minLength(min)
    case (0, max) => text verifying Constraints.maxLength(max)
    case (min, max) => text verifying (Constraints.minLength(min), Constraints.maxLength(max))
  }

  /** Constructs a simple mapping for a numeric field.
    *
    * For example:
    * {{{
    *   Form("size" -> number)
    * }}}
    */
  val number: Mapping[Long] = of[Long]

  /** Constructs a simple mapping for a date field.
    *
    * For example:
    * {{{
    *   Form("birthdate" -> date)
    * }}}
    */
  val date: Mapping[java.util.Date] = of[java.util.Date]

  /** Defines a fixed value in a mapping.
    *
    * This mapping will not participate in binding.
    */
  def fixed[A](value: A): Mapping[A] = of(fixedFormat(value))

  /** Defines an optional mapping. */
  def optional[A](mapping: Mapping[A]): Mapping[Option[A]] = OptionalMapping(mapping)

  /** Constructs a simple mapping for a date field.
    *
    * For example:
    * {{{
    *   Form("birthdate" -> date("dd-MM-yyyy"))
    * }}}
    *
    * @param pattern the date pattern, as defined in `java.text.SimpleDateFormat`
    */
  def date(pattern: String): Mapping[java.util.Date] = of[java.util.Date] as dateFormat(pattern)

  /** Constructs a simple mapping for a date field (mapped as `sql.Date type`).
    *
    * For example:
    * {{{
    *   Form("birthdate" -> sqlDate)
    * }}}
    */
  val sqlDate: Mapping[java.sql.Date] = of[java.sql.Date]

  /** Constructs a simple mapping for a date field (mapped as `sql.Date type`).
    *
    * For example:
    * {{{
    *   Form("birthdate" -> sqlDate("dd-MM-yyyy"))
    * }}}
    *
    * @param pattern the date pattern, as defined in `java.text.SimpleDateFormat`
    */
  def sqlDate(pattern: String): Mapping[java.sql.Date] = of[java.sql.Date] as sqlDateFormat(pattern)

  /** Constructs a simple mapping for an e-mail field.
    *
    * For example:
    * {{{
    *   Form("email" -> email)
    * }}}
    */
  val email: Mapping[String] = of[String] verifying Constraints.pattern(
    """\b[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,4}\b""".r,
    "constraint.email",
    "error.email")

  /** Constructs a simple mapping for a Boolean field, such as a check-box.
    *
    * For example:
    * {{{
    *   Form("accepted" -> boolean)
    * }}}
    */
  val boolean: Mapping[Boolean] = of[Boolean]

}