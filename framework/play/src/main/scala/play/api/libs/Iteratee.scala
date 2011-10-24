package play.api.libs.iteratee

import play.api.libs.concurrent._

object Iteratee {

  def flatten[E, A](i: Promise[Iteratee[E, A]]): Iteratee[E, A] = new Iteratee[E, A] {

    def fold[B](done: (A, Input[E]) => Promise[B],
      cont: (Input[E] => Iteratee[E, A]) => Promise[B],
      error: (String, Input[E]) => Promise[B]): Promise[B] = i.flatMap(_.fold(done, cont, error))
  }

  def fold[E, A](state: A)(f: (A, E) => A): Iteratee[E, A] =
    {
      def step(s: A)(i: Input[E]): Iteratee[E, A] = i match {

        case EOF => Done(s, EOF)
        case Empty => Cont[E, A](i => step(s)(i))
        case El(e) => { val s1 = f(s, e); Cont[E, A](i => step(s1)(i)) }
      }
      (Cont[E, A](i => step(state)(i)))
    }

  def mapChunk_[E](f: E => Unit): Iteratee[E, Unit] = fold[E, Unit](())((_, e) => f(e))

}

trait Input[+E] {
  def map[U](f: (E => U)): Input[U] = this match {
    case El(e) => El(f(e))
    case Empty => Empty
    case EOF => EOF
  }
}

case class El[E](e: E) extends Input[E]
case object Empty extends Input[Nothing]
case object EOF extends Input[Nothing]

trait Iteratee[E, +A] {
  self =>
  def run[AA >: A]: Promise[AA] = fold((a, _) => Promise.pure(a),
    k => k(EOF).fold((a1, _) => Promise.pure(a1),
      _ => error("diverging iteratee after EOF"),
      (msg, e) => error(msg)),
    (msg, e) => error(msg))

  def fold[B](done: (A, Input[E]) => Promise[B],
    cont: (Input[E] => Iteratee[E, A]) => Promise[B],
    error: (String, Input[E]) => Promise[B]): Promise[B]

  def flatFold[B, C](done: (A, Input[E]) => Promise[Iteratee[B, C]],
    cont: (Input[E] => Iteratee[E, A]) => Promise[Iteratee[B, C]],
    error: (String, Input[E]) => Promise[Iteratee[B, C]]): Iteratee[B, C] = Iteratee.flatten(fold(done, cont, error))

  def mapDone[B](f: A => B): Iteratee[E, B] =
    Iteratee.flatten(this.fold((a, e) => Promise.pure(Done(f(a), e)),
      k => Promise.pure(Cont((in: Input[E]) => k(in).mapDone(f))),
      (err, e) => Promise.pure[Iteratee[E, B]](Error(err, e))))

  def flatMap[B](f: A => Iteratee[E, B]): Iteratee[E, B] = new Iteratee[E, B] {

    def fold[C](done: (B, Input[E]) => Promise[C],
      cont: (Input[E] => Iteratee[E, B]) => Promise[C],
      error: (String, Input[E]) => Promise[C]) =

      self.fold({
        case (a, Empty) => f(a).fold(done, cont, error)
        case (a, e) => f(a).fold((a, _) => done(a, e),
          k => cont(k),
          error)
      },
        ((k) => cont(e => (k(e).flatMap(f)))),
        error)

  }

}

object Done {
  def apply[E, A](a: A, e: Input[E]): Iteratee[E, A] = new Iteratee[E, A] {
    def fold[B](done: (A, Input[E]) => Promise[B],
      cont: (Input[E] => Iteratee[E, A]) => Promise[B],
      error: (String, Input[E]) => Promise[B]): Promise[B] = done(a, e)

  }

}

object Cont {
  def apply[E, A](k: Input[E] => Iteratee[E, A]): Iteratee[E, A] = new Iteratee[E, A] {
    def fold[B](done: (A, Input[E]) => Promise[B],
      cont: (Input[E] => Iteratee[E, A]) => Promise[B],
      error: (String, Input[E]) => Promise[B]): Promise[B] = cont(k)

  }
}
object Error {
  def apply[E](msg: String, e: Input[E]): Iteratee[E, Nothing] = new Iteratee[E, Nothing] {
    def fold[B](done: (Nothing, Input[E]) => Promise[B],
      cont: (Input[E] => Iteratee[E, Nothing]) => Promise[B],
      error: (String, Input[E]) => Promise[B]): Promise[B] = error(msg, e)

  }
}

trait Enumerator[+E] {

  parent =>
  def apply[A, EE >: E](i: Iteratee[EE, A]): Promise[Iteratee[EE, A]]
  def <<:[A, EE >: E](i: Iteratee[EE, A]): Promise[Iteratee[EE, A]] = apply(i)

  def andThen[F >: E](e: Enumerator[F]): Enumerator[F] = new Enumerator[F] {
    def apply[A, FF >: F](i: Iteratee[FF, A]): Promise[Iteratee[FF, A]] = parent.apply(i).flatMap(e.apply) //bad implementation, should remove EOF in the end of first
  }

  def map[U](f: Input[E] => Input[U]) = new Enumerator[U] {
    def apply[A, UU >: U](it: Iteratee[UU, A]) = {

      case object OuterEOF extends Input[Nothing]
      type R = Iteratee[E, Iteratee[UU, A]]

      def step(ri: Iteratee[UU, A])(in: Input[E]): R =

        in match {
          case OuterEOF => Done(ri, EOF)
          case any =>
            Iteratee.flatten(
              ri.fold((a, _) => Promise.pure(Done(ri, any)),
                k => {
                  val next = k(f(any))
                  next.fold((a, _) => Promise.pure(Done(next, in)),
                    _ => Promise.pure(Cont(step(next))),
                    (msg, _) => Promise.pure[R](Error(msg, in)))
                },
                (msg, _) => Promise.pure[R](Error(msg, any))))
        }

      parent.apply(Cont(step(it)))
        .flatMap(_.fold((a, _) => Promise.pure(a),
          k => k(OuterEOF).fold(
            (a1, _) => Promise.pure(a1),
            _ => error("diverging iteratee after EOF"),
            (msg, e) => error(msg)),
          (msg, e) => error(msg)))
    }
  }

}

