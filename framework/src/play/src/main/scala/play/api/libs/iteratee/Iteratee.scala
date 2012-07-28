package play.api.libs.iteratee

import play.api.libs.concurrent._

object Iteratee {

  /**
   * flatten a [[play.api.libs.concurrent.Promise]] of [[play.api.libs.iteratee.Iteratee]]] into an Iteratee
   *
   * @param i a promise of iteratee
   */
  def flatten[E, A](i: Promise[Iteratee[E, A]]): Iteratee[E, A] = new Iteratee[E, A] {

    def fold[B](done: (A, Input[E]) => Promise[B],
      cont: (Input[E] => Iteratee[E, A]) => Promise[B],
      error: (String, Input[E]) => Promise[B]): Promise[B] = i.flatMap(_.fold(done, cont, error))
  }

  /**
   * Create an [[play.api.libs.iteratee.Iteratee]] which folds the content of the Input using a given function and an initial state
   *
   * Example:
   * {{{
   *   // Count the number of input elements
   *   def count[E]: Iteratee[E, Int] = Iteratee.fold(0)((c, _) => c + 1)
   * }}}
   *
   * @param state initial state
   * @param f a function folding the previous state and an input to a new state
   */
  def fold[E, A](state: A)(f: (A, E) => A): Iteratee[E, A] = {
    def step(s: A)(i: Input[E]): Iteratee[E, A] = i match {

      case Input.EOF => Done(s, Input.EOF)
      case Input.Empty => Cont[E, A](i => step(s)(i))
      case Input.El(e) => { val s1 = f(s, e); Cont[E, A](i => step(s1)(i)) }
    }
    (Cont[E, A](i => step(state)(i)))
  }

  /**
   * Create an [[play.api.libs.iteratee.Iteratee]] which folds the content of the Input using a given function and an initial state
   *
   * It also gives the opportunity to return a [[play.api.libs.concurrent.Promise]] so that promises are combined in a complete reactive flow of logic.
   *
   *
   * @param state initial state
   * @param f a function folding the previous state and an input to a new promise of state
   */
  def fold1[E, A](state: A)(f: (A, E) => Promise[A]): Iteratee[E, A] = {
    def step(s: A)(i: Input[E]): Iteratee[E, A] = i match {

      case Input.EOF => Done(s, Input.EOF)
      case Input.Empty => Cont[E, A](i => step(s)(i))
      case Input.El(e) => { val newS = f(s, e); flatten(newS.map(s1 => Cont[E, A](i => step(s1)(i)))) }
    }
    (Cont[E, A](i => step(state)(i)))
  }

  /**
   * Create an [[play.api.libs.iteratee.Iteratee]] which folds the content of the Input using a given function and an initial state
   *
   * It also gives the opportunity to return a [[play.api.libs.concurrent.Promise]] so that promises are combined in a complete reactive flow of logic.
   *
   *
   * @param state initial state
   * @param f a function folding the previous state and an input to a new promise of state
   */
  def fold1[E, A](state: Promise[A])(f: (A, E) => Promise[A]): Iteratee[E, A] = {
    flatten(state.map(s => fold1(s)(f)))
  }

  /**
   * Create an [[play.api.libs.iteratee.Iteratee]] which consumes and concatenates all Input chunks
   *
   * Example:
   * {{{
   *   // Get all chunks of input
   *   def getAll: Iteratee[Array[Byte], Array[Byte]] = Iteratee.consume[Array[Byte]]()
   * }}}
   *
   * Chunks type should be viewable as TraversableOnce
   *
   */
  def consume[E] = new {
    def apply[B, That]()(implicit t: E => TraversableOnce[B], bf: scala.collection.generic.CanBuildFrom[E, B, That]): Iteratee[E, That] = {
      fold[E, Seq[E]](Seq.empty) { (els, chunk) =>
        els :+ chunk
      }.mapDone { elts =>
        val builder = bf()
        elts.foreach(builder ++= _)
        builder.result()
      }
    }
  }

  def eofOrElse[E] = new {

    def apply[A, B](otherwise: B)(then: A) = {
      def cont: Iteratee[E, Either[B, A]] = Cont((in: Input[E]) => {
        in match {
          case Input.El(e) => Done(Left(otherwise), in)
          case Input.EOF => Done(Right(then), in)
          case Input.Empty => cont
        }
      })
      cont
    }
  }

  /**
   * @return an [[play.api.libs.iteratee.Iteratee]] which just ignores its input
   */
  def ignore[E]: Iteratee[E, Unit] = fold[E, Unit](())((_, _) => ())

