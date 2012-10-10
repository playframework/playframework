package play.api.libs.iteratee

import scala.concurrent.Future
import play.api.libs.iteratee.internal.defaultExecutionContext

/**
 * Combines the roles of an Iteratee[From] and a Enumerator[To]
 */
trait Enumeratee[From, To] {
  parent =>

  def applyOn[A](inner: Iteratee[To, A]): Iteratee[From, Iteratee[To, A]]

  /**
   * Alias for `applyOn`
   */
  def apply[A](inner: Iteratee[To, A]): Iteratee[From, Iteratee[To, A]] = applyOn[A](inner)

  def transform[A](inner: Iteratee[To, A]): Iteratee[From, A] = apply(inner).joinI

  /**
   * Alias for `transform`
   */
  def &>>[A](inner: Iteratee[To, A]): Iteratee[From, A] = transform(inner)

  /**
   * Alias for `apply`
   */
  def &>[A](inner: Iteratee[To, A]): Iteratee[From, Iteratee[To, A]] = apply(inner)

  def compose[To2](other: Enumeratee[To, To2]): Enumeratee[From, To2] = {
    new Enumeratee[From, To2] {
      def applyOn[A](iteratee: Iteratee[To2, A]): Iteratee[From, Iteratee[To2, A]] = {
        parent.applyOn(other.applyOn(iteratee)).joinI
      }
    }
  }

  /**
   * Compose this Enumerator with another Enumerator
   */
  def ><>[To2](other: Enumeratee[To, To2]): Enumeratee[From, To2] = compose(other)

  def composeConcat[X](other: Enumeratee[To, To])(implicit p: To => scala.collection.TraversableLike[X, To], bf: scala.collection.generic.CanBuildFrom[To, X, To]): Enumeratee[From, To] = {
    new Enumeratee[From, To] {
      def applyOn[A](iteratee: Iteratee[To, A]): Iteratee[From, Iteratee[To, A]] = {
        parent.applyOn(other.applyOn(iteratee).joinConcatI)
      }
    }
  }

  def >+>[X](other: Enumeratee[To, To])(implicit p: To => scala.collection.TraversableLike[X, To], bf: scala.collection.generic.CanBuildFrom[To, X, To]): Enumeratee[From, To] = composeConcat[X](other)

}

object Enumeratee {

  trait CheckDone[From, To] extends Enumeratee[From, To] {

    def continue[A](k: Input[To] => Iteratee[To, A]): Iteratee[From, Iteratee[To, A]]

    def applyOn[A](it: Iteratee[To, A]): Iteratee[From, Iteratee[To, A]] =
      it.pureFlatFold {
        case Step.Cont(k) => continue(k)
        case _ => Done(it, Input.Empty)
      }
  }

  def zip[E, A, B](inner1: Iteratee[E, A], inner2: Iteratee[E, B]): Iteratee[E, (A, B)] = zipWith(inner1, inner2)((_, _))

  def zipWith[E, A, B, C](inner1: Iteratee[E, A], inner2: Iteratee[E, B])(zipper: (A, B) => C): Iteratee[E, C] = {

    def getNext(it1: Iteratee[E, A], it2: Iteratee[E, B]): Iteratee[E, C] = {
      val eventuallyIter =
        for (
          (a1, it1_) <- getInside(it1);
          (a2, it2_) <- getInside(it2)
        ) yield checkDone(a1, a2) match {
          case Left((msg, in)) => Error(msg, in)
          case Right(None) => Cont(step(it1_, it2_))
          case Right(Some(Left(Left(a)))) => it2_.map(b => zipper(a, b))
          case Right(Some(Left(Right(b)))) => it1_.map(a => zipper(a, b))
          case Right(Some(Right(((a, b), e)))) => Done(zipper(a, b), e)
        }

      Iteratee.flatten(eventuallyIter)
    }

    def step(it1: Iteratee[E, A], it2: Iteratee[E, B])(in: Input[E]) = {
      Iteratee.flatten(
        for (
          it1_ <- it1.feed(in);
          it2_ <- it2.feed(in)
        ) yield getNext(it1_, it2_))

    }

    def getInside[T](it: Iteratee[E, T]): Future[(Option[Either[(String, Input[E]), (T, Input[E])]], Iteratee[E, T])] = {
      it.pureFold {
        case Step.Done(a, e) => Some(Right((a, e)))
        case Step.Cont(k) => None
        case Step.Error(msg, e) => Some(Left((msg, e)))
      }.map(r => (r, it))

    }

    def checkDone(x: Option[Either[(String, Input[E]), (A, Input[E])]], y: Option[Either[(String, Input[E]), (B, Input[E])]]): Either[(String, Input[E]), Option[Either[Either[A, B], ((A, B), Input[E])]]] =
      (x, y) match {
        case (Some(Right((a, e1))), Some(Right((b, e2)))) => Right(Some(Right(((a, b), e1 /* FIXME: should calculate smalled here*/ ))))
        case (Some(Left((msg, e))), _) => Left((msg, e))
        case (_, Some(Left((msg, e)))) => Left((msg, e))
        case (Some(Right((a, _))), None) => Right(Some(Left(Left(a))))
        case (None, Some(Right((b, _)))) => Right(Some(Left(Right(b))))
        case (None, None) => Right(None)

      }
    getNext(inner1, inner2)

  }

