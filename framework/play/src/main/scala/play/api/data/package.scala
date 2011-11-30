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
   * Creates a Mapping of type `T`.
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

  /**
   * Creates a Mapping of type `T`.
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
  def of[R <: Product, A1](apply: Function1[A1, R])(a1: (String, Mapping[A1])): Mapping[R] = {
    ObjectMapping1(apply, a1)
  }
  def of[R <: Product, A1, A2](apply: Function2[A1, A2, R])(a1: (String, Mapping[A1]), a2: (String, Mapping[A2])): Mapping[R] = {
    ObjectMapping2(apply, a1, a2)
  }
  def of[R <: Product, A1, A2, A3](apply: Function3[A1, A2, A3, R])(a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3])): Mapping[R] = {
    ObjectMapping3(apply, a1, a2, a3)
  }
  def of[R <: Product, A1, A2, A3, A4](apply: Function4[A1, A2, A3, A4, R])(a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4])): Mapping[R] = {
    ObjectMapping4(apply, a1, a2, a3, a4)
  }
  def of[R <: Product, A1, A2, A3, A4, A5](apply: Function5[A1, A2, A3, A4, A5, R])(a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5])): Mapping[R] = {
    ObjectMapping5(apply, a1, a2, a3, a4, a5)
  }
  def of[R <: Product, A1, A2, A3, A4, A5, A6](apply: Function6[A1, A2, A3, A4, A5, A6, R])(a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6])): Mapping[R] = {
    ObjectMapping6(apply, a1, a2, a3, a4, a5, a6)
  }
  def of[R <: Product, A1, A2, A3, A4, A5, A6, A7](apply: Function7[A1, A2, A3, A4, A5, A6, A7, R])(a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7])): Mapping[R] = {
    ObjectMapping7(apply, a1, a2, a3, a4, a5, a6, a7)
  }
  def of[R <: Product, A1, A2, A3, A4, A5, A6, A7, A8](apply: Function8[A1, A2, A3, A4, A5, A6, A7, A8, R])(a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8])): Mapping[R] = {
    ObjectMapping8(apply, a1, a2, a3, a4, a5, a6, a7, a8)
  }
  def of[R <: Product, A1, A2, A3, A4, A5, A6, A7, A8, A9](apply: Function9[A1, A2, A3, A4, A5, A6, A7, A8, A9, R])(a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9])): Mapping[R] = {
    ObjectMapping9(apply, a1, a2, a3, a4, a5, a6, a7, a8, a9)
  }
  def of[R <: Product, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10](apply: Function10[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, R])(a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9]), a10: (String, Mapping[A10])): Mapping[R] = {
    ObjectMapping10(apply, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10)
  }
  def of[R <: Product, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11](apply: Function11[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, R])(a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9]), a10: (String, Mapping[A10]), a11: (String, Mapping[A11])): Mapping[R] = {
    ObjectMapping11(apply, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11)
  }
  def of[R <: Product, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12](apply: Function12[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, R])(a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9]), a10: (String, Mapping[A10]), a11: (String, Mapping[A11]), a12: (String, Mapping[A12])): Mapping[R] = {
    ObjectMapping12(apply, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12)
  }
  def of[R <: Product, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13](apply: Function13[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, R])(a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9]), a10: (String, Mapping[A10]), a11: (String, Mapping[A11]), a12: (String, Mapping[A12]), a13: (String, Mapping[A13])): Mapping[R] = {
    ObjectMapping13(apply, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13)
  }
  def of[R <: Product, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14](apply: Function14[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, R])(a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9]), a10: (String, Mapping[A10]), a11: (String, Mapping[A11]), a12: (String, Mapping[A12]), a13: (String, Mapping[A13]), a14: (String, Mapping[A14])): Mapping[R] = {
    ObjectMapping14(apply, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14)
  }
  def of[R <: Product, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15](apply: Function15[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, R])(a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9]), a10: (String, Mapping[A10]), a11: (String, Mapping[A11]), a12: (String, Mapping[A12]), a13: (String, Mapping[A13]), a14: (String, Mapping[A14]), a15: (String, Mapping[A15])): Mapping[R] = {
    ObjectMapping15(apply, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15)
  }
  def of[R <: Product, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16](apply: Function16[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, R])(a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9]), a10: (String, Mapping[A10]), a11: (String, Mapping[A11]), a12: (String, Mapping[A12]), a13: (String, Mapping[A13]), a14: (String, Mapping[A14]), a15: (String, Mapping[A15]), a16: (String, Mapping[A16])): Mapping[R] = {
    ObjectMapping16(apply, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16)
  }
  def of[R <: Product, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17](apply: Function17[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, R])(a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9]), a10: (String, Mapping[A10]), a11: (String, Mapping[A11]), a12: (String, Mapping[A12]), a13: (String, Mapping[A13]), a14: (String, Mapping[A14]), a15: (String, Mapping[A15]), a16: (String, Mapping[A16]), a17: (String, Mapping[A17])): Mapping[R] = {
    ObjectMapping17(apply, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17)
  }
  def of[R <: Product, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18](apply: Function18[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, R])(a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9]), a10: (String, Mapping[A10]), a11: (String, Mapping[A11]), a12: (String, Mapping[A12]), a13: (String, Mapping[A13]), a14: (String, Mapping[A14]), a15: (String, Mapping[A15]), a16: (String, Mapping[A16]), a17: (String, Mapping[A17]), a18: (String, Mapping[A18])): Mapping[R] = {
    ObjectMapping18(apply, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18)
  }
  def of[R <: Product, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19](apply: Function19[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, R])(a1: (String, Mapping[A1]), a2: (String, Mapping[A2]), a3: (String, Mapping[A3]), a4: (String, Mapping[A4]), a5: (String, Mapping[A5]), a6: (String, Mapping[A6]), a7: (String, Mapping[A7]), a8: (String, Mapping[A8]), a9: (String, Mapping[A9]), a10: (String, Mapping[A10]), a11: (String, Mapping[A11]), a12: (String, Mapping[A12]), a13: (String, Mapping[A13]), a14: (String, Mapping[A14]), a15: (String, Mapping[A15]), a16: (String, Mapping[A16]), a17: (String, Mapping[A17]), a18: (String, Mapping[A18]), a19: (String, Mapping[A19])): Mapping[R] = {
    ObjectMapping19(apply, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19)
  }

  /**
   * Creates a Mapping of tuple `(A,B)`.
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

  def of[A, B, C, D, E](a: (String, Mapping[A]), b: (String, Mapping[B]), c: (String, Mapping[C]), d: (String, Mapping[D]), e: (String, Mapping[E])): Mapping[(A, B, C, D, E)] = of((a: A, b: B, c: C, d: D, e: E) => (a, b, c, d, e))(a, b, c, d, e)

  def of[A, B, C, D, E, F](a: (String, Mapping[A]), b: (String, Mapping[B]), c: (String, Mapping[C]),
    d: (String, Mapping[D]), e: (String, Mapping[E]), f: (String, Mapping[F])): Mapping[(A, B, C, D, E, F)] = of((a: A, b: B, c: C, d: D, e: E, f: F) => (a, b, c, d, e, f))(a, b, c, d, e, f)

  def of[A, B, C, D, E, F, G](a: (String, Mapping[A]), b: (String, Mapping[B]), c: (String, Mapping[C]),
    d: (String, Mapping[D]), e: (String, Mapping[E]), f: (String, Mapping[F]), g: (String, Mapping[G])): Mapping[(A, B, C, D, E, F, G)] = of((a: A, b: B, c: C, d: D, e: E, f: F, g: G) => (a, b, c, d, e, f, g))(a, b, c, d, e, f, g)

  def of[A, B, C, D, E, F, G, H](a: (String, Mapping[A]), b: (String, Mapping[B]), c: (String, Mapping[C]),
    d: (String, Mapping[D]), e: (String, Mapping[E]), f: (String, Mapping[F]), g: (String, Mapping[G]), h: (String, Mapping[H])): Mapping[(A, B, C, D, E, F, G, H)] = of((a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H) => (a, b, c, d, e, f, g, h))(a, b, c, d, e, f, g, h)

  def of[A, B, C, D, E, F, G, H, I](a: (String, Mapping[A]), b: (String, Mapping[B]), c: (String, Mapping[C]),
    d: (String, Mapping[D]), e: (String, Mapping[E]), f: (String, Mapping[F]), g: (String, Mapping[G]), h: (String, Mapping[H]), i: (String, Mapping[I])): Mapping[(A, B, C, D, E, F, G, H, I)] = of((a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I) => (a, b, c, d, e, f, g, h, i))(a, b, c, d, e, f, g, h, i)

  def of[A, B, C, D, E, F, G, H, I, J](a: (String, Mapping[A]), b: (String, Mapping[B]), c: (String, Mapping[C]),
    d: (String, Mapping[D]), e: (String, Mapping[E]), f: (String, Mapping[F]), g: (String, Mapping[G]), h: (String, Mapping[H]), i: (String, Mapping[I]), j: (String, Mapping[J])): Mapping[(A, B, C, D, E, F, G, H, I, J)] = of((a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J) => (a, b, c, d, e, f, g, h, i, j))(a, b, c, d, e, f, g, h, i, j)
  //10th
  def of[A, B, C, D, E, F, G, H, I, J, K](a: (String, Mapping[A]), b: (String, Mapping[B]), c: (String, Mapping[C]),
    d: (String, Mapping[D]), e: (String, Mapping[E]), f: (String, Mapping[F]),
    g: (String, Mapping[G]), h: (String, Mapping[H]), i: (String, Mapping[I]), j: (String, Mapping[J]), k: (String, Mapping[K])): Mapping[(A, B, C, D, E, F, G, H, I, J, K)] = of((a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K) => (a, b, c, d, e, f, g, h, i, j, k))(a, b, c, d, e, f, g, h, i, j, k)

  def of[A, B, C, D, E, F, G, H, I, J, K, L](a: (String, Mapping[A]), b: (String, Mapping[B]), c: (String, Mapping[C]),
    d: (String, Mapping[D]), e: (String, Mapping[E]), f: (String, Mapping[F]),
    g: (String, Mapping[G]), h: (String, Mapping[H]),
    i: (String, Mapping[I]), j: (String, Mapping[J]),
    k: (String, Mapping[K]), l: (String, Mapping[L])): Mapping[(A, B, C, D, E, F, G, H, I, J, K, L)] = of((a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L) => (a, b, c, d, e, f, g, h, i, j, k, l))(a, b, c, d, e, f, g, h, i, j, k, l)

  def of[A, B, C, D, E, F, G, H, I, J, K, L, N](a: (String, Mapping[A]), b: (String, Mapping[B]), c: (String, Mapping[C]),
    d: (String, Mapping[D]), e: (String, Mapping[E]), f: (String, Mapping[F]),
    g: (String, Mapping[G]), h: (String, Mapping[H]),
    i: (String, Mapping[I]), j: (String, Mapping[J]),
    k: (String, Mapping[K]), l: (String, Mapping[L]), n: (String, Mapping[N])): Mapping[(A, B, C, D, E, F, G, H, I, J, K, L, N)] = of((a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, n: N) => (a, b, c, d, e, f, g, h, i, j, k, l, n))(a, b, c, d, e, f, g, h, i, j, k, l, n)

  def of[A, B, C, D, E, F, G, H, I, J, K, L, N, M](a: (String, Mapping[A]), b: (String, Mapping[B]), c: (String, Mapping[C]),
    d: (String, Mapping[D]), e: (String, Mapping[E]), f: (String, Mapping[F]),
    g: (String, Mapping[G]), h: (String, Mapping[H]),
    i: (String, Mapping[I]), j: (String, Mapping[J]),
    k: (String, Mapping[K]), l: (String, Mapping[L]), n: (String, Mapping[N]), m: (String, Mapping[M])): Mapping[(A, B, C, D, E, F, G, H, I, J, K, L, N, M)] = of((a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, n: N, m: M) => (a, b, c, d, e, f, g, h, i, j, k, l, n, m))(a, b, c, d, e, f, g, h, i, j, k, l, n, m)

  def of[A, B, C, D, E, F, G, H, I, J, K, L, N, M, O](a: (String, Mapping[A]), b: (String, Mapping[B]), c: (String, Mapping[C]),
    d: (String, Mapping[D]), e: (String, Mapping[E]), f: (String, Mapping[F]),
    g: (String, Mapping[G]), h: (String, Mapping[H]),
    i: (String, Mapping[I]), j: (String, Mapping[J]),
    k: (String, Mapping[K]), l: (String, Mapping[L]),
    n: (String, Mapping[N]), m: (String, Mapping[M]),
    o: (String, Mapping[O])): Mapping[(A, B, C, D, E, F, G, H, I, J, K, L, N, M, O)] =
    of((a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, n: N, m: M, o: O) => (a, b, c, d, e, f, g, h, i, j, k, l, n, m, o))(a, b, c, d, e, f, g, h, i, j, k, l, n, m, o)

  def of[A, B, C, D, E, F, G, H, I, J, K, L, N, M, O, P](a: (String, Mapping[A]), b: (String, Mapping[B]), c: (String, Mapping[C]),
    d: (String, Mapping[D]), e: (String, Mapping[E]), f: (String, Mapping[F]),
    g: (String, Mapping[G]), h: (String, Mapping[H]),
    i: (String, Mapping[I]), j: (String, Mapping[J]),
    k: (String, Mapping[K]), l: (String, Mapping[L]),
    n: (String, Mapping[N]), m: (String, Mapping[M]),
    o: (String, Mapping[O]), p: (String, Mapping[P])): Mapping[(A, B, C, D, E, F, G, H, I, J, K, L, N, M, O, P)] =
    of((a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, n: N, m: M, o: O, p: P) => (a, b, c, d, e, f, g, h, i, j, k, l, n, m, o, p))(a, b, c, d, e, f, g, h, i, j, k, l, n, m, o, p)

  def of[A, B, C, D, E, F, G, H, I, J, K, L, N, M, O, P, R](a: (String, Mapping[A]), b: (String, Mapping[B]), c: (String, Mapping[C]),
    d: (String, Mapping[D]), e: (String, Mapping[E]), f: (String, Mapping[F]),
    g: (String, Mapping[G]), h: (String, Mapping[H]),
    i: (String, Mapping[I]), j: (String, Mapping[J]),
    k: (String, Mapping[K]), l: (String, Mapping[L]),
    n: (String, Mapping[N]), m: (String, Mapping[M]),
    o: (String, Mapping[O]), p: (String, Mapping[P]), r: (String, Mapping[R])): Mapping[(A, B, C, D, E, F, G, H, I, J, K, L, N, M, O, P, R)] =
    of((a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, n: N, m: M, o: O, p: P, r: R) => (a, b, c, d, e, f, g, h, i, j, k, l, n, m, o, p, r))(a, b, c, d, e, f, g, h, i, j, k, l, n, m, o, p, r)

  def of[A, B, C, D, E, F, G, H, I, J, K, L, N, M, O, P, R, S](a: (String, Mapping[A]), b: (String, Mapping[B]), c: (String, Mapping[C]),
    d: (String, Mapping[D]), e: (String, Mapping[E]), f: (String, Mapping[F]),
    g: (String, Mapping[G]), h: (String, Mapping[H]),
    i: (String, Mapping[I]), j: (String, Mapping[J]),
    k: (String, Mapping[K]), l: (String, Mapping[L]),
    n: (String, Mapping[N]), m: (String, Mapping[M]),
    o: (String, Mapping[O]), p: (String, Mapping[P]), r: (String, Mapping[R]), s: (String, Mapping[S])): Mapping[(A, B, C, D, E, F, G, H, I, J, K, L, N, M, O, P, R, S)] =
    of((a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, n: N, m: M, o: O, p: P, r: R, s: S) => (a, b, c, d, e, f, g, h, i, j, k, l, n, m, o, p, r, s))(a, b, c, d, e, f, g, h, i, j, k, l, n, m, o, p, r, s)

  def of[A, B, C, D, E, F, G, H, I, J, K, L, N, M, O, P, R, S, T](a: (String, Mapping[A]), b: (String, Mapping[B]), c: (String, Mapping[C]),
    d: (String, Mapping[D]), e: (String, Mapping[E]), f: (String, Mapping[F]),
    g: (String, Mapping[G]), h: (String, Mapping[H]),
    i: (String, Mapping[I]), j: (String, Mapping[J]),
    k: (String, Mapping[K]), l: (String, Mapping[L]),
    n: (String, Mapping[N]), m: (String, Mapping[M]),
    o: (String, Mapping[O]), p: (String, Mapping[P]), r: (String, Mapping[R]), s: (String, Mapping[S]), t: (String, Mapping[T])): Mapping[(A, B, C, D, E, F, G, H, I, J, K, L, N, M, O, P, R, S, T)] =
    of((a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, n: N, m: M, o: O, p: P, r: R, s: S, t: T) => (a, b, c, d, e, f, g, h, i, j, k, l, n, m, o, p, r, s, t))(a, b, c, d, e, f, g, h, i, j, k, l, n, m, o, p, r, s, t)

  // --

  import Form._
  import Formats._

  /**
   * Constructs a simple mapping for a text field.
   *
   * For example:
   * {{{
   *   Form("username" -> text)
   * }}}
   */
  val text: Mapping[String] = of[String]

  /**
   * Constructs a simple mapping for required text field.
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
   * Constructs a simple mapping for a text field.
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

  /**
   * Constructs a simple mapping for required text field.
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
   * Constructs a simple mapping for a numeric field.
   *
   * For example:
   * {{{
   *   Form("size" -> number)
   * }}}
   */
  val number: Mapping[Long] = of[Long]

  /**
   * Constructs a simple mapping for a numeric field.
   *
   * For example:
   * {{{
   *   Form("size" -> number(min=0, max=100))
   * }}}
   *
   * @param min minimum value
   * @param max maximum value
   */
  def number(min: Long = Long.MinValue, max: Long = Long.MaxValue): Mapping[Long] = (min, max) match {
    case (Long.MinValue, Long.MaxValue) => number
    case (min, Long.MaxValue) => number verifying Constraints.min(min)
    case (Long.MinValue, max) => number verifying Constraints.max(max)
    case (min, max) => number verifying (Constraints.min(min), Constraints.max(max))
  }

  /**
   * Constructs a simple mapping for a date field.
   *
   * For example:
   * {{{
   *   Form("birthdate" -> date)
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
   * Defines an optional mapping.
   *
   * @param mapping The mapping to make optional.
   */
  def optional[A](mapping: Mapping[A]): Mapping[Option[A]] = OptionalMapping(mapping)

  /**
   * Constructs a simple mapping for a date field.
   *
   * For example:
   * {{{
   *   Form("birthdate" -> date("dd-MM-yyyy"))
   * }}}
   *
   * @param pattern the date pattern, as defined in `java.text.SimpleDateFormat`
   */
  def date(pattern: String): Mapping[java.util.Date] = of[java.util.Date] as dateFormat(pattern)

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
   */
  def sqlDate(pattern: String): Mapping[java.sql.Date] = of[java.sql.Date] as sqlDateFormat(pattern)

  /**
   * Constructs a simple mapping for an e-mail field.
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

  /**
   * Constructs a simple mapping for a Boolean field, such as a check-box.
   *
   * For example:
   * {{{
   *   Form("accepted" -> boolean)
   * }}}
   */
  val boolean: Mapping[Boolean] = of[Boolean]

}