  /**
   * @return an [[play.api.libs.iteratee.Iteratee]] which executes a provided function for every chunk. Returns Done on EOF.
   *
   * Example:
   * {{{
   *   // Get all chunks of input
   *   def printChunks: Iteratee[String, Unit] = Iteratee.foreach[String]( s => println(s) )
   * }}}
   *
   * @param f the function that should be executed for every chunk
   */
  def foreach[E](f: E => Unit): Iteratee[E, Unit] = fold[E, Unit](())((_, e) => f(e))

  /**
   *
   * @return an [[play.api.libs.iteratee.Iteratee]] which pushes the input into the provided [[play.api.libs.iteratee.Iteratee]], starting over again each time it terminates until an EOF is received, collecting a sequence of results of the different use of the iteratee
   *
   * @param i an iteratee used repeatedly to compute a sequence of results
   */
  def repeat[E, A](i: Iteratee[E, A]): Iteratee[E, Seq[A]] = {

    def step(s: Seq[A])(input: Input[E]): Iteratee[E, Seq[A]] = {
      input match {
        case Input.EOF => Done(s, Input.EOF)

        case Input.Empty => Cont(step(s))

        case Input.El(e) => i.pureFlatFold(
          (a, e) => Done(s :+ a, input),
          k => for {
            a <- k(input);
            az <- repeat(i)
          } yield s ++ (a +: az),
          (msg, e) => Error(msg, e))
      }
    }

    Cont(step(Seq.empty[A]))

  }

}

sealed trait Input[+E] {
  def map[U](f: (E => U)): Input[U] = this match {
    case Input.El(e) => Input.El(f(e))
    case Input.Empty => Input.Empty
    case Input.EOF => Input.EOF
  }
}

object Input {

  case class El[+E](e: E) extends Input[E]
  case object Empty extends Input[Nothing]
  case object EOF extends Input[Nothing]

}

/**
 * @tparam E Input type
 * @tparam A Result type of this Iteratee
 */
trait Iteratee[E, +A] {
  self =>

  /**
   * Extracts the computed result of the Iteratee pushing an Input.EOF if necessary
   *
   *  @return a [[play.api.libs.concurrent.Promise]] of the eventually computed result
   */
  def run[AA >: A]: Promise[AA] = fold((a, _) => Promise.pure(a),
    k => k(Input.EOF).fold((a1, _) => Promise.pure(a1),
      _ => sys.error("diverging iteratee after Input.EOF"),
      (msg, e) => sys.error(msg)),
    (msg, e) => sys.error(msg))

  def feed[AA >: A](in: Input[E]): Promise[Iteratee[E, AA]] = {
    Enumerator.enumInput(in) |>> this
  }

  /**
   *
   * This method provides the means to check on the state of the Iteratee and eventually extract a value in a Promise
   * @param done a function that will be called if the Iteratee is a Done
   * @param cont a function that will be called if the Iteratee is a Cont
   * @param error a function that will be called if the Iteratee is an Error
   * @return a [[play.api.libs.concurrent.Promise]] of a value extracted by calling the appropriate provided function
   */
  def fold[B](done: (A, Input[E]) => Promise[B],
    cont: (Input[E] => Iteratee[E, A]) => Promise[B],
    error: (String, Input[E]) => Promise[B]): Promise[B]

  /**
   * Like fold but taking functions returning pure values (not in promises)
   *
   * @return a [[play.api.libs.concurrent.Promise]] of a value extracted by calling the appropriate provided function
   */
  def pureFold[B](done: (A, Input[E]) => B,
    cont: (Input[E] => Iteratee[E, A]) => B,
    error: (String, Input[E]) => B): Promise[B] =
    fold[B](
      (a, e) => Promise.pure(done(a, e)),
      k => Promise.pure(cont(k)),
      (msg, e) => Promise.pure(error(msg, e)))

  /**
   * Like pureFold, except taking functions that return an Iteratee
   *
   * @return an Iteratee extracted by calling the appropriate provided function
   */
  def pureFlatFold[B, C](done: (A, Input[E]) => Iteratee[B, C],
    cont: (Input[E] => Iteratee[E, A]) => Iteratee[B, C],
    error: (String, Input[E]) => Iteratee[B, C]): Iteratee[B, C] =
    Iteratee.flatten(pureFold(done, cont, error))

  def flatFold[B, C](done: (A, Input[E]) => Promise[Iteratee[B, C]],
    cont: (Input[E] => Iteratee[E, A]) => Promise[Iteratee[B, C]],
    error: (String, Input[E]) => Promise[Iteratee[B, C]]): Iteratee[B, C] = Iteratee.flatten(fold(done, cont, error))

