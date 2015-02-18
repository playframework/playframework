/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.iteratee

import play.api.libs.iteratee.Execution.Implicits.{ defaultExecutionContext => dec }
import play.api.libs.iteratee.internal.{ eagerFuture, executeFuture }
import scala.concurrent.{ ExecutionContext, Future, Promise, blocking }
import scala.util.{ Try, Success, Failure }
import scala.language.reflectiveCalls

/**
 * A producer which pushes input to an [[play.api.libs.iteratee.Iteratee]].
 *
 * @define paramEcSingle @param ec The context to execute the supplied function with. The context is prepared on the calling thread before being used.
 * @define paramEcMultiple @param ec The context to execute the supplied functions with. The context is prepared on the calling thread before being used.
 */
trait Enumerator[E] {
  parent =>

  /**
   * Attaches this Enumerator to an [[play.api.libs.iteratee.Iteratee]], driving the
   * Iteratee to (asynchronously) consume the input. The Iteratee may enter its
   * [[play.api.libs.iteratee.Done]] or [[play.api.libs.iteratee.Error]]
   * state, or it may be left in a [[play.api.libs.iteratee.Cont]] state (allowing it
   * to consume more input after that sent by the enumerator).
   *
   * If the Iteratee reaches a [[play.api.libs.iteratee.Done]] state, it will
   * contain a computed result and the remaining (unconsumed) input.
   */
  def apply[A](i: Iteratee[E, A]): Future[Iteratee[E, A]]

  /**
   * Alias for `apply`, produces input driving the given [[play.api.libs.iteratee.Iteratee]]
   * to consume it.
   */
  def |>>[A](i: Iteratee[E, A]): Future[Iteratee[E, A]] = apply(i)

  /**
   * Attaches this Enumerator to an [[play.api.libs.iteratee.Iteratee]], driving the
   * Iteratee to (asynchronously) consume the enumerator's input. If the Iteratee does not
   * reach a [[play.api.libs.iteratee.Done]] or [[play.api.libs.iteratee.Error]]
   * state when the Enumerator finishes, this method forces one of those states by
   * feeding `Input.EOF` to the Iteratee.
   *
   * If the iteratee is left in a [[play.api.libs.iteratee.Done]]
   * state then the promise is completed with the iteratee's result.
   * If the iteratee is left in an [[play.api.libs.iteratee.Error]] state, then the
   * promise is completed with a [[java.lang.RuntimeException]] containing the
   * iteratee's error message.
   *
   * Unlike `apply` or `|>>`, this method does not allow you to access the
   * unconsumed input.
   */
  def |>>>[A](i: Iteratee[E, A]): Future[A] = apply(i).flatMap(_.run)(dec)

  /**
   * Alias for `|>>>`; drives the iteratee to consume the enumerator's
   * input, adding an Input.EOF at the end of the input. Returns either a result
   * or an exception.
   */
  def run[A](i: Iteratee[E, A]): Future[A] = |>>>(i)

  /**
   * A variation on `apply` or `|>>` which returns the state of the iteratee rather
   * than the iteratee itself. This can make your code a little shorter.
   */
  def |>>|[A](i: Iteratee[E, A]): Future[Step[E, A]] = apply(i).flatMap(_.unflatten)(dec)

  /**
   * Sequentially combine this Enumerator with another Enumerator. The resulting enumerator
   * will produce both input streams, this one first, then the other.
   * Note: the current implementation will break if the first enumerator
   * produces an Input.EOF.
   */
  def andThen(e: Enumerator[E]): Enumerator[E] = new Enumerator[E] {
    def apply[A](i: Iteratee[E, A]): Future[Iteratee[E, A]] = parent.apply(i).flatMap(e.apply)(dec) //bad implementation, should remove Input.EOF in the end of first
  }

  def interleave[B >: E](other: Enumerator[B]): Enumerator[B] = Enumerator.interleave(this, other)

