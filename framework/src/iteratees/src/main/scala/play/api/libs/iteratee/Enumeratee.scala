package play.api.libs.iteratee

import play.api.libs.iteratee.Execution.Implicits.{ defaultExecutionContext => dec }
import play.api.libs.iteratee.internal.{ executeIteratee, executeFuture }
import scala.language.reflectiveCalls
import scala.concurrent.{ ExecutionContext, Future }

/**
 * Combines the roles of an Iteratee[From] and a Enumerator[To].  This allows adapting of streams to that modify input
 * produced by an Enumerator, or to be consumed by a Iteratee.
 */
trait Enumeratee[From, To] {
  parent =>

  /**
   * Create a new Iteratee that feeds its input, potentially modifying it along the way, into the inner Iteratee, and
   * produces that Iteratee as its result.
   */
  def applyOn[A](inner: Iteratee[To, A]): Iteratee[From, Iteratee[To, A]]

  /**
   * Alias for `applyOn`
   */
  def apply[A](inner: Iteratee[To, A]): Iteratee[From, Iteratee[To, A]] = applyOn[A](inner)

  /**
   * Transform the given iteratee into an iteratee that accepts the input type that this enumeratee maps.
   */
  def transform[A](inner: Iteratee[To, A]): Iteratee[From, A] = apply(inner).joinI

  /**
   * Alias for `transform`
   */
  def &>>[A](inner: Iteratee[To, A]): Iteratee[From, A] = transform(inner)

  /**
   * Alias for `apply`
   */
  def &>[A](inner: Iteratee[To, A]): Iteratee[From, Iteratee[To, A]] = apply(inner)

  /**
   * Compose this Enumeratee with another Enumeratee
   */
  def compose[To2](other: Enumeratee[To, To2]): Enumeratee[From, To2] = {
    new Enumeratee[From, To2] {
      def applyOn[A](iteratee: Iteratee[To2, A]): Iteratee[From, Iteratee[To2, A]] = {
        parent.applyOn(other.applyOn(iteratee)).joinI
      }
    }
  }

  /**
   * Compose this Enumeratee with another Enumeratee
   */
  def ><>[To2](other: Enumeratee[To, To2]): Enumeratee[From, To2] = compose(other)

  /**
   * Compose this Enumeratee with another Enumeratee, concatenating any input left by both Enumeratees when they
   * are done.
   */
  def composeConcat[X](other: Enumeratee[To, To])(implicit p: To => scala.collection.TraversableLike[X, To], bf: scala.collection.generic.CanBuildFrom[To, X, To]): Enumeratee[From, To] = {
    new Enumeratee[From, To] {
      def applyOn[A](iteratee: Iteratee[To, A]): Iteratee[From, Iteratee[To, A]] = {
        parent.applyOn(other.applyOn(iteratee).joinConcatI)
      }
    }
  }

  /**
   * Alias for `composeConcat`
   */
  def >+>[X](other: Enumeratee[To, To])(implicit p: To => scala.collection.TraversableLike[X, To], bf: scala.collection.generic.CanBuildFrom[To, X, To]): Enumeratee[From, To] = composeConcat[X](other)

}

/**
 * @define paramEcSingle @param ec The context to execute the supplied function with. The context is prepared on the calling thread before being used.
 * @define paramEcMultiple @param ec The context to execute the supplied functions with. The context is prepared on the calling thread before being used.
 */
object Enumeratee {

  /**
   * An Enumeratee that checks to ensure that the passed in Iteratee is not done before doing any work.
   */
  trait CheckDone[From, To] extends Enumeratee[From, To] {

    def continue[A](k: Input[To] => Iteratee[To, A]): Iteratee[From, Iteratee[To, A]]

    def applyOn[A](it: Iteratee[To, A]): Iteratee[From, Iteratee[To, A]] =
      it.pureFlatFold[From, Iteratee[To, A]] {
        case Step.Cont(k) => continue(k)
        case _ => Done(it, Input.Empty)
      }(dec)
  }

  /**
   * flatten a [[scala.concurrent.Future]] of [[play.api.libs.iteratee.Enumeratee]]] into an Enumeratee
   *
   * @param futureOfEnumeratee a future of enumeratee
   */
  def flatten[From, To](futureOfEnumeratee: Future[Enumeratee[From, To]]) = new Enumeratee[From, To] {
    def applyOn[A](it: Iteratee[To, A]): Iteratee[From, Iteratee[To, A]] =
      Iteratee.flatten(futureOfEnumeratee.map(_.applyOn[A](it))(dec))
  }