  def mapDone[B](f: A => B): Iteratee[E, B] =
    this.pureFlatFold((a, e) => Done(f(a), e),
      k => Cont((in: Input[E]) => k(in).mapDone(f)),
      (err, e) => Error(err, e))

  /**
   *
   * Uses the provided function to transform the Iteratee's computed result when the Iteratee is done.
   *
   * @param f a function for tranforming the computed result
   */
  def map[B](f: A => B): Iteratee[E, B] = this.flatMap(a => Done(f(a), Input.Empty))

  /**
   * On Done of this Iteratee, the result is passed to the provided function, and the resulting Iteratee is used to continue consuming input
   *
   * If the resulting Iteratee of evaluating the f function is a Done then its left Input is ignored and its computed result is wrapped in a Done and returned
   */
  def flatMap[B](f: A => Iteratee[E, B]): Iteratee[E, B] = self.pureFlatFold(
    {
      case (a, Input.Empty) => f(a)
      case (a, e) => f(a).pureFlatFold(
        (a, _) => Done(a, e),
        k => k(e),
        (msg, e) => Error(msg, e))
    },
    k => Cont(in => k(in).flatMap(f)),
    (msg, e) => Error(msg, e))

  /**
   * Like flatMap except that it concatenates left inputs if the Iteratee returned by evaluating f is a Done.
   */
  def flatMapTraversable[B, X](f: A => Iteratee[E, B])(implicit p: E => scala.collection.TraversableLike[X, E], bf: scala.collection.generic.CanBuildFrom[E, X, E]): Iteratee[E, B] = self.pureFlatFold(
    {
      case (a, Input.Empty) => f(a)
      case (a, e) => f(a).pureFlatFold(
        (a, eIn) => {
          val fullIn = (e, eIn) match {
            case (Input.Empty, in) => in
            case (in, Input.Empty) => in
            case (Input.EOF, _) => Input.EOF
            case (in, Input.EOF) => in
            case (Input.El(e1), Input.El(e2)) => Input.El[E](p(e1) ++ p(e2))
          }

          Done(a, fullIn)
        },
        k => k(e),
        (msg, e) => Error(msg, e))
    },
    k => Cont(in => k(in).flatMap(f)),
    (msg, e) => Error(msg, e))

  def joinI[AIn](implicit in: A <:< Iteratee[_, AIn]): Iteratee[E, AIn] = {
    this.flatMap { a =>
      val inner = in(a)
      inner.pureFlatFold(
        (a, _) => Done(a, Input.Empty),
        k => k(Input.EOF).pureFlatFold(
          (a, _) => Done(a, Input.Empty),
          k => Error("divergent inner iteratee on joinI after EOF", Input.EOF),
          (msg, e) => Error(msg, Input.EOF)),
        (msg, e) => Error(msg, Input.Empty))
    }

  }

  def joinConcatI[AIn, X](implicit in: A <:< Iteratee[E, AIn], p: E => scala.collection.TraversableLike[X, E], bf: scala.collection.generic.CanBuildFrom[E, X, E]): Iteratee[E, AIn] = {
    this.flatMapTraversable { a =>
      val inner = in(a)
      inner.pureFlatFold(
        (a, e) => Done(a, e),
        k => k(Input.EOF).pureFlatFold(
          (a, e) => Done(a, e),
          k => Error("divergent inner iteratee on joinI after EOF", Input.EOF),
          (msg, e) => Error(msg, Input.EOF)),
        (msg, e) => Error(msg, Input.Empty))
    }

  }
}

object Done {
  /**
   * Create an [[play.api.libs.iteratee.Iteratee]] in the “done” state.
   * @param a Result
   * @param e Remaining unused input
   */
  def apply[E, A](a: A, e: Input[E]): Iteratee[E, A] = new Iteratee[E, A] {
    def fold[B](done: (A, Input[E]) => Promise[B],
      cont: (Input[E] => Iteratee[E, A]) => Promise[B],
      error: (String, Input[E]) => Promise[B]): Promise[B] = done(a, e)

  }

}