  /**
   * Alias for interleave
   */
  def >-[B >: E](other: Enumerator[B]): Enumerator[B] = interleave(other)

  /**
   * Compose this Enumerator with an Enumeratee. Alias for through
   */
  def &>[To](enumeratee: Enumeratee[E, To]): Enumerator[To] = new Enumerator[To] {

    def apply[A](i: Iteratee[To, A]): Future[Iteratee[To, A]] = {
      val transformed = enumeratee.applyOn(i)
      val xx = parent |>> transformed
      xx.flatMap(_.run)(dec)

    }
  }

  /**
   * Creates an Enumerator which calls the given callback when a passed in iteratee has either entered the done state,
   * or if an error is returned.
   *
   * This is equivalent to a finally call, and can be used to clean up any resources used by this enumerator.  Note that
   * if the callback throws an exception, then the future returned by the enumerator will be completed with that
   * exception.
   *
   * @param callback The callback to call
   * $paramEcSingle
   */
  def onDoneEnumerating(callback: => Unit)(implicit ec: ExecutionContext): Enumerator[E] = new Enumerator[E] {
    private val pec = ec.prepare()

    def apply[A](it: Iteratee[E, A]): Future[Iteratee[E, A]] = parent.apply(it).andThen {
      case someTry =>
        callback
        someTry.get
    }(pec)

  }

  /**
   * Compose this Enumerator with an Enumeratee
   */
  def through[To](enumeratee: Enumeratee[E, To]): Enumerator[To] = &>(enumeratee)

  /**
   * Alias for `andThen`
   */
  def >>>(e: Enumerator[E]): Enumerator[E] = andThen(e)

  /**
   * maps the given function f onto parent Enumerator
   * @param f function to map
   * $paramEcSingle
   * @return enumerator
   */
  def map[U](f: E => U)(implicit ec: ExecutionContext): Enumerator[U] = parent &> Enumeratee.map[E](f)(ec)

  /**
   * Creates an Enumerator, based on this one, with each input transformed by the given function.
   *
   * @param f Used to transform the input.
   * $paramEcSingle
   */
  def mapInput[U](f: Input[E] => Input[U])(implicit ec: ExecutionContext): Enumerator[U] = parent &> Enumeratee.mapInput[E](f)(ec)

  /**
   * flatmaps the given function f onto parent Enumerator
   * @param f function to map
   * $paramEcSingle
   * @return enumerator
   */
  def flatMap[U](f: E => Enumerator[U])(implicit ec: ExecutionContext): Enumerator[U] = {
    val pec = ec.prepare()
    import Execution.Implicits.{ defaultExecutionContext => ec } // Shadow ec to make this the only implicit EC in scope
    new Enumerator[U] {
      def apply[A](iteratee: Iteratee[U, A]): Future[Iteratee[U, A]] = {
        val folder = Iteratee.fold2[E, Iteratee[U, A]](iteratee) { (it, e) =>
          for {
            en <- Future(f(e))(pec)
            newIt <- en(it)
            done <- Iteratee.isDoneOrError(newIt)
          } yield ((newIt, done))
        }(dec)
        parent(folder).flatMap(_.run)
      }
    }
  }

}
/**
 * Enumerator is the source that pushes input into a given iteratee.
 * It enumerates some input into the iteratee and eventually returns the new state of that iteratee.
 *
 * @define paramEcSingle @param ec The context to execute the supplied function with. The context is prepared on the calling thread before being used.
 * @define paramEcMultiple @param ec The context to execute the supplied functions with. The context is prepared on the calling thread before being used.
 */
object Enumerator {

  def flatten[E](eventuallyEnum: Future[Enumerator[E]]): Enumerator[E] = new Enumerator[E] {

    def apply[A](it: Iteratee[E, A]): Future[Iteratee[E, A]] = eventuallyEnum.flatMap(_.apply(it))(dec)

  }