  def mapInput[From] = new {
    def apply[To](f: Input[From] => Input[To]) = new CheckDone[From, To] {

      def step[A](k: K[To, A]): K[From, Iteratee[To, A]] = {
        case in @ (Input.El(_) | Input.Empty) =>
          new CheckDone[From, To] { def continue[A](k: K[To, A]) = Cont(step(k)) } &> k(f(in))

        case Input.EOF => Done(Cont(k), Input.EOF)
      }

      def continue[A](k: K[To, A]) = Cont(step(k))
    }
  }

  def mapConcatInput[From] = new {
    def apply[To](f: From => Seq[Input[To]]) = mapFlatten[From](in => Enumerator.enumerateSeq2(f(in)))
  }

  def mapConcat[From] = new {
    def apply[To](f: From => Seq[To]) = mapFlatten[From](in => Enumerator.enumerateSeq1(f(in)))
  }

  def mapFlatten[From] = new {
    def apply[To](f: From => Enumerator[To]) = new CheckDone[From, To] {

      def step[A](k: K[To, A]): K[From, Iteratee[To, A]] = {
        case Input.El(e) =>
          new CheckDone[From, To] { def continue[A](k: K[To, A]) = Cont(step(k)) } &> Iteratee.flatten(f(e)(Cont(k)))

        case in @ Input.Empty =>
          new CheckDone[From, To] { def continue[A](k: K[To, A]) = Cont(step(k)) } &> k(in)

        case Input.EOF => Done(Cont(k), Input.EOF)
      }

      def continue[A](k: K[To, A]) = Cont(step(k))
    }
  }

  def mapInputFlatten[From] = new {
    def apply[To](f: Input[From] => Enumerator[To]) = new CheckDone[From, To] {

      def step[A](k: K[To, A]): K[From, Iteratee[To, A]] = {
        case in =>
          new CheckDone[From, To] { def continue[A](k: K[To, A]) = Cont(step(k)) } &> Iteratee.flatten(f(in)(Cont(k)))
      }

      def continue[A](k: K[To, A]) = Cont(step(k))
    }
  }

  def mapInputM[From] = new {
    def apply[To](f: Input[From] => Future[Input[To]]) = new CheckDone[From, To] {

      def step[A](k: K[To, A]): K[From, Iteratee[To, A]] = {
        case in @ (Input.El(_) | Input.Empty) =>
          new CheckDone[From, To] { def continue[A](k: K[To, A]) = Cont(step(k)) } &> Iteratee.flatten(f(in).map(k(_)))

        case Input.EOF => Done(Cont(k), Input.EOF)
      }

      def continue[A](k: K[To, A]) = Cont(step(k))
    }
  }

  def mapM[E] = new {
    def apply[NE](f: E => Future[NE]): Enumeratee[E, NE] = mapInputM[E] {
      case Input.Empty => Future.successful(Input.Empty)
      case Input.EOF => Future.successful(Input.EOF)
      case Input.El(e) => f(e).map(Input.El(_))
    }
  }

  /**
   * Create an Enumeratee which transforms its input using a given function
   */
  def map[E] = new {
    def apply[NE](f: E => NE): Enumeratee[E, NE] = mapInput[E](in => in.map(f))
  }

