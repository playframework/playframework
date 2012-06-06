package play.api.libs.iteratee

import play.api.libs.concurrent._
import play.api.libs.concurrent.execution.defaultContext

object Iteratee {

  /**
   * flatten a [[play.api.libs.concurrent.Promise]] of [[play.api.libs.iteratee.Iteratee]]] into an Iteratee
   *
   * @param i a promise of iteratee
   */
  def flatten[E, A](i: Promise[Iteratee[E, A]]): Iteratee[E, A] = new Iteratee[E, A] {

    def fold[B](folder: Step[E,A] => Promise[B]): Promise[B] = i.flatMap(_.fold(folder))

  }

  def isDoneOrError[E, A](it: Iteratee[E, A]): Promise[Boolean] = it.pureFold{ case Step.Cont(_) => false; case _ => true }

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
  def foldM[E, A](state: A)(f: (A, E) => Promise[A]): Iteratee[E, A] = {
    def step(s: A)(i: Input[E]): Iteratee[E, A] = i match {

      case Input.EOF => Done(s, Input.EOF)
      case Input.Empty => Cont[E, A](i => step(s)(i))
      case Input.El(e) => { val newS = f(s, e); flatten(newS.map(s1 => Cont[E, A](i => step(s1)(i)))) }
    }
    (Cont[E, A](i => step(state)(i)))
  }