object Cont {
  /**
   * Create an [[play.api.libs.iteratee.Iteratee]] in the “cont” state.
   * @param k Continuation which will compute the next Iteratee state according to an input
   */
  def apply[E, A](k: Input[E] => Iteratee[E, A]): Iteratee[E, A] = new Iteratee[E, A] {
    def fold[B](done: (A, Input[E]) => Promise[B],
      cont: (Input[E] => Iteratee[E, A]) => Promise[B],
      error: (String, Input[E]) => Promise[B]): Promise[B] = cont(k)

  }
}
object Error {
  /**
   * Create an [[play.api.libs.iteratee.Iteratee]] in the “error” state.
   * @param msg Error message
   * @param e The input that caused the error
   */
  def apply[E](msg: String, e: Input[E]): Iteratee[E, Nothing] = new Iteratee[E, Nothing] {
    def fold[B](done: (Nothing, Input[E]) => Promise[B],
      cont: (Input[E] => Iteratee[E, Nothing]) => Promise[B],
      error: (String, Input[E]) => Promise[B]): Promise[B] = error(msg, e)

  }
}

/**
 * Pushes input to an [[play.api.libs.iteratee.Iteratee]]
 * @type E Type of the input
 */
trait Enumerator[E] {
  parent =>

  /**
   * Apply this Enumerator to an Iteratee
   */
  def apply[A](i: Iteratee[E, A]): Promise[Iteratee[E, A]]

  /**
   * Alias for `apply`
   */
  def |>>[A](i: Iteratee[E, A]): Promise[Iteratee[E, A]] = apply(i)

  /**
   * Sequentially combine this Enumerator with another Enumerator
   */
  def andThen(e: Enumerator[E]): Enumerator[E] = new Enumerator[E] {
    def apply[A](i: Iteratee[E, A]): Promise[Iteratee[E, A]] = parent.apply(i).flatMap(e.apply) //bad implementation, should remove Input.EOF in the end of first
  }

  def interleave[B >: E](other: Enumerator[B]): Enumerator[B] = Enumerator.interleave(this, other)

  def >-[B >: E](other: Enumerator[B]): Enumerator[B] = interleave(other)

  /**
   * Compose this Enumerator with an Enumeratee
   */
  def &>[To](enumeratee: Enumeratee[E, To]): Enumerator[To] = new Enumerator[To] {

    def apply[A](i: Iteratee[To, A]): Promise[Iteratee[To, A]] = {
      val transformed = enumeratee.applyOn(i)
      val xx = parent |>> transformed
      xx.flatMap(_.run)

    }

  }

  def through[To](enumeratee: Enumeratee[E, To]): Enumerator[To] = &>(enumeratee)

  /**
   * Alias for `andThen`
   */
  def >>>(e: Enumerator[E]): Enumerator[E] = andThen(e)

  def map[U](f: E => U): Enumerator[U] = parent &> Enumeratee.map[E](f)

