package play.api.libs.iteratee

object Traversable {

  def passAlong[M <: scala.collection.TraversableLike[_, M]] = new Enumeratee[M, M] {
    def apply[A](it: Iteratee[M, A]): Iteratee[M, Iteratee[M, A]] = {
      it.mapDone(a => Done(a, Empty))

    }

  }

  def takeUpTo[M <: scala.collection.TraversableLike[_, M]](count: Int): Enumeratee[M, M] = new Enumeratee[M, M] {

    def apply[A](it: Iteratee[M, A]): Iteratee[M, Iteratee[M, A]] = {

      def step(inner: Iteratee[M, A], leftToTake: Int)(in: Input[M]): Iteratee[M, Iteratee[M, A]] = {
        in match {
          case in @ El(e) =>
            inner.pureFlatFold(
              (_, _) => Done(inner, in),
              k => e.splitAt(leftToTake) match {
                case (all, Nil) => Cont(step(k(El(all)), (leftToTake - all.size)))
                case (Nil, left) => Done(inner, El(left))
                case (toPush, left) => Done(k(El(toPush)), El(left))
              },
              (_, _) => Done(inner, in))

          case EOF => Done(inner, EOF)

          case Empty => Cont(step(inner, leftToTake))
        }

      }
      Cont(step(it, count))

    }
  }

  def take[M <: scala.collection.TraversableLike[_, M]](count: Int): Enumeratee[M, M] = new Enumeratee[M, M] {

    def apply[A](it: Iteratee[M, A]): Iteratee[M, Iteratee[M, A]] = {

      def step(inner: Iteratee[M, A], leftToTake: Int)(in: Input[M]): Iteratee[M, Iteratee[M, A]] = {
        in match {
          case in @ El(e) =>
            e.splitAt(leftToTake) match {
              case (all, Nil) => inner.pureFlatFold(
                (_, _) => Cont(step(inner, (leftToTake - all.size))),
                k => Cont(step(k(El(all)), (leftToTake - all.size))),
                (_, _) => Cont(step(inner, (leftToTake - all.size))))
              case (Nil, left) => Done(inner, El(left))
              case (toPush, left) => Done(inner.pureFlatFold((_, _) => inner, k => k(El(toPush)), (_, _) => inner), El(left))
            }

          case EOF => Done(inner, EOF)

          case Empty => Cont(step(inner, leftToTake))
        }

      }
      Cont(step(it, count))

    }
  }

  def drop[M <: scala.collection.TraversableLike[_, M]](count: Int): Enumeratee[M, M] = new Enumeratee[M, M] {

    def apply[A](inner: Iteratee[M, A]): Iteratee[M, Iteratee[M, A]] = {

      def step(it: Iteratee[M, A], leftToDrop: Int)(in: Input[M]): Iteratee[M, Iteratee[M, A]] = {
        in match {
          case in @ El(e) =>
            val left = leftToDrop - e.size
            left match {
              case i if i > 0 => Cont(step(it, left))
              case i =>
                val toPass = if (i < 0) El(e.drop(leftToDrop)) else Empty
                it.pureFlatFold(
                  (_, _) => Done(it, toPass),
                  k => passAlong(k(toPass)),
                  (_, _) => Done(it, toPass))

            }
          case Empty => Cont(step(it, leftToDrop))

          case EOF => Done(it, EOF)
        }
      }

      Cont(step(inner, count))

    }
  }
}