  /**
   * Creates an enumerator which produces the one supplied
   * input and nothing else. This enumerator will NOT
   * automatically produce Input.EOF after the given input.
   */
  def enumInput[E](e: Input[E]) = new Enumerator[E] {
    def apply[A](i: Iteratee[E, A]): Future[Iteratee[E, A]] =
      i.fold {
        case Step.Cont(k) => eagerFuture(k(e))
        case _ => Future.successful(i)
      }(dec)
  }

  /**
   * Interleave multiple enumerators together.
   *
   * Interleaving is done based on whichever enumerator next has input ready, if multiple have input ready, the order
   * is undefined.
   */
  def interleave[E](e1: Enumerator[E], es: Enumerator[E]*): Enumerator[E] = interleave(e1 +: es)

  /**
   * Interleave multiple enumerators together.
   *
   * Interleaving is done based on whichever enumerator next has input ready, if multiple have input ready, the order
   * is undefined.
   */
  def interleave[E](es: Seq[Enumerator[E]]): Enumerator[E] = new Enumerator[E] {

    import scala.concurrent.stm._

    def apply[A](it: Iteratee[E, A]): Future[Iteratee[E, A]] = {

      val iter: Ref[Iteratee[E, A]] = Ref(it)
      val attending: Ref[Option[Seq[Boolean]]] = Ref(Some(es.map(_ => true)))
      val result = Promise[Iteratee[E, A]]()

      def redeemResultIfNotYet(r: Iteratee[E, A]) {
        if (attending.single.transformIfDefined { case Some(_) => None })
          result.success(r)
      }

      def iteratee[EE <: E](f: Seq[Boolean] => Seq[Boolean]): Iteratee[EE, Unit] = {
        def step(in: Input[EE]): Iteratee[EE, Unit] = {

          val p = Promise[Iteratee[E, A]]()
          val i = iter.single.swap(Iteratee.flatten(p.future))
          in match {
            case Input.El(_) | Input.Empty =>

              val nextI = i.fold {

                case Step.Cont(k) =>
                  val n = k(in)
                  n.fold {
                    case Step.Cont(kk) =>
                      p.success(Cont(kk))
                      Future.successful(Cont(step))
                    case _ =>
                      p.success(n)
                      Future.successful(Done((), Input.Empty: Input[EE]))
                  }(dec)
                case _ =>
                  p.success(i)
                  Future.successful(Done((), Input.Empty: Input[EE]))

              }(dec)
              Iteratee.flatten(nextI)
            case Input.EOF => {
              if (attending.single.transformAndGet { _.map(f) }.forall(_ == false)) {
                p.complete(Try(Iteratee.flatten(i.feed(Input.EOF))))
              } else {
                p.success(i)
              }
              Done((), Input.Empty)
            }
          }
        }
        Cont(step)
      }
      val ps = es.zipWithIndex.map { case (e, index) => e |>> iteratee[E](_.patch(index, Seq(true), 1)) }
        .map(_.flatMap(_.pureFold(any => ())(dec)))

      Future.sequence(ps).onComplete {
        case Success(_) =>
          redeemResultIfNotYet(iter.single())
        case Failure(e) => result.failure(e)

      }

      result.future
    }

  }