  def mapInput[U](f: Input[E] => Input[U]) = parent &> Enumeratee.mapInput[E](f)

}

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
      it.pureFlatFold(
        (_, _) => Done(it, Input.Empty),
        k => continue(k),
        (_, _) => Done(it, Input.Empty))

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

    def getInside[T](it: Iteratee[E, T]): Promise[(Option[Either[(String, Input[E]), (T, Input[E])]], Iteratee[E, T])] = {
      it.pureFold(
        (a, e) => Some(Right((a, e))),
        k => None,
        (msg, e) => Some(Left((msg, e)))
      ).map(r => (r, it))

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

        case Input.EOF => Done(k(Input.EOF), Input.EOF)
      }

      def continue[A](k: K[To, A]) = Cont(step(k))
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

      case in @ Input.El(_) if remaining == 1 => 
        Done(k(in), Input.Empty)
      
      case in @ Input.El(_) if remaining > 1 =>
        new CheckDone[E, E] { def continue[A](k: K[E, A]) = Cont(step(remaining - 1)(k)) } &> k(in)

      case in @ Input.Empty if remaining > 0 =>
        new CheckDone[E, E] { def continue[A](k: K[E, A]) = Cont(step(remaining)(k)) } &> k(in)

      case Input.EOF => 
        Done(k(Input.EOF), Input.EOF)

      case in => 
        Done(Cont(k), in)
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

        case Input.EOF => Done(k(Input.EOF), Input.EOF)

      }

      def continue[A](k: K[To, A]) = Cont(step(seed)(k))
    }
  }

  def grouped[From] = new {

    def apply[To](folder: Iteratee[From, To]): Enumeratee[From, To] = new CheckDone[From, To] {

      def step[A](f: Iteratee[From, To])(k: K[To, A]): K[From, Iteratee[To, A]] = {

        case in @ (Input.El(_) | Input.Empty) =>

          Iteratee.flatten(f.feed(in)).pureFlatFold(
            (a, _) => new CheckDone[From, To] { def continue[A](k: K[To, A]) = Cont(step(folder)(k)) } &> k(Input.El(a)),
            kF => Cont(step(Cont(kF))(k)),
            (msg, e) => Error(msg, in))

        case Input.EOF => Iteratee.flatten(f.run.map((c:To) => Done(k(Input.El(c)), Input.EOF)))

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

      case Input.EOF => Done(k(Input.EOF), Input.EOF)

    }

    def continue[A](k: K[E, A]) = Cont(step(k))

  }



  def collect[From] = new {

    def apply[To](transformer: PartialFunction[From, To]): Enumeratee[From, To] = new CheckDone[From, To] {

      def step[A](k: K[To, A]): K[From, Iteratee[To, A]] = {

        case in @ Input.El(e) if transformer.isDefinedAt(e) =>
          new CheckDone[From, To] { def continue[A](k: K[To, A]) = Cont(step(k)) } &> k(in.map(transformer))

        case in @ Input.El(e) => Cont(step(k))

        case in @ Input.Empty =>
          new CheckDone[From, To] { def continue[A](k: K[To, A]) = Cont(step(k)) } &> k(in)

        case Input.EOF => Done(k(Input.EOF), Input.EOF)

      }

      def continue[A](k: K[To, A]) = Cont(step(k))

    }
  }

  def drop[E](count: Int): Enumeratee[E, E] = new Enumeratee[E, E] {

    def applyOn[A](iteratee: Iteratee[E, A]): Iteratee[E, Iteratee[E, A]] = {

      def step(counter: Int, inner: Iteratee[E, A])(in: Input[E]): Iteratee[E, Iteratee[E, A]] = {

        in match {

          case Input.El(e) if counter > 0 => Cont(step(counter - 1, inner))

          case Input.El(e) => inner.pureFlatFold(
            (_, _) => Done(inner, in),
            k => Cont(step(0, k(in))),
            (_, _) => Done(inner, in))

          case Input.EOF => inner.pureFlatFold(
            (_, _) => Done(inner, Input.EOF),
            k => Done(k(Input.EOF), Input.EOF),
            (_, _) => Done(inner, Input.EOF))

          case Input.Empty => Cont(step(counter, inner))

        }

      }

      Cont(step(count, iteratee))

    }

  }

  def takeWhile[E](p: E => Boolean): Enumeratee[E, E] = new Enumeratee[E, E] {

    def applyOn[A](iteratee: Iteratee[E, A]): Iteratee[E, Iteratee[E, A]] = {

      def step(inner: Iteratee[E, A])(in: Input[E]): Iteratee[E, Iteratee[E, A]] = {

        in match {
          case Input.El(e) if !p(e) => Done(inner, in)
          case Input.El(e) => inner.pureFlatFold(
            (_, _) => Cont(step(inner)),
            k => Cont(step(k(in))),
            (_, _) => Cont(step(inner)))

          case Input.EOF => inner.pureFlatFold(
            (_, _) => Done(inner, Input.EOF),
            k => Done(k(Input.EOF), Input.EOF),
            (_, _) => Done(inner, Input.EOF))

          case Input.Empty => Cont(step(inner))
        }
      }

      Cont(step(iteratee))
    }

  }

  def breakE[E](p: E => Boolean) = new Enumeratee[E, E] {
    def applyOn[A](inner: Iteratee[E, A]): Iteratee[E, Iteratee[E, A]] = {
      def step(inner: Iteratee[E, A])(in: Input[E]): Iteratee[E, Iteratee[E, A]] = {
        in match {
          case Input.El(e) if (p(e)) => Done(inner, in)
          case _ =>
            inner.pureFlatFold(
              (_, _) => Done(inner, in),
              k => {
                val next = k(in)
                next.pureFlatFold(
                  (_, _) => Done(inner, in),
                  k => Cont(step(next)),
                  (_, _) => Done(inner, in))
              },
              (_, _) => Done(inner, in))
        }

      }
      Cont(step(inner))

    }
  }
}

object Enumerator {

  def flatten[E](eventuallyEnum: Promise[Enumerator[E]]): Enumerator[E] = new Enumerator[E] {

    def apply[A](it: Iteratee[E, A]): Promise[Iteratee[E, A]] = eventuallyEnum.flatMap(_.apply(it))

  }

  def enumInput[E](e: Input[E]) = new Enumerator[E] {
    def apply[A](i: Iteratee[E, A]): Promise[Iteratee[E, A]] =
      i.fold((a, e) => Promise.pure(i),
        k => Promise.pure(k(e)),
        (_, _) => Promise.pure(i))

  }

