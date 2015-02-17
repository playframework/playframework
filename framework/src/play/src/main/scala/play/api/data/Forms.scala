/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.data

import play.api.data.format._
import play.api.data.validation._

import scala.annotation._

/**
 * Contains data manipulation helpers (typically HTTP form handling)
 *
 * {{{
 * import play.api.data._
 * import play.api.data.Forms._
 *
 * val taskForm = Form(
 *   of(Task.apply _, Task.unapply _)(
 *     "name" -> text(minLength = 3),
 *     "dueDate" -> date("yyyy-MM-dd"),
 *     "done" -> boolean
 *   )
 * )
 * }}}
 *
 */
object Forms {

  /**
   * Creates a Mapping of type `T`.
   *
   * For example:
   * {{{
   * Form("email" -> of[String])
   * }}}
   *
   * @tparam T the mapping type
   * @return a mapping for a simple field
   */
  def of[T](implicit binder: Formatter[T]): FieldMapping[T] = FieldMapping[T]()(binder)

  /**
   * Creates a Mapping of type `T`.
   *
   * For example:
   * {{{
   * Form(
   *   mapping(
   *     "email" -> of[String]
   *   )(User.apply, User.unapply)
   * )
   * }}}
   *
   * @tparam T the mapped type
   * @param apply A function able to create a value of T from a value of A1 (If T is case class you can use its own apply function)
   * @param unapply A function able to create A1 from a value of T (If T is a case class you can use its own unapply function)
   * @return a mapping for type `T`
   */
  def mapping[R, A1](a1: (String, Mapping[A1]))(apply: Function1[A1, R])(unapply: Function1[R, Option[(A1)]]): Mapping[R] = {
    new ObjectMapping1(apply, unapply, a1)
  }

