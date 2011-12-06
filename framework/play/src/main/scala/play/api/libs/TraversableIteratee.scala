package play.api.libs.iteratee

object Traversable {

  def passAlong[M <: scala.collection.TraversableLike[_, M]] = new Enumeratee[M, M] {
    def apply[A](it: Iteratee[M, A]): Iteratee[M, Iteratee[M, A]] = {
      it.mapDone(a => Done(a, Input.Empty))

    }

  }

  def takeUpTo[M <: scala.collection.TraversableLike[_, M]](count: Int): Enumeratee[M, M] = new Enumeratee[M, M] {

    def apply[A](it: Iteratee[M, A]): Iteratee[M, Iteratee[M, A]] = {

      def step(inner: Iteratee[M, A], leftToTake: Int)(in: Input[M]): Iteratee[M, Iteratee[M, A]] = {
        in match {
          case in @ Input.El(e) =>
            inner.pureFlatFold(
              (_, _) => Done(inner, in),
              k => e.splitAt(leftToTake) match {
                case (all, Nil) => Cont(step(k(Input.El(all)), (leftToTake - all.size)))
                case (Nil, left) => Done(inner, Input.El(left))
                case (toPush, left) => Done(k(Input.El(toPush)), Input.El(left))
              },
              (_, _) => Done(inner, in))

          case Input.EOF => Done(inner, Input.EOF)

          case Input.Empty => Cont(step(inner, leftToTake))
        }

      }
      Cont(step(it, count))

    }
  }

  def take[M <: scala.collection.TraversableLike[_, M]](count: Int): Enumeratee[M, M] = new Enumeratee[M, M] {

    def apply[A](it: Iteratee[M, A]): Iteratee[M, Iteratee[M, A]] = {

      def step(inner: Iteratee[M, A], leftToTake: Int)(in: Input[M]): Iteratee[M, Iteratee[M, A]] = {
        in match {
          case in @ Input.El(e) =>
            e.splitAt(leftToTake) match {
              case (all, Nil) => inner.pureFlatFold(
                (_, _) => Cont(step(inner, (leftToTake - all.size))),
                k => Cont(step(k(Input.El(all)), (leftToTake - all.size))),
                (_, _) => Cont(step(inner, (leftToTake - all.size))))
              case (Nil, left) => Done(inner, Input.El(left))
              case (toPush, left) => Done(inner.pureFlatFold((_, _) => inner, k => k(Input.El(toPush)), (_, _) => inner), Input.El(left))
            }

          case Input.EOF => Done(inner, Input.EOF)

          case Input.Empty => Cont(step(inner, leftToTake))
        }

      }
      Cont(step(it, count))

    }
  }

  def drop[M <: scala.collection.TraversableLike[_, M]](count: Int): Enumeratee[M, M] = new Enumeratee[M, M] {

    def apply[A](inner: Iteratee[M, A]): Iteratee[M, Iteratee[M, A]] = {

      def step(it: Iteratee[M, A], leftToDrop: Int)(in: Input[M]): Iteratee[M, Iteratee[M, A]] = {
        in match {
          case in @ Input.El(e) =>
            val left = leftToDrop - e.size
            left match {
              case i if i > 0 => Cont(step(it, left))
              case i =>
                val toPass = if (i < 0) Input.El(e.drop(leftToDrop)) else Input.Empty
                it.pureFlatFold(
                  (_, _) => Done(it, toPass),
                  k => passAlong(k(toPass)),
                  (_, _) => Done(it, toPass))

            }
          case Input.Empty => Cont(step(it, leftToDrop))

          case Input.EOF => Done(it, Input.EOF)
        }
      }

      Cont(step(inner, count))

    }
  }
}