  /**
   * Interleave two enumerators together.
   *
   * Interleaving is done based on whichever enumerator next has input ready, if both have input ready, the order is
   * undefined.
   */
  def interleave[E1, E2 >: E1](e1: Enumerator[E1], e2: Enumerator[E2]): Enumerator[E2] = new Enumerator[E2] {

    import scala.concurrent.stm._

    def apply[A](it: Iteratee[E2, A]): Future[Iteratee[E2, A]] = {

      val iter: Ref[Iteratee[E2, A]] = Ref(it)
      val attending: Ref[Option[(Boolean, Boolean)]] = Ref(Some(true -> true))
      val result = Promise[Iteratee[E2, A]]()

      def redeemResultIfNotYet(r: Iteratee[E2, A]) {
        if (attending.single.transformIfDefined { case Some(_) => None })
          result.success(r)
      }

      def iteratee[EE <: E2](f: ((Boolean, Boolean)) => (Boolean, Boolean)): Iteratee[EE, Unit] = {
        def step(in: Input[EE]): Iteratee[EE, Unit] = {

          val p = Promise[Iteratee[E2, A]]()
          val i = iter.single.swap(Iteratee.flatten(p.future))
          in match {
            case Input.El(_) | Input.Empty =>

              val nextI = i.fold {

                case Step.Cont(k) =>
                  val n = k(in)
                  n.fold {
                    case Step.Cont(kk) =>
                      p.success(Cont(kk))
                      Future.successful(Cont(step))
                    case _ =>
                      p.success(n)
                      Future.successful(Done((), Input.Empty: Input[EE]))
                  }(dec)
                case _ =>
                  p.success(i)
                  Future.successful(Done((), Input.Empty: Input[EE]))

              }(dec)
              Iteratee.flatten(nextI)
            case Input.EOF => {
              if (attending.single.transformAndGet { _.map(f) } == Some((false, false))) {
                p.complete(Try(Iteratee.flatten(i.feed(Input.EOF))))
              } else {
                p.success(i)
              }
              Done((), Input.Empty)
            }
          }
        }
        Cont(step)
      }

      val itE1 = iteratee[E1] { case (l, r) => (false, r) }
      val itE2 = iteratee[E2] { case (l, r) => (l, false) }
      val r1 = e1 |>>| itE1
      val r2 = e2 |>>| itE2
      r1.flatMap(_ => r2).onComplete {
        case Success(_) =>
          redeemResultIfNotYet(iter.single())
        case Failure(e) => result.failure(e)

      }
      result.future
    }

  }

  /**
   * Like [[play.api.libs.iteratee.Enumerator.unfold]], but allows the unfolding to be done asynchronously.
   *
   * @param s The value to unfold
   * @param f The unfolding function. This will take the value, and return a future for some tuple of the next value
   *          to unfold and the next input, or none if the value is completely unfolded.
   * $paramEcSingle
   */
  def unfoldM[S, E](s: S)(f: S => Future[Option[(S, E)]])(implicit ec: ExecutionContext): Enumerator[E] = checkContinue1(s)(new TreatCont1[E, S] {
    private val pec = ec.prepare()

    def apply[A](loop: (Iteratee[E, A], S) => Future[Iteratee[E, A]], s: S, k: Input[E] => Iteratee[E, A]): Future[Iteratee[E, A]] = {
      executeFuture(f(s))(pec).flatMap {
        case Some((newS, e)) => loop(k(Input.El(e)), newS)
        case None => Future.successful(Cont(k))
      }(dec)
    }
  })

  /**
   * Unfold a value of type S into input for an enumerator.
   *
   * For example, the following would enumerate the elements of a list, implementing the same behavior as
   * Enumerator.enumerate:
   *
   * {{{
   *   Enumerator.sequence[List[Int], Int]{ list =>
   *     list.headOption.map(input => list.tail -> input)
   *   }
   * }}}
   *
   * @param s The value to unfold
   * @param f The unfolding function. This will take the value, and return some tuple of the next value to unfold and
   *          the next input, or none if the value is completely unfolded.
   * $paramEcSingle
   */
  def unfold[S, E](s: S)(f: S => Option[(S, E)])(implicit ec: ExecutionContext): Enumerator[E] = checkContinue1(s)(new TreatCont1[E, S] {
    private val pec = ec.prepare()

    def apply[A](loop: (Iteratee[E, A], S) => Future[Iteratee[E, A]], s: S, k: Input[E] => Iteratee[E, A]): Future[Iteratee[E, A]] = Future(f(s))(pec).flatMap {
      case Some((s, e)) => loop(k(Input.El(e)), s)
      case None => Future.successful(Cont(k))
    }(dec)
  })

