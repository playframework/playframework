package play.api.libs.iteratee

import play.api.libs.iteratee.Execution.Implicits.{ defaultExecutionContext => dec }
import scala.concurrent.{ ExecutionContext, Future }

/**
 * @define paramEcSingle @param ec The context to execute the supplied function with. The context is prepared on the calling thread before being used.
 */
object Traversable {

  /**
   * A partially-applied function returned by the `head` method.
   */
  trait Head[E] {
    def apply[A](implicit p: E => scala.collection.TraversableLike[A, E]): Iteratee[E, Option[A]]
  }

  def head[E] = new Head[E] {
    def apply[A](implicit p: E => scala.collection.TraversableLike[A, E]): Iteratee[E, Option[A]] = {

      def step: K[E, Option[A]] = {
        case Input.Empty => Cont(step)
        case Input.EOF => Done(None, Input.EOF)
        case Input.El(xs) if !xs.isEmpty => Done(Some(xs.head), Input.El(xs.tail))
        case Input.El(empty) => Cont(step)
      }
      Cont(step)
    }
  }

  def takeUpTo[M](count: Int)(implicit p: M => scala.collection.TraversableLike[_, M]): Enumeratee[M, M] = new Enumeratee[M, M] {

    def applyOn[A](it: Iteratee[M, A]): Iteratee[M, Iteratee[M, A]] = {

      def step(inner: Iteratee[M, A], leftToTake: Int)(in: Input[M]): Iteratee[M, Iteratee[M, A]] = {
        in match {
          case in @ Input.El(e) =>
            inner.pureFlatFold {
              case Step.Cont(k) => e.splitAt(leftToTake) match {
                case (all, x) if x.isEmpty => Cont(step(k(Input.El(all)), (leftToTake - all.size)))
                case (x, left) if x.isEmpty => Done(inner, Input.El(left))
                case (toPush, left) => Done(k(Input.El(toPush)), Input.El(left))
              }
              case _ => Done(inner, in)
            }

          case Input.EOF => Done(inner, Input.EOF)

          case Input.Empty => Cont(step(inner, leftToTake))
        }

      }
      Cont(step(it, count))
    }
  }

  def take[M](count: Int)(implicit p: M => scala.collection.TraversableLike[_, M]): Enumeratee[M, M] = new Enumeratee[M, M] {

    def applyOn[A](it: Iteratee[M, A]): Iteratee[M, Iteratee[M, A]] = {

      def step(inner: Iteratee[M, A], leftToTake: Int)(in: Input[M]): Iteratee[M, Iteratee[M, A]] = {
        in match {
          case in @ Input.El(e) =>
            e.splitAt(leftToTake) match {
              case (all, x) if x.isEmpty => inner.pureFlatFold {
                case Step.Done(_, _) => Cont(step(inner, (leftToTake - all.size)))
                case Step.Cont(k) => Cont(step(k(Input.El(all)), (leftToTake - all.size)))
                case Step.Error(_, _) => Cont(step(inner, (leftToTake - all.size)))
              }
              case (x, left) if x.isEmpty => Done(inner, Input.El(left))
              case (toPush, left) => Done(inner.pureFlatFold { case Step.Cont(k) => k(Input.El(toPush)); case _ => inner }, Input.El(left))
            }

          case Input.EOF => Done(inner, Input.EOF)

          case Input.Empty => Cont(step(inner, leftToTake))
        }

      }
      Cont(step(it, count))

    }
  }

  import Enumeratee.CheckDone

  /**
   * Splits a stream of traversable input elements on a predicate. Yields all input up until
   * the predicate matches within an input element. The input following a match is unprocessed.
   *
   * @param p The predicate to split input on.
   * $paramEcSingle
   */
  def splitOnceAt[M, E](p: E => Boolean)(implicit traversableLike: M => scala.collection.TraversableLike[E, M], ec: ExecutionContext): Enumeratee[M, M] = new CheckDone[M, M] {
    val pec = ec.prepare()

    def step[A](k: K[M, A]): K[M, Iteratee[M, A]] = {

      case in @ Input.El(e) =>
        Iteratee.flatten(Future(e.span(p))(pec).map {
          case (prefix, suffix) if suffix.isEmpty => new CheckDone[M, M] { def continue[A](k: K[M, A]) = Cont(step(k)) } &> k(Input.El(prefix))
          case (prefix, suffix) => Done(if (prefix.isEmpty) Cont(k) else k(Input.El(prefix)), Input.El(suffix.drop(1)))
        }(dec))

      case in @ Input.Empty =>
        new CheckDone[M, M] { def continue[A](k: K[M, A]) = Cont(step(k)) } &> k(in)

      case Input.EOF => Done(Cont(k), Input.EOF)

    }

    def continue[A](k: K[M, A]) = Cont(step(k))

  }

  def drop[M](count: Int)(implicit p: M => scala.collection.TraversableLike[_, M]): Enumeratee[M, M] = new Enumeratee[M, M] {

    def applyOn[A](inner: Iteratee[M, A]): Iteratee[M, Iteratee[M, A]] = {

      def step(it: Iteratee[M, A], leftToDrop: Int)(in: Input[M]): Iteratee[M, Iteratee[M, A]] = {
        in match {
          case in @ Input.El(e) =>
            val left = leftToDrop - e.size
            left match {
              case i if i > 0 => Cont(step(it, left))
              case i =>
                val toPass = if (i < 0) Input.El(e.drop(leftToDrop)) else Input.Empty
                it.pureFlatFold {
                  case Step.Cont(k) => Enumeratee.passAlong.applyOn(k(toPass))
                  case _ => Done(it, toPass)
                }
            }
          case Input.Empty => Cont(step(it, leftToDrop))

          case Input.EOF => Done(it, Input.EOF)
        }
      }

      Cont(step(inner, count))

    }
  }
}