  def fold2[E, A](state: A)(f: (A, E) => Promise[(A,Boolean)]): Iteratee[E, A] = {
    def step(s: A)(i: Input[E]): Iteratee[E, A] = i match {

      case Input.EOF => Done(s, Input.EOF)
      case Input.Empty => Cont[E, A](i => step(s)(i))
      case Input.El(e) => { val newS = f(s, e); flatten(newS.map { case (s1, done) => if (!done) Cont[E, A](i => step(s1)(i)) else Done(s1, Input.Empty) }) }
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

  def head[E]:Iteratee[E,Option[E]] = {

      def step:K[E,Option[E]] = {
        case Input.Empty => Cont(step)
        case Input.EOF => Done(None,Input.EOF)
        case Input.El(e)  => Done(Some(e), Input.Empty)
      }
      Cont(step)
  }

  def getChunks[E]: Iteratee[E,List[E]] = fold[E, List[E]](Nil) { (els, chunk) => els :+ chunk }

  def skipToEof[E]: Iteratee[E,Unit] = {
    def cont: Iteratee[E,Unit] = Cont {
      case Input.EOF => Done((),Input.EOF)
      case _ => cont
    }
    cont
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

        case Input.El(e) => i.pureFlatFold {
          case Step.Done(a, e) => Done(s :+ a, input)
          case Step.Cont(k) => for {
            a <- k(input);
            az <- repeat(i)
          } yield s ++ (a +: az)
          case Step.Error(msg, e) => Error(msg, e)
        }
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

sealed trait Step[E,+A] {

  lazy val it:Iteratee[E,A] = this match {
    case Step.Done(a,e) => Done(a,e)
    case Step.Cont(k) => Cont(k)
    case Step.Error(msg,e) => Error(msg,e)
  }

}

object Step {
  case class Done[+A,E](a:A, remaining:Input[E]) extends Step[E,A]
  case class Cont[E,+A](k: Input[E] => Iteratee[E,A]) extends Step[E,A]
  case class Error[E](msg:String, input:Input[E]) extends Step[E,Nothing]
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
  def run[AA >: A]: Promise[AA] = fold({
    case Step.Done(a,_) => Promise.pure(a)
    case Step.Cont(k) => k(Input.EOF).fold({
      case Step.Done(a1,_) => Promise.pure(a1)
      case Step.Cont(_) => sys.error("diverging iteratee after Input.EOF")
      case Step.Error(msg,e) => sys.error(msg)
    })
    case Step.Error(msg,e) => sys.error(msg)
  })

  def feed[AA >: A](in: Input[E]): Promise[Iteratee[E, AA]] = {
    Enumerator.enumInput(in) |>> this
  }

  def unflatten: Promise[Step[E,A]] = pureFold(identity)

  /**
   *
   * This method provides the means to check on the state of the Iteratee and eventually extract a value in a Promise
   * @param done a function that will be called if the Iteratee is a Done
   * @param cont a function that will be called if the Iteratee is a Cont
   * @param error a function that will be called if the Iteratee is an Error
   * @return a [[play.api.libs.concurrent.Promise]] of a value extracted by calling the appropriate provided function
   */
  def fold1[B](done: (A, Input[E]) => Promise[B],
    cont: (Input[E] => Iteratee[E, A]) => Promise[B],
    error: (String, Input[E]) => Promise[B]): Promise[B] = fold({
      case Step.Done(a,e) => done(a,e)
      case Step.Cont(k) => cont(k)
      case Step.Error(msg,e) => error(msg,e)
    })

  def fold[B](folder: Step[E,A] => Promise[B]): Promise[B]

  /**
   * Like fold but taking functions returning pure values (not in promises)
   *
   * @return a [[play.api.libs.concurrent.Promise]] of a value extracted by calling the appropriate provided function
   */
  def pureFold[B](folder: Step[E,A] => B): Promise[B] = fold(s => Promise.pure(folder(s)))

  /**
   * Like pureFold, except taking functions that return an Iteratee
   *
   * @return an Iteratee extracted by calling the appropriate provided function
   */
  def pureFlatFold[B,C](folder: Step[E,A] => Iteratee[B,C]): Iteratee[B,C] = Iteratee.flatten(pureFold(folder))

  def flatFold[B, C](done: (A, Input[E]) => Promise[Iteratee[B, C]],
    cont: (Input[E] => Iteratee[E, A]) => Promise[Iteratee[B, C]],
    error: (String, Input[E]) => Promise[Iteratee[B, C]]): Iteratee[B, C] = Iteratee.flatten(fold1(done, cont, error))

  def mapDone[B](f: A => B): Iteratee[E, B] =
    this.pureFlatFold{
      case Step.Done(a,e) => Done(f(a), e)
      case Step.Cont(k) =>  Cont((in: Input[E]) => k(in).mapDone(f))
      case Step.Error(err,e) => Error(err, e)
    }

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
  def flatMap[B](f: A => Iteratee[E, B]): Iteratee[E, B] = self.pureFlatFold{
      case Step.Done(a, Input.Empty) => f(a)
      case Step.Done(a, e) => f(a).pureFlatFold {
        case Step.Done(a, _) => Done(a, e)
        case Step.Cont(k) => k(e)
        case Step.Error(msg, e) => Error(msg, e)
      }
      case Step.Cont(k) => Cont(in => k(in).flatMap(f))
      case Step.Error(msg, e) => Error(msg, e)
  }

  def flatMapInput[B](f: Step[E,A] => Iteratee[E, B]): Iteratee[E, B] = self.pureFlatFold(f)

  /**
   * Like flatMap except that it concatenates left inputs if the Iteratee returned by evaluating f is a Done.
   */
  def flatMapTraversable[B, X](f: A => Iteratee[E, B])(implicit p: E => scala.collection.TraversableLike[X, E], bf: scala.collection.generic.CanBuildFrom[E, X, E]): Iteratee[E, B] = self.pureFlatFold{
    case Step.Done(a, Input.Empty) => f(a)
    case Step.Done(a, e) => f(a).pureFlatFold {
      case Step.Done(a, eIn) => {
        val fullIn = (e, eIn) match {
          case (Input.Empty, in) => in
          case (in, Input.Empty) => in
          case (Input.EOF, _) => Input.EOF
          case (in, Input.EOF) => in
          case (Input.El(e1), Input.El(e2)) => Input.El[E](p(e1) ++ p(e2))
        }

        Done(a, fullIn)
      }
      case Step.Cont(k) => k(e)
      case Step.Error(msg, e) => Error(msg, e)
    }
    case Step.Cont(k) => Cont(in => k(in).flatMap(f))
    case Step.Error(msg, e) => Error(msg, e)
  }

  def joinI[AIn](implicit in: A <:< Iteratee[_, AIn]): Iteratee[E, AIn] = {
    this.flatMap { a =>
      val inner = in(a)
      inner.pureFlatFold {
        case Step.Done(a, _) => Done(a, Input.Empty)
        case Step.Cont(k) => k(Input.EOF).pureFlatFold {
          case Step.Done(a, _) => Done(a, Input.Empty)
          case Step.Cont(k) => Error("divergent inner iteratee on joinI after EOF", Input.EOF)
          case Step.Error(msg, e) => Error(msg, Input.EOF)
        }
        case Step.Error(msg, e) => Error(msg, Input.Empty)
      }
    }
  }

  def joinConcatI[AIn, X](implicit in: A <:< Iteratee[E, AIn], p: E => scala.collection.TraversableLike[X, E], bf: scala.collection.generic.CanBuildFrom[E, X, E]): Iteratee[E, AIn] = {
    this.flatMapTraversable { a =>
      val inner = in(a)
      inner.pureFlatFold {
        case Step.Done(a, e) => Done(a, e)
        case Step.Cont(k) => k(Input.EOF).pureFlatFold {
          case Step.Done(a, e) => Done(a, e)
          case Step.Cont(k) => Error("divergent inner iteratee on joinI after EOF", Input.EOF)
          case Step.Error(msg, e) => Error(msg, Input.EOF)
        }
        case Step.Error(msg, e) => Error(msg, Input.Empty)
      }
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

    def fold[B](folder: Step[E,A] => Promise[B]): Promise[B] = folder(Step.Done(a,e))

  }

}

object Cont {
  /**
   * Create an [[play.api.libs.iteratee.Iteratee]] in the “cont” state.
   * @param k Continuation which will compute the next Iteratee state according to an input
   */
  def apply[E, A](k: Input[E] => Iteratee[E, A]): Iteratee[E, A] = new Iteratee[E, A] {

    def fold[B](folder: Step[E,A] => Promise[B]): Promise[B] = folder(Step.Cont(k))

  }
}
object Error {
  /**
   * Create an [[play.api.libs.iteratee.Iteratee]] in the “error” state.
   * @param msg Error message
   * @param e The input that caused the error
   */
  def apply[E](msg: String, e: Input[E]): Iteratee[E, Nothing] = new Iteratee[E, Nothing] {

    def fold[B](folder: Step[E,Nothing] => Promise[B]): Promise[B] = folder(Step.Error(msg,e))

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

      Iteratee.flatten(inner.fold1((a, e) => Promise.pure(Done(Done(a, e), Input.Empty: Input[Array[Byte]])),
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

          Iteratee.flatten(inner.fold1((a, e) => Promise.pure(Done(Done(a, e), inputOrEmpty(rest))),
            k => {
              val (result, suffix) = scan(Nil, all, 0)
              val fed = result.filter(!_.content.isEmpty).foldLeft(Promise.pure(Array[Byte](), Cont(k))) { (p, m) =>
                p.flatMap(i => i._2.fold1((a, e) => Promise.pure((i._1 ++ m.content, Done(a, e))),
                  k => Promise.pure((i._1, k(Input.El(m)))),
                  (err, e) => throw new Exception()))
              }
              fed.flatMap {
                case (ss, i) => i.fold1((a, e) => Promise.pure(Done(Done(a, e), inputOrEmpty(ss ++ suffix))),
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