  def take[E](count: Int): Enumeratee[E, E] = new CheckDone[E, E] {

    def step[A](remaining: Int)(k: K[E, A]): K[E, Iteratee[E, A]] = {

      case in @ Input.El(_) if remaining == 1 => Done(k(in), Input.Empty)

      case in @ Input.El(_) if remaining > 1 =>
        new CheckDone[E, E] { def continue[A](k: K[E, A]) = Cont(step(remaining - 1)(k)) } &> k(in)

      case in @ Input.Empty if remaining > 0 =>
        new CheckDone[E, E] { def continue[A](k: K[E, A]) = Cont(step(remaining)(k)) } &> k(in)

      case Input.EOF => Done(Cont(k), Input.EOF)

      case in => Done(Cont(k), in)
    }

    def continue[A](k: K[E, A]) = Cont(step(count)(k))

  }

  def scanLeft[From] = new {

    def apply[To](seed: To)(f: (To, From) => To): Enumeratee[From, To] = new CheckDone[From, To] {

      def step[A](lastTo: To)(k: K[To, A]): K[From, Iteratee[To, A]] = {

        case in @ Input.El(e) =>
          val next = f(lastTo, e)
          new CheckDone[From, To] { def continue[A](k: K[To, A]) = Cont(step(next)(k)) } &> k(Input.El(next))

        case in @ Input.Empty =>
          new CheckDone[From, To] { def continue[A](k: K[To, A]) = Cont(step(lastTo)(k)) } &> k(in)

        case Input.EOF => Done(Cont(k), Input.EOF)

      }

      def continue[A](k: K[To, A]) = Cont(step(seed)(k))
    }
  }

  def grouped[From] = new {

    def apply[To](folder: Iteratee[From, To]): Enumeratee[From, To] = new CheckDone[From, To] {

      def step[A](f: Iteratee[From, To])(k: K[To, A]): K[From, Iteratee[To, A]] = {

        case in @ (Input.El(_) | Input.Empty) =>

          Iteratee.flatten(f.feed(in)).pureFlatFold {
            case Step.Done(a, left) => new CheckDone[From, To] {
              def continue[A](k: K[To, A]) =
                (left match {
                  case Input.El(_) => step(folder)(k)(left)
                  case _ => Cont(step(folder)(k))
                })
            } &> k(Input.El(a))
            case Step.Cont(kF) => Cont(step(Cont(kF))(k))
            case Step.Error(msg, e) => Error(msg, in)
          }

        case Input.EOF => Iteratee.flatten(f.run.map((c: To) => Done(k(Input.El(c)), Input.EOF)))

      }

      def continue[A](k: K[To, A]) = Cont(step(folder)(k))
    }
  }

  def filter[E](predicate: E => Boolean): Enumeratee[E, E] = new CheckDone[E, E] {

    def step[A](k: K[E, A]): K[E, Iteratee[E, A]] = {

      case in @ Input.El(e) if predicate(e) =>
        new CheckDone[E, E] { def continue[A](k: K[E, A]) = Cont(step(k)) } &> k(in)

      case in @ Input.El(e) => Cont(step(k))

      case in @ Input.Empty =>
        new CheckDone[E, E] { def continue[A](k: K[E, A]) = Cont(step(k)) } &> k(in)

      case Input.EOF => Done(Cont(k), Input.EOF)

    }

    def continue[A](k: K[E, A]) = Cont(step(k))

  }

  def filterNot[E](predicate: E => Boolean): Enumeratee[E, E] = filter(e => !predicate(e))

  def collect[From] = new {

    def apply[To](transformer: PartialFunction[From, To]): Enumeratee[From, To] = new CheckDone[From, To] {

      def step[A](k: K[To, A]): K[From, Iteratee[To, A]] = {

        case in @ Input.El(e) if transformer.isDefinedAt(e) =>
          new CheckDone[From, To] { def continue[A](k: K[To, A]) = Cont(step(k)) } &> k(in.map(transformer))

        case in @ Input.El(e) => Cont(step(k))

        case in @ Input.Empty =>
          new CheckDone[From, To] { def continue[A](k: K[To, A]) = Cont(step(k)) } &> k(in)

        case Input.EOF => Done(Cont(k), Input.EOF)

      }

      def continue[A](k: K[To, A]) = Cont(step(k))

    }
  }

  def drop[E](count: Int): Enumeratee[E, E] = new CheckDone[E, E] {

    def step[A](remaining: Int)(k: K[E, A]): K[E, Iteratee[E, A]] = {

      case in @ Input.El(_) if remaining == 1 => passAlong[E](Cont(k))

      case in @ Input.El(_) if remaining > 1 => Cont(step(remaining - 1)(k))

      case in @ Input.Empty if remaining > 0 => Cont(step(remaining)(k))

      case Input.EOF => Done(Cont(k), Input.EOF)

      case in => passAlong[E] &> k(in)

    }

    def continue[A](k: K[E, A]) = Cont(step(count)(k))

  }