  /**
   * Repeat the given input function indefinitely.
   *
   * @param e The input function.
   * $paramEcSingle
   */
  def repeat[E](e: => E)(implicit ec: ExecutionContext): Enumerator[E] = checkContinue0(new TreatCont0[E] {
    private val pec = ec.prepare()

    def apply[A](loop: Iteratee[E, A] => Future[Iteratee[E, A]], k: Input[E] => Iteratee[E, A]) = Future(e)(pec).flatMap(ee => loop(k(Input.El(ee))))(dec)

  })

  /**
   * Like [[play.api.libs.iteratee.Enumerator.repeat]], but allows repeated values to be asynchronously fetched.
   *
   * @param e The input function
   * $paramEcSingle
   */
  def repeatM[E](e: => Future[E])(implicit ec: ExecutionContext): Enumerator[E] = checkContinue0(new TreatCont0[E] {
    private val pec = ec.prepare()

    def apply[A](loop: Iteratee[E, A] => Future[Iteratee[E, A]], k: Input[E] => Iteratee[E, A]) = executeFuture(e)(pec).flatMap(ee => loop(k(Input.El(ee))))(dec)

  })

  /**
   * Like [[play.api.libs.iteratee.Enumerator.repeatM]], but the callback returns an Option, which allows the stream
   * to be eventually terminated by returning None.
   *
   * @param e The input function.  Returns a future eventually redeemed with Some value if there is input to pass, or a
   *          future eventually redeemed with None if the end of the stream has been reached.
   */
  def generateM[E](e: => Future[Option[E]])(implicit ec: ExecutionContext): Enumerator[E] = checkContinue0(new TreatCont0[E] {
    private val pec = ec.prepare()

    def apply[A](loop: Iteratee[E, A] => Future[Iteratee[E, A]], k: Input[E] => Iteratee[E, A]) = executeFuture(e)(pec).flatMap {
      case Some(e) => loop(k(Input.El(e)))
      case None => Future.successful(Cont(k))
    }(dec)
  })

  trait TreatCont0[E] {

    def apply[A](loop: Iteratee[E, A] => Future[Iteratee[E, A]], k: Input[E] => Iteratee[E, A]): Future[Iteratee[E, A]]

  }

  def checkContinue0[E](inner: TreatCont0[E]) = new Enumerator[E] {

    def apply[A](it: Iteratee[E, A]): Future[Iteratee[E, A]] = {

      def step(it: Iteratee[E, A]): Future[Iteratee[E, A]] = it.fold {
        case Step.Done(a, e) => Future.successful(Done(a, e))
        case Step.Cont(k) => inner[A](step, k)
        case Step.Error(msg, e) => Future.successful(Error(msg, e))
      }(dec)

      step(it)
    }
  }

  trait TreatCont1[E, S] {

    def apply[A](loop: (Iteratee[E, A], S) => Future[Iteratee[E, A]], s: S, k: Input[E] => Iteratee[E, A]): Future[Iteratee[E, A]]

  }

  def checkContinue1[E, S](s: S)(inner: TreatCont1[E, S]) = new Enumerator[E] {

    def apply[A](it: Iteratee[E, A]): Future[Iteratee[E, A]] = {

      def step(it: Iteratee[E, A], state: S): Future[Iteratee[E, A]] = it.fold {
        case Step.Done(a, e) => Future.successful(Done(a, e))
        case Step.Cont(k) => inner[A](step, state, k)
        case Step.Error(msg, e) => Future.successful(Error(msg, e))
      }(dec)
      step(it, s)
    }

  }