  def interleave[E1, E2 >: E1](e1: Enumerator[E1], e2: Enumerator[E2]): Enumerator[E2] = new Enumerator[E2] {

    import scala.concurrent.stm._

    def apply[A](it: Iteratee[E2, A]): Promise[Iteratee[E2, A]] = {

      var iter: Ref[Iteratee[E2, A]] = Ref(it)
      val attending: Ref[Option[(Boolean, Boolean)]] = Ref(Some(true, true))
      val result = Promise[Iteratee[E2, A]]()

      def redeemResultIfNotYet() = {
        val toRedeem = atomic { implicit transaction =>
          if (attending().isDefined) {
            attending() = None
            val it = iter()
            Some(it)
          } else None
        }
        toRedeem.foreach(result.redeem(_))
      }

      def iteratee[EE <: E2](f: ((Boolean, Boolean)) => (Boolean, Boolean)): Iteratee[EE, Unit] = {
        def step(in: Input[EE]): Iteratee[EE, Unit] = {
          in match {
            case Input.El(_) | Input.Empty =>
              val p = Promise[Iteratee[E2, A]]()
              val i = iter.single.swap(Iteratee.flatten(p))
              val nextI = Iteratee.flatten(i.feed(in))
              p.redeem(nextI)
              nextI.pureFlatFold(
                (a, e) => {
                  redeemResultIfNotYet()
                  Done((), Input.Empty: Input[EE])
                },
                k => Cont(step),
                (msg, e) => {
                  redeemResultIfNotYet()
                  Error(msg, Input.Empty: Input[EE])
                })

            case Input.EOF => {
              if (attending.single.transformAndGet { _.map(f) } == Some((false, false)))
                redeemResultIfNotYet()
              Done((), Input.Empty)
            }
          }
        }
        Cont(step)
      }

      val itE1 = iteratee[E1] { case (l, r) => (false, r) }
      val itE2 = iteratee[E2] { case (l, r) => (l, false) }
      val r1 = e1 |>> itE1
      val r2 = e2 |>> itE2
      r1.flatMap(_ => r2).extend1 {
        case Redeemed(_) => redeemResultIfNotYet()
        case Thrown(e) => result.throwing(e)
      }
      result
    }

  }

  trait Pushee[E] {

    def push(item: E): Boolean

    def close()

  }

  def imperative[E](
    onStart: => Unit = () => (),
    onComplete: => Unit = () => (),
    onError: (String, Input[E]) => Unit = (_: String, _: Input[E]) => ()): PushEnumerator[E] = new PushEnumerator[E](onStart, onComplete, onError)

  def pushee[E](
    onStart: Pushee[E] => Unit,
    onComplete: => Unit = () => (),
    onError: (String, Input[E]) => Unit = (_: String, _: Input[E]) => ()) = new Enumerator[E] {

    def apply[A](it: Iteratee[E, A]): Promise[Iteratee[E, A]] = {
      var iteratee: Iteratee[E, A] = it
      var promise: Promise[Iteratee[E, A]] with Redeemable[Iteratee[E, A]] = new STMPromise[Iteratee[E, A]]()

      val pushee = new Pushee[E] {
        def close() {
          if (iteratee != null) {
            iteratee.feed(Input.EOF).map(result => promise.redeem(result))
            iteratee = null
            promise = null
          }
        }
        def push(item: E): Boolean = {
          if (iteratee != null) {
            iteratee = iteratee.pureFlatFold[E, A](

              // DONE
              (a, in) => {
                onComplete
                Done(a, in)
              },

              // CONTINUE
              k => {
                val next = k(Input.El(item))
                next.pureFlatFold(
                  (a, in) => {
                    onComplete
                    next
                  },
                  _ => next,
                  (_, _) => next)
              },

              // ERROR
              (e, in) => {
                onError(e, in)
                Error(e, in)
              })
            true
          } else {
            false
          }
        }
      }
      onStart(pushee)
      promise
    }

  }

  import scalax.io.JavaConverters._