  def dropWhile[E](p: E => Boolean): Enumeratee[E, E] = new CheckDone[E, E] {

    def step[A](k: K[E, A]): K[E, Iteratee[E, A]] = {

      case in @ Input.El(e) if !p(e) => passAlong[E] &> k(in)

      case in @ Input.El(_) => Cont(step(k))

      case in @ Input.Empty => Cont(step(k))

      case Input.EOF => Done(Cont(k), Input.EOF)

    }

    def continue[A](k: K[E, A]) = Cont(step(k))

  }

  def takeWhile[E](p: E => Boolean): Enumeratee[E, E] = new CheckDone[E, E] {

    def step[A](k: K[E, A]): K[E, Iteratee[E, A]] = {

      case in @ Input.El(e) if !p(e) => Done(Cont(k), in)

      case in @ Input.El(e) =>
        new CheckDone[E, E] { def continue[A](k: K[E, A]) = Cont(step(k)) } &> k(in)

      case in @ Input.Empty =>
        new CheckDone[E, E] { def continue[A](k: K[E, A]) = Cont(step(k)) } &> k(in)

      case Input.EOF => Done(Cont(k), Input.EOF)
    }

    def continue[A](k: K[E, A]) = Cont(step(k))

  }

  def breakE[E](p: E => Boolean) = new Enumeratee[E, E] {
    def applyOn[A](inner: Iteratee[E, A]): Iteratee[E, Iteratee[E, A]] = {
      def step(inner: Iteratee[E, A])(in: Input[E]): Iteratee[E, Iteratee[E, A]] = {
        in match {
          case Input.El(e) if (p(e)) => Done(inner, in)
          case _ =>
            inner.pureFlatFold {
              case Step.Cont(k) => {
                val next = k(in)
                next.pureFlatFold {
                  case Step.Cont(k) => Cont(step(next))
                  case _ => Done(inner, in)
                }
              }
              case _ => Done(inner, in)
            }
        }
      }
      Cont(step(inner))
    }
  }

  def passAlong[M] = new Enumeratee.CheckDone[M, M] {

    def step[A](k: K[M, A]): K[M, Iteratee[M, A]] = {

      case in @ (Input.El(_) | Input.Empty) => new Enumeratee.CheckDone[M, M] { def continue[A](k: K[M, A]) = Cont(step(k)) } &> k(in)

      case Input.EOF => Done(Cont(k), Input.EOF)
    }
    def continue[A](k: K[M, A]) = Cont(step(k))
  }

  def heading[E](es: Enumerator[E]) = new Enumeratee[E,E] {

    def applyOn[A](it:Iteratee[E,A]): Iteratee[E, Iteratee[E,A]] = passAlong[E] &> Iteratee.flatten(es(it))

  }

  def trailing[M](es: Enumerator[M]) =  new Enumeratee.CheckDone[M, M] {

    def step[A](k: K[M, A]): K[M, Iteratee[M, A]] = {

      case in @ (Input.El(_) | Input.Empty) => new Enumeratee.CheckDone[M, M] { def continue[A](k: K[M, A]) = Cont(step(k)) } &> k(in)

      case Input.EOF => Iteratee.flatten((es |>> Cont(k)).map(it  => Done(it, Input.EOF)))
    }
    def continue[A](k: K[M, A]) = Cont(step(k))
  }

  def onIterateeDone[E](action: () => Unit): Enumeratee[E, E] = new Enumeratee[E, E] {

    def applyOn[A](iteratee: Iteratee[E, A]): Iteratee[E, Iteratee[E, A]] = passAlong[E](iteratee).map(_.map { a => action(); a })

  }

  def onEOF[E](action: () => Unit): Enumeratee[E, E] = new CheckDone[E, E] {

    def step[A](k: K[E, A]): K[E, Iteratee[E, A]] = {

      case Input.EOF =>
        action()
        Done(Cont(k), Input.EOF)

      case in =>
        new CheckDone[E, E] { def continue[A](k: K[E, A]) = Cont(step(k)) } &> k(in)
    }

    def continue[A](k: K[E, A]) = Cont(step(k))

  }

}