  /**
   * Like [[play.api.libs.iteratee.Enumerator.generateM]], but `retriever` accepts a boolean
   * value that is `true` on its first call only.
   *
   * @param retriever The input function.  Returns a future eventually redeemed with Some value if there is input to pass, or a
   *          future eventually redeemed with None if the end of the stream has been reached.
   * @param onComplete Called when the end of the stream is reached.
   * @param onError Called when an error occurs in the iteratee
   * $paramEcMultiple
   */
  def fromCallback1[E](retriever: Boolean => Future[Option[E]],
    onComplete: () => Unit = () => (),
    onError: (String, Input[E]) => Unit = (_: String, _: Input[E]) => ())(implicit ec: ExecutionContext) = new Enumerator[E] {
    private val pec = ec.prepare()
    def apply[A](it: Iteratee[E, A]): Future[Iteratee[E, A]] = {

      val iterateeP = Promise[Iteratee[E, A]]()

      def step(it: Iteratee[E, A], initial: Boolean = false) {

        val next = it.fold {
          case Step.Cont(k) => {
            executeFuture(retriever(initial))(pec).map {
              case None => {
                val remainingIteratee = k(Input.EOF)
                iterateeP.success(remainingIteratee)
                None
              }
              case Some(read) => {
                val nextIteratee = k(Input.El(read))
                Some(nextIteratee)
              }
            }(dec)
          }
          case Step.Error(msg, in) =>
            onError(msg, in)
            iterateeP.success(it)
            Future.successful(None)
          case _ =>
            iterateeP.success(it)
            Future.successful(None)
        }(dec)

        next.onFailure {
          case reason: Exception =>
            onError(reason.getMessage(), Input.Empty)
        }(dec)

        next.onComplete {
          case Success(Some(i)) => step(i)

          case Success(None) => Future(onComplete())(pec)
          case Failure(e) =>
            iterateeP.failure(e)
        }(dec)
      }
      step(it, true)
      iterateeP.future
    }
  }

  /**
   * Create an enumerator from the given input stream.
   *
   * This enumerator will block on reading the input stream, in the supplied ExecutionContext.  Care must therefore
   * be taken to ensure that this isn't a slow stream.  If using this with slow input streams, make sure the
   * ExecutionContext is appropriately configured to handle the blocking.
   *
   * @param input The input stream
   * @param chunkSize The size of chunks to read from the stream.
   * @param ec The ExecutionContext to execute blocking code.
   */
  def fromStream(input: java.io.InputStream, chunkSize: Int = 1024 * 8)(implicit ec: ExecutionContext): Enumerator[Array[Byte]] = {
    implicit val pec = ec.prepare()
    generateM({
      val buffer = new Array[Byte](chunkSize)
      val bytesRead = blocking { input.read(buffer) }
      val chunk = bytesRead match {
        case -1 => None
        case `chunkSize` => Some(buffer)
        case read =>
          val input = new Array[Byte](read)
          System.arraycopy(buffer, 0, input, 0, read)
          Some(input)
      }
      Future.successful(chunk)
    })(pec).onDoneEnumerating(input.close)(pec)
  }

  /**
   * Create an enumerator from the given input stream.
   *
   * Note that this enumerator will block when it reads from the file.
   *
   * @param file The file to create the enumerator from.
   * @param chunkSize The size of chunks to read from the file.
   */
  def fromFile(file: java.io.File, chunkSize: Int = 1024 * 8)(implicit ec: ExecutionContext): Enumerator[Array[Byte]] = {
    fromStream(new java.io.FileInputStream(file), chunkSize)(ec)
  }

  /**
   * Create an Enumerator of bytes with an OutputStream.
   *
   * Note that calls to write will not block, so if the iteratee that is being fed to is slow to consume the input, the
   * OutputStream will not push back.  This means it should not be used with large streams since there is a risk of
   * running out of memory.
   *
   * @param a A callback that provides the output stream when this enumerator is written to an iteratee.
   * $paramEcSingle
   */
  def outputStream(a: java.io.OutputStream => Unit)(implicit ec: ExecutionContext): Enumerator[Array[Byte]] = {
    Concurrent.unicast[Array[Byte]] { channel =>
      val outputStream = new java.io.OutputStream() {
        override def close() {
          channel.end()
        }
        override def flush() {}
        override def write(value: Int) {
          channel.push(Array(value.toByte))
        }
        override def write(buffer: Array[Byte]) {
          write(buffer, 0, buffer.length)
        }
        override def write(buffer: Array[Byte], start: Int, count: Int) {
          channel.push(buffer.slice(start, start + count))
        }
      }
      a(outputStream)
    }(ec)
  }