  def fromCallback1[E](retriever: Boolean => Promise[Option[E]],
    onComplete: () => Unit = () => (),
    onError: (String, Input[E]) => Unit = (_: String, _: Input[E]) => ()) = new Enumerator[E] {
    def apply[A](it: Iteratee[E, A]): Promise[Iteratee[E, A]] = {

      var iterateeP = Promise[Iteratee[E, A]]()

      def step(it: Iteratee[E, A], initial: Boolean = false) {

        val next = it.fold(
          (a, e) => { iterateeP.redeem(it); Promise.pure(None) },
          k => {
            retriever(initial).map {
              case None => {
                val remainingIteratee = k(Input.EOF)
                iterateeP.redeem(remainingIteratee)
                None
              }
              case Some(read) => {
                val nextIteratee = k(Input.El(read))
                Some(nextIteratee)
              }
            }
          },
          (_, _) => { iterateeP.redeem(it); Promise.pure(None) }
        )

        next.extend1 {
          case Redeemed(Some(i)) => step(i)
          case Thrown(e) => 
            iterateeP.throwing(e)
            onComplete()
          case _ => throw new RuntimeException("should be either Redeemed or Thrown")
        }

      }

      step(it, true)
      iterateeP
    }
  }

  def fromCallback[E](retriever: () => Promise[Option[E]],
    onComplete: () => Unit = () => (),
    onError: (String, Input[E]) => Unit = (_: String, _: Input[E]) => ()) = new Enumerator[E] {
    def apply[A](it: Iteratee[E, A]): Promise[Iteratee[E, A]] = {

      var iterateeP = Promise[Iteratee[E, A]]()

      def step(it: Iteratee[E, A]) {

        val next = it.fold(
          (a, e) => { iterateeP.redeem(it); Promise.pure(None) },
          k => {
            retriever().map {
              case None => {
                val remainingIteratee = k(Input.EOF)
                iterateeP.redeem(remainingIteratee)
                None
              }
              case Some(read) => {
                val nextIteratee = k(Input.El(read))
                Some(nextIteratee)
              }
            }
          },
          (_, _) => { iterateeP.redeem(it); Promise.pure(None) }
        )

        next.extend1 {
          case Redeemed(Some(i)) => step(i)
          case _ => onComplete()
        }

      }

      step(it)
      iterateeP
    }
  }

  def fromStream(input: java.io.InputStream, chunkSize: Int = 1024 * 8) = {

    fromCallback(() => {
      val buffer = new Array[Byte](chunkSize)
      val chunk = input.read(buffer) match {
        case -1 => None
        case read =>
          val input = new Array[Byte](read)
          System.arraycopy(buffer, 0, input, 0, read)
          Some(input)
      }
      Promise.pure(chunk)
    }, input.close)
  }

  def fromFile(file: java.io.File, chunkSize: Int = 1024 * 8): Enumerator[Array[Byte]] = fromStream(new java.io.FileInputStream(file), chunkSize)

  def eof[A] = enumInput[A](Input.EOF)

  /**
   * Create an Enumerator from a set of values
   *
   * Example:
   * {{{
   *   val enumerator: Enumerator[String] = Enumerator("kiki", "foo", "bar")
   * }}}
   */
  def apply[E](in: E*): Enumerator[E] = new Enumerator[E] {

    def apply[A](i: Iteratee[E, A]): Promise[Iteratee[E, A]] = enumerate(in, i)

  }

  private def enumerate[E, A]: (Seq[E], Iteratee[E, A]) => Promise[Iteratee[E, A]] = { (l, i) =>
    l.foldLeft(Promise.pure(i))((i, e) =>
      i.flatMap(it => it.pureFold((_, _) => it,
        k => k(Input.El(e)),
        (_, _) => it)))
  }



}

class PushEnumerator[E] private[iteratee] (
    onStart: => Unit = () => (),
    onComplete: => Unit = () => (),
    onError: (String, Input[E]) => Unit = (_: String, _: Input[E]) => ()) extends Enumerator[E] with Enumerator.Pushee[E] {

  var iteratee: Iteratee[E, _] = _
  var promise: Promise[Iteratee[E, _]] with Redeemable[Iteratee[E, _]] = _

  def apply[A](it: Iteratee[E, A]): Promise[Iteratee[E, A]] = {
    onStart
    iteratee = it.asInstanceOf[Iteratee[E, _]]
    val newPromise = new STMPromise[Iteratee[E, A]]()
    promise = newPromise.asInstanceOf[Promise[Iteratee[E, _]] with Redeemable[Iteratee[E, _]]]
    newPromise
  }

  def close() {
    if (iteratee != null) {
      iteratee.feed(Input.EOF).map(result => promise.redeem(result))
      iteratee = null
      promise = null
    }
  }

  def push(item: E): Boolean = {
    if (iteratee != null) {
      iteratee = iteratee.pureFlatFold[E, Any](

        // DONE
        (a, in) => {
          onComplete
          Done(a, in)
        },

        // CONTINUE
        k => {
          val next = k(Input.El(item))
          next.pureFlatFold(
            (a, in) => {
              onComplete
              next
            },
            _ => next,
            (_, _) => next)
        },

        // ERROR
        (e, in) => {
          onError(e, in)
          Error(e, in)
        })
      true
    } else {
      false
    }
  }

}

