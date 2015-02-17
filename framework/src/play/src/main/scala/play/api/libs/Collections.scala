/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs

/**
 * Utilities functions for Collections
 */
object Collections {

  /**
   * Produces a Seq from a seed and a function.
   *
   * Example, produces a List from 100 to 0.
   *
   * {{{
   * unfoldLeft(0) {
   *   case a if a > 100 => None
   *   case a => Some((a + 1, a))
   * }
   * }}}
   *
   * @tparam A Type of the final List elements.
   * @tparam B Seed type
   * @param seed Initial value.
   * @param f Function producing the List elements.
   */
  def unfoldLeft[A, B](seed: B)(f: B => Option[(B, A)]): Seq[A] = {
    def loop(seed: B)(ls: List[A]): List[A] = f(seed) match {
      case Some((b, a)) => loop(b)(a :: ls)
      case None => ls
    }
    loop(seed)(Nil)
  }

}