  /**
   * An enumerator that produces EOF and nothing else.
   */
  def eof[A] = enumInput[A](Input.EOF)

  /**
   * Create an Enumerator from a set of values
   *
   * Example:
   * {{{
   *   val enumerator: Enumerator[String] = Enumerator("kiki", "foo", "bar")
   * }}}
   */
  def apply[E](in: E*): Enumerator[E] = in.length match {
    case 0 => Enumerator.empty
    case 1 => new Enumerator[E] {
      def apply[A](i: Iteratee[E, A]): Future[Iteratee[E, A]] = i.pureFoldNoEC {
        case Step.Cont(k) => k(Input.El(in.head))
        case _ => i
      }
    }
    case _ => new Enumerator[E] {
      def apply[A](i: Iteratee[E, A]): Future[Iteratee[E, A]] = enumerateSeq(in, i)
    }
  }

  /**
   * Create an Enumerator from any TraversableOnce like collection of elements.
   *
   * Example of an iterator of lines of a file :
   * {{{
   *  val enumerator: Enumerator[String] = Enumerator( scala.io.Source.fromFile("myfile.txt").getLines )
   * }}}
   */
  def enumerate[E](traversable: TraversableOnce[E])(implicit ctx: scala.concurrent.ExecutionContext): Enumerator[E] = {
    val it = traversable.toIterator
    Enumerator.unfoldM[scala.collection.Iterator[E], E](it: scala.collection.Iterator[E])({ currentIt =>
      if (currentIt.hasNext)
        Future[Option[(scala.collection.Iterator[E], E)]]({
          val next = currentIt.next
          Some((currentIt -> next))
        })(ctx)
      else
        Future.successful[Option[(scala.collection.Iterator[E], E)]]({
          None
        })
    })(dec)
  }

  /**
   * An empty enumerator
   */
  def empty[E]: Enumerator[E] = new Enumerator[E] {
    def apply[A](i: Iteratee[E, A]) = Future.successful(i)
  }

  private def enumerateSeq[E, A]: (Seq[E], Iteratee[E, A]) => Future[Iteratee[E, A]] = { (l, i) =>
    l.foldLeft(Future.successful(i))((i, e) =>
      i.flatMap(it => it.pureFold {
        case Step.Cont(k) => k(Input.El(e))
        case _ => it
      }(dec))(dec))
  }

  private[iteratee] def enumerateSeq1[E](s: Seq[E]): Enumerator[E] = checkContinue1(s)(new TreatCont1[E, Seq[E]] {
    def apply[A](loop: (Iteratee[E, A], Seq[E]) => Future[Iteratee[E, A]], s: Seq[E], k: Input[E] => Iteratee[E, A]): Future[Iteratee[E, A]] =
      if (!s.isEmpty)
        loop(k(Input.El(s.head)), s.tail)
      else Future.successful(Cont(k))
  })

  private[iteratee] def enumerateSeq2[E](s: Seq[Input[E]]): Enumerator[E] = checkContinue1(s)(new TreatCont1[E, Seq[Input[E]]] {
    def apply[A](loop: (Iteratee[E, A], Seq[Input[E]]) => Future[Iteratee[E, A]], s: Seq[Input[E]], k: Input[E] => Iteratee[E, A]): Future[Iteratee[E, A]] =
      if (!s.isEmpty)
        loop(k(s.head), s.tail)
      else Future.successful(Cont(k))
  })

}