object Parsing {

  sealed trait MatchInfo[A] {
    def content: A
    def isMatch = this match {
      case Matched(_) => true
      case Unmatched(_) => false
    }
  }
  case class Matched[A](val content: A) extends MatchInfo[A]
  case class Unmatched[A](val content: A) extends MatchInfo[A]

  def search(needle: Array[Byte]): Enumeratee[Array[Byte], MatchInfo[Array[Byte]]] = new Enumeratee[Array[Byte], MatchInfo[Array[Byte]]] {
    val needleSize = needle.size
    val fullJump = needleSize
    val jumpBadCharecter: (Byte => Int) = {
      val map = Map(needle.dropRight(1).reverse.zipWithIndex.reverse: _*) //remove the last
      byte => map.get(byte).map(_ + 1).getOrElse(fullJump)
    }

    def applyOn[A](inner: Iteratee[MatchInfo[Array[Byte]], A]): Iteratee[Array[Byte], Iteratee[MatchInfo[Array[Byte]], A]] = {

      Iteratee.flatten(inner.fold((a, e) => Promise.pure(Done(Done(a, e), Input.Empty: Input[Array[Byte]])),
        k => Promise.pure(Cont(step(Array[Byte](), Cont(k)))),
        (err, r) => throw new Exception()))

    }
    def scan(previousMatches: List[MatchInfo[Array[Byte]]], piece: Array[Byte], startScan: Int): (List[MatchInfo[Array[Byte]]], Array[Byte]) = {
      if (piece.length < needleSize) {
        (previousMatches, piece)
      } else {
        val fullMatch = Range(needleSize - 1, -1, -1).forall(scan => needle(scan) == piece(scan + startScan))
        if (fullMatch) {
          val (prefix, then) = piece.splitAt(startScan)
          val (matched, left) = then.splitAt(needleSize)
          val newResults = previousMatches ++ List(Unmatched(prefix), Matched(matched)) filter (!_.content.isEmpty)

          if (left.length < needleSize) (newResults, left) else scan(newResults, left, 0)

        } else {
          val jump = jumpBadCharecter(piece(startScan + needleSize - 1))
          val isFullJump = jump == fullJump
          val newScan = startScan + jump
          if (newScan + needleSize > piece.length) {
            val (prefix, suffix) = (piece.splitAt(startScan))
            (previousMatches ++ List(Unmatched(prefix)), suffix)
          } else scan(previousMatches, piece, newScan)
        }
      }
    }

    def step[A](rest: Array[Byte], inner: Iteratee[MatchInfo[Array[Byte]], A])(in: Input[Array[Byte]]): Iteratee[Array[Byte], Iteratee[MatchInfo[Array[Byte]], A]] = {

      in match {
        case Input.Empty => Cont(step(rest, inner)) //here should rather pass Input.Empty along

        case Input.EOF => Done(inner, Input.El(rest))

        case Input.El(chunk) =>
          val all = rest ++ chunk
          def inputOrEmpty(a: Array[Byte]) = if (a.isEmpty) Input.Empty else Input.El(a)

          Iteratee.flatten(inner.fold((a, e) => Promise.pure(Done(Done(a, e), inputOrEmpty(rest))),
            k => {
              val (result, suffix) = scan(Nil, all, 0)
              val fed = result.filter(!_.content.isEmpty).foldLeft(Promise.pure(Array[Byte](), Cont(k))) { (p, m) =>
                p.flatMap(i => i._2.fold((a, e) => Promise.pure((i._1 ++ m.content, Done(a, e))),
                  k => Promise.pure((i._1, k(Input.El(m)))),
                  (err, e) => throw new Exception()))
              }
              fed.flatMap {
                case (ss, i) => i.fold((a, e) => Promise.pure(Done(Done(a, e), inputOrEmpty(ss ++ suffix))),
                  k => Promise.pure(Cont[Array[Byte], Iteratee[MatchInfo[Array[Byte]], A]]((in: Input[Array[Byte]]) => in match {
                    case Input.EOF => Done(k(Input.El(Unmatched(suffix))), Input.EOF) //suffix maybe empty
                    case other => step(ss ++ suffix, Cont(k))(other)
                  })),
                  (err, e) => throw new Exception())
              }
            },
            (err, e) => throw new Exception()))
      }
    }
  }
}