  /**
   * Create an Enumeratee that zips two Iteratees together.
   *
   * Each input gets passed to each Iteratee, and the result is a tuple of both of their results.
   *
   * If either Iteratee encounters an error, the result will be an error.
   *
   * The Enumeratee will continue consuming input until both inner Iteratees are done.  If one inner Iteratee finishes
   * before the other, the result of that Iteratee is held, and the one continues by itself, until it too is finished.
   */
  def zip[E, A, B](inner1: Iteratee[E, A], inner2: Iteratee[E, B]): Iteratee[E, (A, B)] = zipWith(inner1, inner2)((_, _))(dec)

  /**
   * Create an Enumeratee that zips two Iteratees together, using the passed in zipper function to combine the results
   * of the two.
   *
   * @param inner1 The first Iteratee to combine.
   * @param inner2 The second Iteratee to combine.
   * @param zipper Used to combine the results of each Iteratee.
   * $paramEcSingle
   */
  def zipWith[E, A, B, C](inner1: Iteratee[E, A], inner2: Iteratee[E, B])(zipper: (A, B) => C)(implicit ec: ExecutionContext): Iteratee[E, C] = {
    val pec = ec.prepare()
    import Execution.Implicits.{ defaultExecutionContext => ec } // Shadow ec to make this the only implicit EC in scope

    def getNext(it1: Iteratee[E, A], it2: Iteratee[E, B]): Iteratee[E, C] = {
      val eventuallyIter =
        for (
          (a1, it1_) <- getInside(it1);
          (a2, it2_) <- getInside(it2)
        ) yield checkDone(a1, a2) match {
          case Left((msg, in)) => Error(msg, in)
          case Right(None) => Cont(step(it1_, it2_))
          case Right(Some(Left(Left(a)))) => it2_.map(b => zipper(a, b))(pec)
          case Right(Some(Left(Right(b)))) => it1_.map(a => zipper(a, b))(pec)
          case Right(Some(Right(((a, b), e)))) => executeIteratee(Done(zipper(a, b), e))(pec)
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
      }(dec).map(r => (r, it))(dec)

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

  /**
   * A partially-applied function returned by the `mapInput` method.
   */
  trait MapInput[From] {
    /**
     * @param f Used to transform each input element.
     * $paramEcSingle
     */
    def apply[To](f: Input[From] => Input[To])(implicit ec: ExecutionContext): Enumeratee[From, To]
  }

  /**
   * Create an Enumeratee that transforms its input using the given function.
   *
   * This is like the `map` function, except that it allows the Enumeratee to, for example, send EOF to the inner
   * iteratee before EOF is encountered.
   */
  def mapInput[From] = new MapInput[From] {
    def apply[To](f: Input[From] => Input[To])(implicit ec: ExecutionContext) = new CheckDone[From, To] {
      val pec = ec.prepare()

      def step[A](k: K[To, A]): K[From, Iteratee[To, A]] = {
        case in @ (Input.El(_) | Input.Empty) => new CheckDone[From, To] {
          def continue[A](k: K[To, A]) = Cont(step(k))
        } &> Iteratee.flatten(Future(f(in))(pec).map(in => k(in))(dec))

        case Input.EOF => Done(Cont(k), Input.EOF)
      }

      def continue[A](k: K[To, A]) = Cont(step(k))
    }
  }

  /**
   * A partially-applied function returned by the `mapConcatInput` method.
   */
  trait MapConcatInput[From] {
    /**
     * @param f Used to transform each input element into a sequence of inputs.
     * $paramEcSingle
     */
    def apply[To](f: From => Seq[Input[To]])(implicit ec: ExecutionContext): Enumeratee[From, To]
  }

  /**
   * Create an enumeratee that transforms its input into a sequence of inputs for the target iteratee.
   */
  def mapConcatInput[From] = new MapConcatInput[From] {
    def apply[To](f: From => Seq[Input[To]])(implicit ec: ExecutionContext) = mapFlatten[From](in => Enumerator.enumerateSeq2(f(in)))(ec)
  }

  /**
   * A partially-applied function returned by the `mapConcat` method.
   */
  trait MapConcat[From] {
    /**
     * @param f Used to transform each input element into a sequence of input elements.
     * $paramEcSingle
     */
    def apply[To](f: From => Seq[To])(implicit ec: ExecutionContext): Enumeratee[From, To]
  }

  /**
   * Create an Enumeratee that transforms its input elements into a sequence of input elements for the target Iteratee.
   */
  def mapConcat[From] = new MapConcat[From] {
    def apply[To](f: From => Seq[To])(implicit ec: ExecutionContext) = mapFlatten[From](in => Enumerator.enumerateSeq1(f(in)))(ec)
  }

  /**
   * A partially-applied function returned by the `mapFlatten` method.
   */
  trait MapFlatten[From] {
    /**
     * @param f Used to transform each input element into an Enumerator.
     * $paramEcSingle
     */
    def apply[To](f: From => Enumerator[To])(implicit ec: ExecutionContext): Enumeratee[From, To]
  }

  /**
   * Create an Enumeratee that transforms its input elements into an Enumerator that is fed into the target Iteratee.
   */
  def mapFlatten[From] = new MapFlatten[From] {
    def apply[To](f: From => Enumerator[To])(implicit ec: ExecutionContext) = new CheckDone[From, To] {
      val pec = ec.prepare()

      def step[A](k: K[To, A]): K[From, Iteratee[To, A]] = {
        case Input.El(e) =>
          new CheckDone[From, To] { def continue[A](k: K[To, A]) = Cont(step(k)) } &> Iteratee.flatten(Future(f(e))(pec).flatMap(_.apply(Cont(k)))(dec))

        case in @ Input.Empty =>
          new CheckDone[From, To] { def continue[A](k: K[To, A]) = Cont(step(k)) } &> k(in)

        case Input.EOF => Done(Cont(k), Input.EOF)
      }

      def continue[A](k: K[To, A]) = Cont(step(k))
    }
  }

  /**
   * A partially-applied function returned by the `mapInputFlatten` method.
   */
  trait MapInputFlatten[From] {
    /**
     * @param f Used to transform each input into an Enumerator.
     * $paramEcSingle
     */
    def apply[To](f: Input[From] => Enumerator[To])(implicit ec: ExecutionContext): Enumeratee[From, To]
  }

  /**
   * Create an Enumeratee that transforms its input into an Enumerator that is fed into the target Iteratee.
   */
  def mapInputFlatten[From] = new MapInputFlatten[From] {
    def apply[To](f: Input[From] => Enumerator[To])(implicit ec: ExecutionContext) = new CheckDone[From, To] {
      val pec = ec.prepare()

      def step[A](k: K[To, A]): K[From, Iteratee[To, A]] = {
        case in =>
          new CheckDone[From, To] { def continue[A](k: K[To, A]) = Cont(step(k)) } &> Iteratee.flatten(Future(f(in))(pec).flatMap(_.apply(Cont(k)))(dec))
      }

      def continue[A](k: K[To, A]) = Cont(step(k))
    }
  }

  /**
   * A partially-applied function returned by the `mapInputM` method.
   */
  trait MapInputM[From] {
    /**
     * @param f Used to transform each input.
     * $paramEcSingle
     */
    def apply[To](f: Input[From] => Future[Input[To]])(implicit ec: ExecutionContext): Enumeratee[From, To]
  }

  /**
   * Like `mapInput`, but allows the map function to asynchronously return the mapped input.
   */
  def mapInputM[From] = new MapInputM[From] {
    def apply[To](f: Input[From] => Future[Input[To]])(implicit ec: ExecutionContext) = new CheckDone[From, To] {
      val pec = ec.prepare()

      def step[A](k: K[To, A]): K[From, Iteratee[To, A]] = {
        case in @ (Input.El(_) | Input.Empty) =>
          new CheckDone[From, To] { def continue[A](k: K[To, A]) = Cont(step(k)) } &> Iteratee.flatten(executeFuture(f(in))(pec).map(k(_))(dec))

        case Input.EOF => Done(Cont(k), Input.EOF)
      }

      def continue[A](k: K[To, A]) = Cont(step(k))
    }
  }

  /**
   * A partially-applied function returned by the `mapM` method.
   */
  trait MapM[E] {
    /**
     * @param f Used to transform each input element.
     * $paramEcSingle
     */
    def apply[NE](f: E => Future[NE])(implicit ec: ExecutionContext): Enumeratee[E, NE]
  }

  /**
   * Like `map`, but allows the map function to asynchronously return the mapped element.
   */
  def mapM[E] = new MapM[E] {
    def apply[NE](f: E => Future[NE])(implicit ec: ExecutionContext): Enumeratee[E, NE] = mapInputM[E] {
      case Input.Empty => Future.successful(Input.Empty)
      case Input.EOF => Future.successful(Input.EOF)
      case Input.El(e) => f(e).map(Input.El(_))(dec)
    }(ec)
  }

  /**
   * A partially-applied function returned by the `map` method.
   */
  trait Map[E] {
    /**
     * @param f A function to transform input elements.
     * $paramEcSingle
     */
    def apply[NE](f: E => NE)(implicit ec: ExecutionContext): Enumeratee[E, NE]
  }

  /**
   * Create an Enumeratee which transforms its input using a given function
   */
  def map[E] = new Map[E] {
    def apply[NE](f: E => NE)(implicit ec: ExecutionContext): Enumeratee[E, NE] = mapInput[E](in => in.map(f))(ec)
  }

  /**
   * Create an Enumeratee that will take `count` input elements to pass to the target Iteratee, and then be done
   *
   * @param count The number of elements to take
   */
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

    def continue[A](k: K[E, A]) = if (count <= 0) Done(Cont(k), Input.EOF) else Cont(step(count)(k))

  }

  /**
   * A partially-applied function returned by the `scanLeft` method.
   */
  trait ScanLeft[From] {
    def apply[To](seed: To)(f: (To, From) => To): Enumeratee[From, To]
  }

  def scanLeft[From] = new ScanLeft[From] {

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

  /**
   * A partially-applied function returned by the `grouped` method.
   */
  trait Grouped[From] {
    def apply[To](folder: Iteratee[From, To]): Enumeratee[From, To]
  }

  /**
   * Create an Enumeratee that groups input using the given Iteratee.
   *
   * This will apply that Iteratee over and over, passing the result each time as the input for the target Iteratee,
   * until EOF is reached.  For example, let's say you had an Iteratee that took a stream of characters and parsed a
   * single line:
   *
   * {{{
   * def takeLine = for {
   *   line <- Enumeratee.takeWhile[Char](_ != '\n') &>> Iteratee.getChunks
   *   _    <- Enumeratee.take(1) &>> Iteratee.ignore[Char]
   * } yield line.mkString
   * }}}
   *
   * This could be used to build an Enumeratee that converts a stream of characters into a stream of lines:
   *
   * {{{
   * def asLines = Enumeratee.grouped(takeLine)
   * }}}
   */
  def grouped[From] = new Grouped[From] {

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
          }(dec)

        case Input.EOF => Iteratee.flatten(f.run.map[Iteratee[From, Iteratee[To, A]]]((c: To) => Done(k(Input.El(c)), Input.EOF))(dec))

      }

      def continue[A](k: K[To, A]) = Cont(step(folder)(k))
    }
  }

  /**
   * Create an Enumeratee that filters the inputs using the given predicate
   *
   * @param predicate A function to filter the input elements.
   * $paramEcSingle
   */
  def filter[E](predicate: E => Boolean)(implicit ec: ExecutionContext): Enumeratee[E, E] = new CheckDone[E, E] {
    val pec = ec.prepare()

    def step[A](k: K[E, A]): K[E, Iteratee[E, A]] = {

      case in @ Input.El(e) => Iteratee.flatten(Future(predicate(e))(pec).map { b =>
        if (b) (new CheckDone[E, E] { def continue[A](k: K[E, A]) = Cont(step(k)) } &> k(in)) else Cont(step(k))
      }(dec))

      case in @ Input.Empty =>
        new CheckDone[E, E] { def continue[A](k: K[E, A]) = Cont(step(k)) } &> k(in)

      case Input.EOF => Done(Cont(k), Input.EOF)

    }

    def continue[A](k: K[E, A]) = Cont(step(k))

  }

  /**
   * Create an Enumeratee that filters the inputs using the negation of the given predicate
   *
   * @param predicate A function to filter the input elements.
   * $paramEcSingle
   */
  def filterNot[E](predicate: E => Boolean)(implicit ec: ExecutionContext): Enumeratee[E, E] = filter[E](e => !predicate(e))(ec)

  /**
   * A partially-applied function returned by the `collect` method.
   */
  trait Collect[From] {
    /**
     * @param transformer A function to transform and filter the input elements with.
     * $paramSingleEc
     */
    def apply[To](transformer: PartialFunction[From, To])(implicit ec: ExecutionContext): Enumeratee[From, To]
  }

  /**
   * Create an Enumeratee that both filters and transforms its input. The input is transformed by the given
   * PartialFunction. If the PartialFunction isn't defined for an input element then that element is discarded.
   */
  def collect[From] = new Collect[From] {
    def apply[To](transformer: PartialFunction[From, To])(implicit ec: ExecutionContext): Enumeratee[From, To] = new CheckDone[From, To] {
      val pec = ec.prepare()

      def step[A](k: K[To, A]): K[From, Iteratee[To, A]] = {

        case in @ Input.El(e) => Iteratee.flatten(Future {
          if (transformer.isDefinedAt(e)) {
            new CheckDone[From, To] { def continue[A](k: K[To, A]) = Cont(step(k)) } &> k(Input.El(transformer(e)))
          } else {
            Cont(step(k))
          }
        }(pec))

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

  /**
   * Create an Enumeratee that drops input until a predicate is satisfied.
   *
   * @param f A predicate to test the input with.
   * $paramEcSingle
   */
  def dropWhile[E](p: E => Boolean)(implicit ec: ExecutionContext): Enumeratee[E, E] = {
    val pec = ec.prepare()
    new CheckDone[E, E] {

      def step[A](k: K[E, A]): K[E, Iteratee[E, A]] = {

        case in @ Input.El(e) => Iteratee.flatten(Future(p(e))(pec).map {
          b => if (b) Cont(step(k)) else (passAlong[E] &> k(in))
        }(dec))

        case in @ Input.Empty => Cont(step(k))

        case Input.EOF => Done(Cont(k), Input.EOF)

      }

      def continue[A](k: K[E, A]) = Cont(step(k))

    }
  }

  /**
   * Create an Enumeratee that passes input through while a predicate is satisfied. Once the predicate
   * fails, no more input is passed through.
   *
   * @param f A predicate to test the input with.
   * $paramEcSingle
   */
  def takeWhile[E](p: E => Boolean)(implicit ec: ExecutionContext): Enumeratee[E, E] = {
    val pec = ec.prepare()
    new CheckDone[E, E] {

      def step[A](k: K[E, A]): K[E, Iteratee[E, A]] = {

        case in @ Input.El(e) => Iteratee.flatten(Future(p(e))(pec).map {
          b => if (b) (new CheckDone[E, E] { def continue[A](k: K[E, A]) = Cont(step(k)) } &> k(in)) else Done(Cont(k), in)
        }(dec))

        case in @ Input.Empty =>
          new CheckDone[E, E] { def continue[A](k: K[E, A]) = Cont(step(k)) } &> k(in)

        case Input.EOF => Done(Cont(k), Input.EOF)
      }

      def continue[A](k: K[E, A]) = Cont(step(k))

    }
  }

  /**
   * Create an Enumeratee that passes input through until a predicate is satisfied. Once the predicate
   * is satisfied, no more input is passed through.
   *
   * @param f A predicate to test the input with.
   * $paramEcSingle
   */
  def breakE[E](p: E => Boolean)(implicit ec: ExecutionContext) = new Enumeratee[E, E] {
    val pec = ec.prepare()
    def applyOn[A](inner: Iteratee[E, A]): Iteratee[E, Iteratee[E, A]] = {
      def step(inner: Iteratee[E, A])(in: Input[E]): Iteratee[E, Iteratee[E, A]] = in match {
        case Input.El(e) => Iteratee.flatten(Future(p(e))(pec).map(b => if (b) Done(inner, in) else stepNoBreak(inner)(in))(dec))
        case _ => stepNoBreak(inner)(in)
      }
      def stepNoBreak(inner: Iteratee[E, A])(in: Input[E]): Iteratee[E, Iteratee[E, A]] =
        inner.pureFlatFold {
          case Step.Cont(k) => {
            val next = k(in)
            next.pureFlatFold {
              case Step.Cont(k) => Cont(step(next))
              case _ => Done(inner, in)
            }(dec)
          }
          case _ => Done(inner, in)
        }(dec)
      Cont(step(inner))
    }
  }

  /**
   * An enumeratee that passes all input through until EOF is reached, redeeming the final iteratee with EOF as the
   * left over input.
   */
  def passAlong[M] = new Enumeratee.CheckDone[M, M] {

    def step[A](k: K[M, A]): K[M, Iteratee[M, A]] = {

      case in @ (Input.El(_) | Input.Empty) => new Enumeratee.CheckDone[M, M] { def continue[A](k: K[M, A]) = Cont(step(k)) } &> k(in)

      case Input.EOF => Done(Cont(k), Input.EOF)
    }
    def continue[A](k: K[M, A]) = Cont(step(k))
  }

  def heading[E](es: Enumerator[E]) = new Enumeratee[E, E] {

    def applyOn[A](it: Iteratee[E, A]): Iteratee[E, Iteratee[E, A]] = passAlong[E] &> Iteratee.flatten(es(it))

  }

  def trailing[M](es: Enumerator[M]) = new Enumeratee.CheckDone[M, M] {

    def step[A](k: K[M, A]): K[M, Iteratee[M, A]] = {

      case in @ (Input.El(_) | Input.Empty) => new Enumeratee.CheckDone[M, M] { def continue[A](k: K[M, A]) = Cont(step(k)) } &> k(in)

      case Input.EOF => Iteratee.flatten((es |>> Cont(k)).map[Iteratee[M, Iteratee[M, A]]](it => Done(it, Input.EOF))(dec))
    }
    def continue[A](k: K[M, A]) = Cont(step(k))
  }

  /**
   * Create an Enumeratee that performs an action when its Iteratee is done.
   *
   * @param action The action to perform.
   * $paramEcSingle
   */
  def onIterateeDone[E](action: () => Unit)(implicit ec: ExecutionContext): Enumeratee[E, E] = new Enumeratee[E, E] {
    val pec = ec.prepare()

    def applyOn[A](iteratee: Iteratee[E, A]): Iteratee[E, Iteratee[E, A]] = passAlong[E](iteratee).map(_.map { a => action(); a }(pec))(dec)

  }

  /**
   * Create an Enumeratee that performs an action on EOF.
   *
   * @param action The action to perform.
   * $paramEcSingle
   */
  def onEOF[E](action: () => Unit)(implicit ec: ExecutionContext): Enumeratee[E, E] = new CheckDone[E, E] {
    val pec = ec.prepare()

    def step[A](k: K[E, A]): K[E, Iteratee[E, A]] = {

      case Input.EOF =>
        Iteratee.flatten(Future(action())(pec).map(_ => Done[E, Iteratee[E, A]](Cont(k), Input.EOF))(dec))

      case in =>
        new CheckDone[E, E] { def continue[A](k: K[E, A]) = Cont(step(k)) } &> k(in)
    }

    def continue[A](k: K[E, A]) = Cont(step(k))

  }

  /**
   * Create an Enumeratee that recovers an iteratee in Error state.
   *
   * This will ignore the input that caused the iteratee's error state
   * and use the previous state of the iteratee to handle the next input.
   *
   * {{{
   *  Enumerator(0, 2, 4) &> Enumeratee.recover { (error, input) =>
   *    Logger.error(f"oops failure occured with input: $input", error)
   *  } &> Enumeratee.map { i =>
   *    8 / i
   *  } |>>> Iteratee.getChunks // => List(4, 2)
   * }}}
   *
   * @param f Called when an error occurs with the cause of the error and the input associated with the error.
   * $paramEcSingle
   */
  def recover[E](f: (Throwable, Input[E]) => Unit = (_: Throwable, _: Input[E]) => ())(implicit ec: ExecutionContext): Enumeratee[E, E] = {
    val pec = ec.prepare()
    new Enumeratee[E, E] {

      def applyOn[A](it: Iteratee[E, A]): Iteratee[E, Iteratee[E, A]] = {

        def step(it: Iteratee[E, A])(input: Input[E]): Iteratee[E, Iteratee[E, A]] = input match {
          case in @ (Input.El(_) | Input.Empty) =>
            val next: Future[Iteratee[E, Iteratee[E, A]]] = it.pureFlatFold[E, Iteratee[E, A]] {
              case Step.Cont(k) =>
                val n = k(in)
                n.pureFlatFold[E, Iteratee[E, A]] {
                  case Step.Cont(k) => Cont(step(n))
                  case _ => Done(n, Input.Empty)
                }(dec)
              case other => Done(other.it, in)
            }(dec).unflatten.map({ s =>
              s.it
            })(dec).recover({
              case e: Throwable =>
                f(e, in)
                Cont(step(it))
            })(pec)
            Iteratee.flatten(next)
          case Input.EOF =>
            Done(it, Input.Empty)
        }

        Cont(step(it))

      }
    }
  }

}