trait Enumeratee[In, Out] {
  def apply[A](inner: Iteratee[In, A]): Iteratee[Out, Iteratee[In, A]]
}

object Enumerator {

  def enumInput[E](e: Input[E]) = new Enumerator[E] {
    def apply[A, EE >: E](i: Iteratee[EE, A]): Promise[Iteratee[EE, A]] =
      i.fold((a, e) => Promise.pure(i),
        k => Promise.pure(k(e)),
        (_, _) => Promise.pure(i))

  }

  def empty[A] = enumInput[A](EOF)

  def apply[E](in: E*): Enumerator[E] = new Enumerator[E] {

    def apply[A, EE >: E](i: Iteratee[EE, A]): Promise[Iteratee[EE, A]] = enumerate(in, i)

  }
  def enumerate[E, A]: (Seq[E], Iteratee[E, A]) => Promise[Iteratee[E, A]] = { (l, i) =>
    l.foldLeft(Promise.pure(i))((i, e) =>
      i.flatMap(_.fold((_, _) => i,
        k => Promise.pure(k(El(e))),
        (_, _) => i)))
  }
}

object Parsing {

  trait MatchInfo[A] { def content: A }
  case class Matched[A](val content: A) extends MatchInfo[A]
  case class Unmatched[A](val content: A) extends MatchInfo[A]

  def search(needle: Array[Byte]): Enumeratee[MatchInfo[Array[Byte]], Array[Byte]] = new Enumeratee[MatchInfo[Array[Byte]], Array[Byte]] {
    val needleSize = needle.size
    val fullJump = needleSize
    val jumpBadCharecter: (Byte => Int) = {
      val map = Map(needle.dropRight(1).reverse.zipWithIndex: _*) //remove the last
      byte => map.get(byte).map(_ + 1).getOrElse(fullJump)
    }

    def apply[A](inner: Iteratee[MatchInfo[Array[Byte]], A]): Iteratee[Array[Byte], Iteratee[MatchInfo[Array[Byte]], A]] = {

      Iteratee.flatten(inner.fold((a, e) => Promise.pure(Done(Done(a, e), Empty: Input[Array[Byte]])),
        k => Promise.pure(Cont(step(Array[Byte](), Cont(k)))),
        (err, r) => throw new Exception()))

    }
    def scan(previousMatches: List[MatchInfo[Array[Byte]]], piece: Array[Byte], startScan: Int): (List[MatchInfo[Array[Byte]]], Array[Byte]) = {
      println(startScan)
      val fullMatch = Range(needleSize - 1, -1, -1).forall(scan => needle(scan) == piece(scan + startScan))
      if (fullMatch) {
        val (prefix, then) = piece.splitAt(startScan)
        println("first prefix is " + prefix)
        val (matched, left) = then.splitAt(needleSize)
        val newResults = previousMatches ++ List(Unmatched(prefix), Matched(matched)) filter (!_.content.isEmpty)
        if (left.length < needleSize)
          (newResults, left)
        else scan(newResults, left, 0)
      } else {
        val jump = jumpBadCharecter(piece(startScan + needleSize - 1))
        val isFullJump = jump == fullJump
        val newScan = startScan + jump;
        println("new scan is " + newScan)
        if (newScan + needleSize - 1 > piece.length - 1) {
          if (isFullJump) (previousMatches ++ List(Unmatched(piece)), Array[Byte]())
          else {
            val (prefix, suffix) = (piece.splitAt(startScan))
            println("prefix here is: " + prefix)
            (previousMatches ++ List(Unmatched(prefix)), suffix)
          }
        } else scan(previousMatches, piece, newScan)
      }
    }

    def step[A](rest: Array[Byte], inner: Iteratee[MatchInfo[Array[Byte]], A])(in: Input[Array[Byte]]): Iteratee[Array[Byte], Iteratee[MatchInfo[Array[Byte]], A]] = {

      in match {
        case Empty => Cont(step(rest, inner)) //here should rather pass Empty along

        case EOF => Done(inner, El(rest))

        case El(chunk) =>

          val all = rest ++ chunk
          def inputOrEmpty(a: Array[Byte]) = if (a.isEmpty) Empty else El(a)

          Iteratee.flatten(inner.fold((a, e) => Promise.pure(Done(Done(a, e), inputOrEmpty(rest))),
            k => {
              val (result, suffix) = scan(Nil, all, 0)
              println("pipi= " + result.map(m => new String(m.content)) + new String(suffix))
              val fed = result.filter(!_.content.isEmpty).foldLeft(Promise.pure(Array[Byte](), Cont(k))) { (p, m) =>
                p.flatMap(i => i._2.fold((a, e) => Promise.pure((i._1 ++ m.content, Done(a, e))),
                  k => Promise.pure((i._1, k(El(m)))),
                  (err, e) => throw new Exception()))
              }
              fed.flatMap {
                case (ss, i) => i.fold((a, e) => Promise.pure(Done(Done(a, e), inputOrEmpty(ss ++ suffix))),
                  k => Promise.pure(Cont[Array[Byte], Iteratee[MatchInfo[Array[Byte]], A]]((in: Input[Array[Byte]]) => in match {
                    case EOF => Done(k(El(Unmatched(suffix))), EOF) //suffix maybe empty
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