  def mapping[R, A1, A2](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]))(apply: Function2[A1, A2, R])(unapply: Function1[R, Option[(A1, A2)]]): Mapping[R] = {
    new ObjectMapping2(apply, unapply, a1, a2)
  }

  def mapping[R, A1, A2, A3](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]))(apply: Function3[A1, A2, A3, R])(unapply: Function1[R, Option[(A1, A2, A3)]]): Mapping[R] = {
    new ObjectMapping3(apply, unapply, a1, a2, a3)
  }

  def mapping[R, A1, A2, A3, A4](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]))(apply: Function4[A1, A2, A3, A4, R])(unapply: Function1[R, Option[(A1, A2, A3, A4)]]): Mapping[R] = {
    new ObjectMapping4(apply, unapply, a1, a2, a3, a4)
  }

  def mapping[R, A1, A2, A3, A4, A5](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]))(apply: Function5[A1, A2, A3, A4, A5, R])(unapply: Function1[R, Option[(A1, A2, A3, A4, A5)]]): Mapping[R] = {
    new ObjectMapping5(apply, unapply, a1, a2, a3, a4, a5)
  }

  def mapping[R, A1, A2, A3, A4, A5, A6](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]))(apply: Function6[A1, A2, A3, A4, A5, A6, R])(unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6)]]): Mapping[R] = {
    new ObjectMapping6(apply, unapply, a1, a2, a3, a4, a5, a6)
  }

  def mapping[R, A1, A2, A3, A4, A5, A6, A7](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]))(apply: Function7[A1, A2, A3, A4, A5, A6, A7, R])(unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7)]]): Mapping[R] = {
    new ObjectMapping7(apply, unapply, a1, a2, a3, a4, a5, a6, a7)
  }

  def mapping[R, A1, A2, A3, A4, A5, A6, A7, A8](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]))(apply: Function8[A1, A2, A3, A4, A5, A6, A7, A8, R])(unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8)]]): Mapping[R] = {
    new ObjectMapping8(apply, unapply, a1, a2, a3, a4, a5, a6, a7, a8)
  }

  def mapping[R, A1, A2, A3, A4, A5, A6, A7, A8, A9](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9]))(apply: Function9[A1, A2, A3, A4, A5, A6, A7, A8, A9, R])(unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9)]]): Mapping[R] = {
    new ObjectMapping9(apply, unapply, a1, a2, a3, a4, a5, a6, a7, a8, a9)
  }

  def mapping[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9]), a10: (String, Mapping[A10]))(apply: Function10[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, R])(unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10)]]): Mapping[R] = {
    new ObjectMapping10(apply, unapply, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10)
  }

  def mapping[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9]), a10: (String, Mapping[A10]), a11: (String, Mapping[A11]))(apply: Function11[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, R])(unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11)]]): Mapping[R] = {
    new ObjectMapping11(apply, unapply, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11)
  }

  def mapping[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9]), a10: (String, Mapping[A10]), a11: (String, Mapping[A11]), a12: (String, Mapping[A12]))(apply: Function12[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, R])(unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12)]]): Mapping[R] = {
    new ObjectMapping12(apply, unapply, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12)
  }

  def mapping[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9]), a10: (String, Mapping[A10]), a11: (String, Mapping[A11]), a12: (String, Mapping[A12]), a13: (String, Mapping[A13]))(apply: Function13[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, R])(unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13)]]): Mapping[R] = {
    new ObjectMapping13(apply, unapply, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13)
  }

  def mapping[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9]), a10: (String, Mapping[A10]), a11: (String, Mapping[A11]), a12: (String, Mapping[A12]), a13: (String, Mapping[A13]), a14: (String, Mapping[A14]))(apply: Function14[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, R])(unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14)]]): Mapping[R] = {
    new ObjectMapping14(apply, unapply, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14)
  }

  def mapping[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9]), a10: (String, Mapping[A10]), a11: (String, Mapping[A11]), a12: (String, Mapping[A12]), a13: (String, Mapping[A13]), a14: (String, Mapping[A14]), a15: (String, Mapping[A15]))(apply: Function15[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, R])(unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15)]]): Mapping[R] = {
    new ObjectMapping15(apply, unapply, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15)
  }

  def mapping[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9]), a10: (String, Mapping[A10]), a11: (String, Mapping[A11]), a12: (String, Mapping[A12]), a13: (String, Mapping[A13]), a14: (String, Mapping[A14]), a15: (String, Mapping[A15]), a16: (String, Mapping[A16]))(apply: Function16[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, R])(unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16)]]): Mapping[R] = {
    new ObjectMapping16(apply, unapply, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16)
  }

  def mapping[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9]), a10: (String, Mapping[A10]), a11: (String, Mapping[A11]), a12: (String, Mapping[A12]), a13: (String, Mapping[A13]), a14: (String, Mapping[A14]), a15: (String, Mapping[A15]), a16: (String, Mapping[A16]), a17: (String, Mapping[A17]))(apply: Function17[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, R])(unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17)]]): Mapping[R] = {
    new ObjectMapping17(apply, unapply, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17)
  }

  def mapping[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9]), a10: (String, Mapping[A10]), a11: (String, Mapping[A11]), a12: (String, Mapping[A12]), a13: (String, Mapping[A13]), a14: (String, Mapping[A14]), a15: (String, Mapping[A15]), a16: (String, Mapping[A16]), a17: (String, Mapping[A17]), a18: (String, Mapping[A18]))(apply: Function18[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, R])(unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18)]]): Mapping[R] = {
    new ObjectMapping18(apply, unapply, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18)
  }

  def mapping[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9]), a10: (String, Mapping[A10]), a11: (String, Mapping[A11]), a12: (String, Mapping[A12]), a13: (String, Mapping[A13]), a14: (String, Mapping[A14]), a15: (String, Mapping[A15]), a16: (String, Mapping[A16]), a17: (String, Mapping[A17]), a18: (String, Mapping[A18]), a19: (String, Mapping[A19]))(apply: Function19[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, R])(unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19)]]): Mapping[R] = {
    new ObjectMapping19(apply, unapply, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19)
  }

  def mapping[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9]), a10: (String, Mapping[A10]), a11: (String, Mapping[A11]), a12: (String, Mapping[A12]), a13: (String, Mapping[A13]), a14: (String, Mapping[A14]), a15: (String, Mapping[A15]), a16: (String, Mapping[A16]), a17: (String, Mapping[A17]), a18: (String, Mapping[A18]), a19: (String, Mapping[A19]), a20: (String, Mapping[A20]))(apply: Function20[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, R])(unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20)]]): Mapping[R] = {
    new ObjectMapping20(apply, unapply, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20)
  }

  def mapping[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9]), a10: (String, Mapping[A10]), a11: (String, Mapping[A11]), a12: (String, Mapping[A12]), a13: (String, Mapping[A13]), a14: (String, Mapping[A14]), a15: (String, Mapping[A15]), a16: (String, Mapping[A16]), a17: (String, Mapping[A17]), a18: (String, Mapping[A18]), a19: (String, Mapping[A19]), a20: (String, Mapping[A20]), a21: (String, Mapping[A21]))(apply: Function21[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, R])(unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21)]]): Mapping[R] = {
    new ObjectMapping21(apply, unapply, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21)
  }

  def mapping[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9]), a10: (String, Mapping[A10]), a11: (String, Mapping[A11]), a12: (String, Mapping[A12]), a13: (String, Mapping[A13]), a14: (String, Mapping[A14]), a15: (String, Mapping[A15]), a16: (String, Mapping[A16]), a17: (String, Mapping[A17]), a18: (String, Mapping[A18]), a19: (String, Mapping[A19]), a20: (String, Mapping[A20]), a21: (String, Mapping[A21]), a22: (String, Mapping[A22]))(apply: Function22[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, R])(unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22)]]): Mapping[R] = {
    new ObjectMapping22(apply, unapply, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22)
  }

  /**
   * Creates a Mapping for a single value.
   *
   * For example:
   * {{{
   * Form(
   *   single(
   *     "email" -> email
   *   )
   * )
   * }}}
   *
   * @return a mapping for a type A1
   */
  def single[A1](a1: (String, Mapping[A1])): Mapping[(A1)] = mapping(a1)((a1: A1) => (a1))((t: (A1)) => Some(t))

  /**
   * Creates a Mapping of tuple `(A,B)`.
   *
   * For example:
   * {{{
   * Form(
   *   tuple(
   *     "email" -> email,
   *     "password" -> nonEmptyText
   *   )
   * )
   * }}}
   *
   * @return a mapping for a tuple `(A,B)`
   */
  def tuple[A1, A2](a1: (String, Mapping[A1]), a2: (String, Mapping[A2])): Mapping[(A1, A2)] = mapping(a1, a2)((a1: A1, a2: A2) => (a1, a2))((t: (A1, A2)) => Some(t))

  def tuple[A1, A2, A3](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3])): Mapping[(A1, A2, A3)] = mapping(a1, a2, a3)((a1: A1, a2: A2, a3: A3) => (a1, a2, a3))((t: (A1, A2, A3)) => Some(t))

  def tuple[A1, A2, A3, A4](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4])): Mapping[(A1, A2, A3, A4)] = mapping(a1, a2, a3, a4)((a1: A1, a2: A2, a3: A3, a4: A4) => (a1, a2, a3, a4))((t: (A1, A2, A3, A4)) => Some(t))

  def tuple[A1, A2, A3, A4, A5](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5])): Mapping[(A1, A2, A3, A4, A5)] = mapping(a1, a2, a3, a4, a5)((a1: A1, a2: A2, a3: A3, a4: A4, a5: A5) => (a1, a2, a3, a4, a5))((t: (A1, A2, A3, A4, A5)) => Some(t))

  def tuple[A1, A2, A3, A4, A5, A6](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6])): Mapping[(A1, A2, A3, A4, A5, A6)] = mapping(a1, a2, a3, a4, a5, a6)((a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6) => (a1, a2, a3, a4, a5, a6))((t: (A1, A2, A3, A4, A5, A6)) => Some(t))

  def tuple[A1, A2, A3, A4, A5, A6, A7](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7])): Mapping[(A1, A2, A3, A4, A5, A6, A7)] = mapping(a1, a2, a3, a4, a5, a6, a7)((a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7) => (a1, a2, a3, a4, a5, a6, a7))((t: (A1, A2, A3, A4, A5, A6, A7)) => Some(t))

  def tuple[A1, A2, A3, A4, A5, A6, A7, A8](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8])): Mapping[(A1, A2, A3, A4, A5, A6, A7, A8)] = mapping(a1, a2, a3, a4, a5, a6, a7, a8)((a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8) => (a1, a2, a3, a4, a5, a6, a7, a8))((t: (A1, A2, A3, A4, A5, A6, A7, A8)) => Some(t))

  def tuple[A1, A2, A3, A4, A5, A6, A7, A8, A9](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9])): Mapping[(A1, A2, A3, A4, A5, A6, A7, A8, A9)] = mapping(a1, a2, a3, a4, a5, a6, a7, a8, a9)((a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9) => (a1, a2, a3, a4, a5, a6, a7, a8, a9))((t: (A1, A2, A3, A4, A5, A6, A7, A8, A9)) => Some(t))

  def tuple[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9]), a10: (String, Mapping[A10])): Mapping[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10)] = mapping(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10)((a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10))((t: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10)) => Some(t))

  def tuple[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9]), a10: (String, Mapping[A10]), a11: (String, Mapping[A11])): Mapping[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11)] = mapping(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11)((a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11))((t: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11)) => Some(t))

  def tuple[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9]), a10: (String, Mapping[A10]), a11: (String, Mapping[A11]), a12: (String, Mapping[A12])): Mapping[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12)] = mapping(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12)((a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12))((t: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12)) => Some(t))

  def tuple[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9]), a10: (String, Mapping[A10]), a11: (String, Mapping[A11]), a12: (String, Mapping[A12]), a13: (String, Mapping[A13])): Mapping[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13)] = mapping(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13)((a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13))((t: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13)) => Some(t))

  def tuple[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9]), a10: (String, Mapping[A10]), a11: (String, Mapping[A11]), a12: (String, Mapping[A12]), a13: (String, Mapping[A13]), a14: (String, Mapping[A14])): Mapping[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14)] = mapping(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14)((a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14))((t: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14)) => Some(t))

  def tuple[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9]), a10: (String, Mapping[A10]), a11: (String, Mapping[A11]), a12: (String, Mapping[A12]), a13: (String, Mapping[A13]), a14: (String, Mapping[A14]), a15: (String, Mapping[A15])): Mapping[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15)] = mapping(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15)((a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15))((t: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15)) => Some(t))

  def tuple[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9]), a10: (String, Mapping[A10]), a11: (String, Mapping[A11]), a12: (String, Mapping[A12]), a13: (String, Mapping[A13]), a14: (String, Mapping[A14]), a15: (String, Mapping[A15]), a16: (String, Mapping[A16])): Mapping[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16)] = mapping(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16)((a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16))((t: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16)) => Some(t))

  def tuple[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9]), a10: (String, Mapping[A10]), a11: (String, Mapping[A11]), a12: (String, Mapping[A12]), a13: (String, Mapping[A13]), a14: (String, Mapping[A14]), a15: (String, Mapping[A15]), a16: (String, Mapping[A16]), a17: (String, Mapping[A17])): Mapping[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17)] = mapping(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17)((a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17))((t: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17)) => Some(t))

  def tuple[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9]), a10: (String, Mapping[A10]), a11: (String, Mapping[A11]), a12: (String, Mapping[A12]), a13: (String, Mapping[A13]), a14: (String, Mapping[A14]), a15: (String, Mapping[A15]), a16: (String, Mapping[A16]), a17: (String, Mapping[A17]), a18: (String, Mapping[A18])): Mapping[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18)] = mapping(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18)((a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18))((t: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18)) => Some(t))

  def tuple[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9]), a10: (String, Mapping[A10]), a11: (String, Mapping[A11]), a12: (String, Mapping[A12]), a13: (String, Mapping[A13]), a14: (String, Mapping[A14]), a15: (String, Mapping[A15]), a16: (String, Mapping[A16]), a17: (String, Mapping[A17]), a18: (String, Mapping[A18]), a19: (String, Mapping[A19])): Mapping[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19)] = mapping(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19)((a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18, a19: A19) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19))((t: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19)) => Some(t))

  def tuple[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9]), a10: (String, Mapping[A10]), a11: (String, Mapping[A11]), a12: (String, Mapping[A12]), a13: (String, Mapping[A13]), a14: (String, Mapping[A14]), a15: (String, Mapping[A15]), a16: (String, Mapping[A16]), a17: (String, Mapping[A17]), a18: (String, Mapping[A18]), a19: (String, Mapping[A19]), a20: (String, Mapping[A20])): Mapping[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20)] = mapping(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20)((a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18, a19: A19, a20: A20) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20))((t: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20)) => Some(t))

  def tuple[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9]), a10: (String, Mapping[A10]), a11: (String, Mapping[A11]), a12: (String, Mapping[A12]), a13: (String, Mapping[A13]), a14: (String, Mapping[A14]), a15: (String, Mapping[A15]), a16: (String, Mapping[A16]), a17: (String, Mapping[A17]), a18: (String, Mapping[A18]), a19: (String, Mapping[A19]), a20: (String, Mapping[A20]), a21: (String, Mapping[A21])): Mapping[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21)] = mapping(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21)((a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18, a19: A19, a20: A20, a21: A21) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21))((t: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21)) => Some(t))

  def tuple[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22](a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9]), a10: (String, Mapping[A10]), a11: (String, Mapping[A11]), a12: (String, Mapping[A12]), a13: (String, Mapping[A13]), a14: (String, Mapping[A14]), a15: (String, Mapping[A15]), a16: (String, Mapping[A16]), a17: (String, Mapping[A17]), a18: (String, Mapping[A18]), a19: (String, Mapping[A19]), a20: (String, Mapping[A20]), a21: (String, Mapping[A21]), a22: (String, Mapping[A22])): Mapping[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22)] = mapping(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22)((a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18, a19: A19, a20: A20, a21: A21, a22: A22) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22))((t: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22)) => Some(t))

  // --

  import Formats._

  /**
   * Constructs a simple mapping for a text field.
   *
   * For example:
   * {{{
   * Form("username" -> text)
   * }}}
   */
  val text: Mapping[String] = of[String]

  /**
   * Constructs a simple mapping for required text field.
   *
   * Note that all field are always required to be present in the form unless
   * there are marked as optional explicitely. But a nonEmptyText defines text
   * field that must not be empty, even if present in the form.
   *
   * Example:
   * {{{
   * Form("username" -> nonEmptyText)
   * }}}
   */
  val nonEmptyText: Mapping[String] = text verifying Constraints.nonEmpty

  /**
   * Constructs a simple mapping for a text field.
   *
   * For example:
   * {{{
   * Form("username" -> text(minLength=3))
   * }}}
   *
   * @param minLength minimum text length
   * @param maxLength maximum text length
   */
  def text(minLength: Int = 0, maxLength: Int = Int.MaxValue): Mapping[String] = (minLength, maxLength) match {
    case (min, Int.MaxValue) => text verifying Constraints.minLength(min)
    case (0, max) => text verifying Constraints.maxLength(max)
    case (min, max) => text verifying (Constraints.minLength(min), Constraints.maxLength(max))
  }

  /**
   * Constructs a simple mapping for required text field.
   *
   * Example:
   * {{{
   * Form("username" -> nonEmptyText(minLength=3))
   * }}}
   *
   * @param minLength Text min length.
   * @param maxLength Text max length.
   */
  def nonEmptyText(minLength: Int = 0, maxLength: Int = Int.MaxValue): Mapping[String] = text(minLength, maxLength) verifying Constraints.nonEmpty

  /**
   * Constructs a simple mapping for a numeric field.
   *
   * For example:
   * {{{
   * Form("size" -> number)
   * }}}
   */
  val number: Mapping[Int] = number()

  /**
   * Constructs a simple mapping for a numeric field (using a Long type behind).
   *
   * For example:
   * {{{
   * Form("size" -> longNumber)
   * }}}
   */
  val longNumber: Mapping[Long] = longNumber()

  /**
   * Constructs a simple mapping for a numeric field (using a Short type behind).
   *
   * For example:
   * {{{
   * Form("size" -> shortNumber)
   * }}}
   */
  val shortNumber: Mapping[Short] = shortNumber()

  /**
   * Constructs a simple mapping for a numeric field (using a Byte type behind).
   *
   * For example:
   * {{{
   * Form("size" -> byteNumber)
   * }}}
   */
  val byteNumber: Mapping[Byte] = byteNumber()

  /**
   * Constructs a simple mapping for a numeric field.
   *
   * For example:
   * {{{
   * Form("size" -> number(min=0, max=100))
   * }}}
   *
   * @param min minimum value
   * @param max maximum value
   * @param strict should it be a strict comparison
   */
  def number(min: Int = Int.MinValue, max: Int = Int.MaxValue, strict: Boolean = false): Mapping[Int] =
    numberMapping[Int](Int.MinValue, Int.MaxValue, min, max, strict)

  /**
   * Constructs a simple mapping for a numeric field (using a Long type behind).
   *
   * For example:
   * {{{
   * Form("size" -> longNumber(min=0, max=100))
   * }}}
   *
   * @param min minimum value
   * @param max maximum value
   * @param strict should it be a strict comparison
   */
  def longNumber(min: Long = Long.MinValue, max: Long = Long.MaxValue, strict: Boolean = false): Mapping[Long] =
    numberMapping[Long](Long.MinValue, Long.MaxValue, min, max, strict)

  /**
   * Constructs a simple mapping for a numeric field (using a Short type behind).
   *
   * For example:
   * {{{
   * Form("size" -> shortNumber(min=0, max=100))
   * }}}
   *
   * @param min minimum value
   * @param max maximum value
   * @param strict should it be a strict comparison
   */
  def shortNumber(min: Short = Short.MinValue, max: Short = Short.MaxValue, strict: Boolean = false): Mapping[Short] =
    numberMapping[Short](Short.MinValue, Short.MaxValue, min, max, strict)

  /**
   * Constructs a simple mapping for a numeric field (using a Short type behind).
   *
   * For example:
   * {{{
   * Form("size" -> byteNumber(min=0, max=100))
   * }}}
   *
   * @param min minimum value
   * @param max maximum value
   * @param strict should it be a strict comparison
   */
  def byteNumber(min: Byte = Byte.MinValue, max: Byte = Byte.MaxValue, strict: Boolean = false): Mapping[Byte] =
    numberMapping[Byte](Byte.MinValue, Byte.MaxValue, min, max, strict)

  @inline private def numberMapping[N: Numeric: Formatter](
    typeMin: N, typeMax: N, min: N, max: N, strict: Boolean): Mapping[N] = {
    val number = of[N]
    if (min == typeMin && max == typeMax) {
      number
    } else if (min == typeMin) {
      number verifying Constraints.max(max, strict)
    } else if (max == typeMax) {
      number verifying Constraints.min(min, strict)
    } else {
      number verifying (Constraints.min(min, strict), Constraints.max(max, strict))
    }
  }

  /**
   * Constructs a simple mapping for a BigDecimal field.
   *
   * For example:
   * {{{
   * Form("montant" -> bigDecimal)
   * }}}
   */
  val bigDecimal: Mapping[BigDecimal] = of[BigDecimal]

  /**
   * Constructs a mapping for a BigDecimal field.
   *
   * For example:
   * {{{
   * Form("montant" -> bigDecimal(10, 2))
   * }}}
   * @param precision The maximun total number of digits (including decimals)
   * @param scale The maximun number of decimals
   */
  def bigDecimal(precision: Int, scale: Int): Mapping[BigDecimal] = of[BigDecimal] as bigDecimalFormat(Some((precision, scale)))

  /**
   * Constructs a simple mapping for a date field.
   *
   * For example:
   * {{{
   * Form("birthdate" -> date)
   * }}}
   */
  val date: Mapping[java.util.Date] = of[java.util.Date]

  /**
   * Constructs a simple mapping for a UUID field.
   *
   * For example:
   * {{{
   * Form("id" -> uuid)
   * }}}
   */
  val uuid: Mapping[java.util.UUID] = of[java.util.UUID]

  /**
   * Define a fixed value in a mapping.
   * This mapping will not participate to the binding.
   *
   * @param value As we ignore this parameter in binding/unbinding we have to provide a default value.
   */
  def ignored[A](value: A): Mapping[A] = of(ignoredFormat(value))

  /**
   * Defines an optional mapping.
   *
   * {{{
   * Form(
   *   "name" -> optional(text)
   * )
   * }}}
   *
   * @param mapping The mapping to make optional.
   */
  def optional[A](mapping: Mapping[A]): Mapping[Option[A]] = OptionalMapping(mapping)

  /**
   * Defines an default mapping, if the parameter is not present, provide a default value.
   *
   * {{{
   * Form(
   *   "name" -> default(text, "The default text")
   * )
   * }}}
   *
   * @param mapping The mapping to make optional.
   * @param value The default value when mapping and the field is not present.
   */
  def default[A](mapping: Mapping[A], value: A): Mapping[A] = OptionalMapping(mapping).transform(_.getOrElse(value), Some(_))

  /**
   * Defines a repeated mapping.
   * {{{
   * Form(
   *   "name" -> list(text)
   * )
   * }}}
   *
   * @param mapping The mapping to make repeated.
   */
  def list[A](mapping: Mapping[A]): Mapping[List[A]] = RepeatedMapping(mapping)

  /**
   * Defines a repeated mapping.
   * {{{
   * Form(
   *   "name" -> seq(text)
   * )
   * }}}
   *
   * @param mapping The mapping to make repeated.
   */
  def seq[A](mapping: Mapping[A]): Mapping[Seq[A]] = RepeatedMapping(mapping).transform(_.toSeq, _.toList)

  /**
   * Defines a repeated mapping with the Set semantic.
   * {{{
   * Form(
   *   "name" -> set(text)
   * )
   * }}}
   *
   * @param mapping The mapping to make repeated.
   */
  def set[A](mapping: Mapping[A]): Mapping[Set[A]] = RepeatedMapping(mapping).transform(_.toSet, _.toList)

  /**
   * Constructs a simple mapping for a date field.
   *
   * For example:
   * {{{
   *   Form("birthdate" -> date("dd-MM-yyyy"))
   * }}}
   *
   * @param pattern the date pattern, as defined in `java.text.SimpleDateFormat`
   * @param timeZone the `java.util.TimeZone` to use for parsing and formatting
   */
  def date(pattern: String, timeZone: java.util.TimeZone = java.util.TimeZone.getDefault): Mapping[java.util.Date] = of[java.util.Date] as dateFormat(pattern, timeZone)

  /**
   * Constructs a simple mapping for a date field (mapped as `sql.Date type`).
   *
   * For example:
   * {{{
   *   Form("birthdate" -> sqlDate)
   * }}}
   */
  val sqlDate: Mapping[java.sql.Date] = of[java.sql.Date]

  /**
   * Constructs a simple mapping for a date field (mapped as `sql.Date type`).
   *
   * For example:
   * {{{
   *   Form("birthdate" -> sqlDate("dd-MM-yyyy"))
   * }}}
   *
   * @param pattern the date pattern, as defined in `java.text.SimpleDateFormat`
   * @param timeZone the `java.util.TimeZone` to use for parsing and formatting
   */
  def sqlDate(pattern: String, timeZone: java.util.TimeZone = java.util.TimeZone.getDefault): Mapping[java.sql.Date] = of[java.sql.Date] as sqlDateFormat(pattern, timeZone)

  /**
   * Constructs a simple mapping for a date field (mapped as `org.joda.time.DateTime type`).
   *
   * For example:
   * {{{
   *   Form("birthdate" -> jodaDate)
   * }}}
   */
  val jodaDate: Mapping[org.joda.time.DateTime] = of[org.joda.time.DateTime]

  /**
   * Constructs a simple mapping for a date field (mapped as `org.joda.time.DateTime type`).
   *
   * For example:
   * {{{
   *   Form("birthdate" -> jodaDate("dd-MM-yyyy"))
   * }}}
   *
   * @param pattern the date pattern, as defined in `org.joda.time.format.DateTimeFormat`
   * @param timeZone the `org.joda.time.DateTimeZone` to use for parsing and formatting
   */
  def jodaDate(pattern: String, timeZone: org.joda.time.DateTimeZone = org.joda.time.DateTimeZone.getDefault): Mapping[org.joda.time.DateTime] = of[org.joda.time.DateTime] as jodaDateTimeFormat(pattern, timeZone)

  /**
   * Constructs a simple mapping for a date field (mapped as `org.joda.time.LocalDatetype`).
   *
   * For example:
   * {{{
   * Form("birthdate" -> jodaLocalDate)
   * }}}
   */
  val jodaLocalDate: Mapping[org.joda.time.LocalDate] = of[org.joda.time.LocalDate]

  /**
   * Constructs a simple mapping for a date field (mapped as `org.joda.time.LocalDate type`).
   *
   * For example:
   * {{{
   * Form("birthdate" -> jodaLocalDate("dd-MM-yyyy"))
   * }}}
   *
   * @param pattern the date pattern, as defined in `org.joda.time.format.DateTimeFormat`
   */
  def jodaLocalDate(pattern: String): Mapping[org.joda.time.LocalDate] = of[org.joda.time.LocalDate] as jodaLocalDateFormat(pattern)

  /**
   * Constructs a simple mapping for an e-mail field.
   *
   * @see http://www.w3.org/TR/html5/forms.html#e-mail-state-(type=email)
   *
   * For example:
   * {{{
   *   Form("email" -> email)
   * }}}
   */
  val email: Mapping[String] = of[String] verifying Constraints.emailAddress

  /**
   * Constructs a simple mapping for a Boolean field, such as a check-box.
   *
   * For example:
   * {{{
   *   Form("accepted" -> boolean)
   * }}}
   */
  val boolean: Mapping[Boolean] = of[Boolean]

  def checked(msg: String): Mapping[Boolean] = boolean verifying (msg, _ == true)

}
