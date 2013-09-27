package play.api.libs.functional

trait Monoid[A] {

  def append(a1: A, a2: A): A
  def identity: A

}

class MonoidOps[A](m1: A)(implicit m: Monoid[A]) {
  def |+|(m2: A): A = m.append(m1, m2)
}

/* A practical variant of monoid act/action/operator (search on wikipedia)
 * - allows to take an element A to create a B
 * - allows a prepend/append a A to a B
 * cf Reducer[JsValue, JsArray]
 */
trait Reducer[A, B] {

  def unit(a: A): B
  def prepend(a: A, b: B): B
  def append(b: B, a: A): B

}

object Reducer {
  def apply[A, B](f: A => B)(implicit m: Monoid[B]) = new Reducer[A, B] {
    def unit(a: A): B = f(a)
    def prepend(a: A, b: B) = m.append(unit(a), b)
    def append(b: B, a: A) = m.append(b, unit(a))
  }
}
