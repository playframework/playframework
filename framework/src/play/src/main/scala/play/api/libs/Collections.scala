package play.api.libs

/**
 * Utilities functions for Collections
 */
object Collections {

  def unfoldLeft[A, B](seed: B)(f: B => Option[(B, A)]): Seq[A] = {
    def loop(seed: B)(ls: List[A]): List[A] = f(seed) match {
      case Some((b, a)) => loop(b)(a :: ls)
      case None => ls
    }
    loop(seed)(Nil)
  }

}