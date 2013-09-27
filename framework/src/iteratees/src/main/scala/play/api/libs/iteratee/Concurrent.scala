package play.api.libs.iteratee

import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.util.{ Try, Failure, Success }
import Enumerator.Pushee
import java.util.concurrent.{ TimeUnit }
import play.api.libs.iteratee.Execution.Implicits.{ defaultExecutionContext => dec }

/**
 * Utilities for concurrent usage of iteratees, enumerators and enumeratees.
 *
 * @define paramEcSingle @param ec The context to execute the supplied function with. The context is prepared on the calling thread before being used.
 * @define paramEcMultiple @param ec The context to execute the supplied functions with. The context is prepared on the calling thread before being used.
 */
object Concurrent {

  private val timer = new java.util.Timer()

  private def timeoutFuture[A](v: A, delay: Long, unit: TimeUnit): Future[A] = {

    val p = Promise[A]()
    timer.schedule(new java.util.TimerTask {
      def run() {
        p.success(v)
      }
    }, unit.toMillis(delay))
    p.future
  }

  /**
   * A channel for imperative style feeding of input into one or more iteratees.
   */
  trait Channel[E] {

    /**
     * Push an input chunk into this channel
     *
     * @param chunk The chunk to push
     */
    def push(chunk: Input[E])

    /**
     * Push an item into this channel
     *
     * @param item The item to push
     */
    def push(item: E) { push(Input.El(item)) }

    /**
     * Send a failure to this channel.  This results in any promises that the enumerator associated with this channel
     * produced being redeemed with a failure.
     *
     * @param e The failure.
     */
    def end(e: Throwable)

    /**
     * End the input for this channel.  This results in any promises that the enumerator associated with this channel
     * produced being redeemed.
     *
     * Note that an EOF won't be sent, so any iteratees consuming this channel will still be able to consume input
     * (if they are in the cont state).
     */
    def end()

    /**
     * Send an EOF to the channel, and then end the input for the channel.
     */
    def eofAndEnd() {
      push(Input.EOF)
      end()
    }
  }

  /**
   * Create an enumerator and channel for broadcasting input to many iteratees.
   *
   * This is intended for imperative style push input feeding into iteratees.  For example:
   *
   * {{{
   * val (chatEnumerator, chatChannel) = Concurrent.broadcast[String]
   * val chatClient1 = Iteratee.foreach[String](m => println("Client 1: " + m))
   * val chatClient2 = Iteratee.foreach[String](m => println("Client 2: " + m))
   * chatEnumerator |>>> chatClient1
   * chatEnumerator |>>> chatClient2
   *
   * chatChannel.push(Message("Hello world!"))
   * }}}
   */
  def broadcast[E]: (Enumerator[E], Channel[E]) = {

    import scala.concurrent.stm._

    val iteratees: Ref[List[(Iteratee[E, _], Promise[Iteratee[E, _]])]] = Ref(List())

    def step(in: Input[E]): Iteratee[E, Unit] = {
      val interested = iteratees.single.swap(List())

      val ready = interested.map {
        case (it, p) =>
          it.fold {
            case Step.Done(a, e) => Future.successful(Left(Done(a, e)))
            case Step.Cont(k) => {
              val next = k(in)
              next.pureFold {
                case Step.Done(a, e) => Left(Done(a, e))
                case Step.Cont(k) => Right((Cont(k), p))
                case Step.Error(msg, e) => Left(Error(msg, e))
              }(dec)
            }
            case Step.Error(msg, e) => Future.successful(Left(Error(msg, e)))
          }(dec).map {
            case Left(s) =>
              p.success(s)
              None
            case Right(s) =>
              Some(s)
          }(dec).recover {
            case e: Throwable =>
              p.failure(e)
              None
          }(dec)
      }

      Iteratee.flatten(Future.sequence(ready).map[Iteratee[E, Unit]] { commitReady =>

        val downToZero = atomic { implicit txn =>
          iteratees.transform(commitReady.collect { case Some(s) => s } ++ _)
          (interested.length > 0 && iteratees().length <= 0)
        }

        if (in == Input.EOF) Done((), Input.Empty) else Cont(step)

      }(dec))
    }

    val redeemed = Ref(None: Option[Try[Unit]])

    val enumerator = new Enumerator[E] {

      def apply[A](it: Iteratee[E, A]): Future[Iteratee[E, A]] = {
        val result = Promise[Iteratee[E, A]]()

        val finished = atomic { implicit txn =>
          redeemed() match {
            case None =>
              iteratees.transform(_ :+ ((it, (result: Promise[Iteratee[E, A]]).asInstanceOf[Promise[Iteratee[E, _]]])))
              None
            case Some(notWaiting) => Some(notWaiting)
          }
        }
        finished.foreach {
          case Success(_) => result.success(it)
          case Failure(e) => result.failure(e)
        }
        result.future
      }

    }

    val mainIteratee = Ref(Cont(step))

    val toPush = new Channel[E] {

      def push(chunk: Input[E]) {

        val itPromise = Promise[Iteratee[E, Unit]]()

        val current: Iteratee[E, Unit] = mainIteratee.single.swap(Iteratee.flatten(itPromise.future))

        val next = current.pureFold {
          case Step.Done(a, e) => Done(a, e)
          case Step.Cont(k) => k(chunk)
          case Step.Error(msg, e) => Error(msg, e)
        }(dec)

        next.onComplete {
          case Success(it) => itPromise.success(it)
          case Failure(e) => {
            val its = atomic { implicit txn =>
              redeemed() = Some(Failure(e))
              iteratees.swap(List())
            }
            itPromise.failure(e)
            its.foreach { case (it, p) => p.success(it) }
          }
        }(dec)
      }

      def end(e: Throwable) {
        val current: Iteratee[E, Unit] = mainIteratee.single.swap(Done((), Input.Empty))
        def endEveryone() = Future {
          val its = atomic { implicit txn =>
            redeemed() = Some(Failure(e))
            iteratees.swap(List())
          }
          its.foreach { case (it, p) => p.failure(e) }
        }(dec)

        current.fold { case _ => endEveryone() }(dec)
      }

      def end() {
        val current: Iteratee[E, Unit] = mainIteratee.single.swap(Done((), Input.Empty))
        def endEveryone() = Future {
          val its = atomic { implicit txn =>
            redeemed() = Some(Success(()))
            iteratees.swap(List())
          }
          its.foreach { case (it, p) => p.success(it) }
        }(dec)
        current.fold { case _ => endEveryone() }(dec)
      }

    }
    (enumerator, toPush)
  }

  /**
   * Enumeratee that times out if the iteratee it feeds to takes too long to consume available input.
   *
   * @param timeout The timeout period
   * @param unit the time unit
   */
  def lazyAndErrIfNotReady[E](timeout: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): Enumeratee[E, E] = new Enumeratee[E, E] {

    def applyOn[A](inner: Iteratee[E, A]): Iteratee[E, Iteratee[E, A]] = {
      def step(it: Iteratee[E, A]): K[E, Iteratee[E, A]] = {
        case Input.EOF => Done(it, Input.EOF)

        case other => Iteratee.flatten(
          Future.firstCompletedOf(
            it.unflatten.map(Left(_))(dec) :: timeoutFuture(Right(()), timeout, unit) :: Nil
          )(dec).map {
              case Left(Step.Cont(k)) => Cont(step(k(other)))
              case Left(done) => Done(done.it, other)
              case Right(_) => Error("iteratee is taking too long", other)
            }(dec)
        )
      }
      Cont(step(inner))
    }
  }

  /**
   * A buffering enumeratee.
   *
   * Maintains a buffer of maximum size maxBuffer, consuming as much of the input as the buffer will allow as quickly
   * as it comes, while allowing the iteratee it feeds to consume it as slowly as it likes.
   *
   * This is useful in situations where the enumerator holds expensive resources open, while the iteratee may be slow,
   * for example if the enumerator is a database result set that holds a transaction open, but the result set is being
   * serialised and fed directly to an HTTP response.
   *
   * @param maxBuffer The maximum number of items to buffer
   */
  def buffer[E](maxBuffer: Int): Enumeratee[E, E] = buffer[E](maxBuffer, length = (_: Input[E]) => 1)(dec)

  /**
   * A buffering enumeratee.
   *
   * Maintains a buffer of maximum size maxBuffer, consuming as much of the input as the buffer will allow as quickly
   * as it comes, while allowing the iteratee it feeds to consume it as slowly as it likes.
   *
   * This is useful in situations where the enumerator holds expensive resources open, while the iteratee may be slow,
   * for example if the enumerator is a database result set that holds a transaction open, but the result set is being
   * serialised and fed directly to an HTTP response.
   *
   * @param maxBuffer The maximum size to buffer.  The size is computed using the given `length` function.
   * @param length A function that computes the length of an input item
   * $paramEcSingle
   */
  def buffer[E](maxBuffer: Int, length: Input[E] => Int)(implicit ec: ExecutionContext): Enumeratee[E, E] = new Enumeratee[E, E] {
    val pec = ec.prepare()

    import scala.collection.immutable.Queue
    import scala.concurrent.stm._
    import play.api.libs.iteratee.Enumeratee.CheckDone

    def applyOn[A](it: Iteratee[E, A]): Iteratee[E, Iteratee[E, A]] = {

      val last = Promise[Iteratee[E, Iteratee[E, A]]]()

      sealed trait State
      case class Queueing(q: Queue[Input[E]], length: Long) extends State
      case class Waiting(p: scala.concurrent.Promise[Input[E]]) extends State
      case class DoneIt(s: Iteratee[E, Iteratee[E, A]]) extends State

      val state: Ref[State] = Ref(Queueing(Queue[Input[E]](), 0))

      def step: K[E, Iteratee[E, A]] = {
        case in @ Input.EOF =>
          state.single.getAndTransform {
            case Queueing(q, l) => Queueing(q.enqueue(in), l)

            case Waiting(p) => Queueing(Queue(), 0)

            case d @ DoneIt(it) => d

          } match {
            case Waiting(p) =>
              p.success(in)
            case _ =>

          }
          Iteratee.flatten(last.future)

        case other =>
          Iteratee.flatten(Future(length(other))(pec).map { chunkLength =>
            val s = state.single.getAndTransform {
              case Queueing(q, l) if maxBuffer > 0 && l <= maxBuffer => Queueing(q.enqueue(other), l + chunkLength)

              case Queueing(q, l) => Queueing(Queue(Input.EOF), l)

              case Waiting(p) => Queueing(Queue(), 0)

              case d @ DoneIt(it) => d

            }
            s match {
              case Waiting(p) =>
                p.success(other)
                Cont(step)
              case DoneIt(it) => it
              case Queueing(q, l) if maxBuffer > 0 && l <= maxBuffer => Cont(step)
              case Queueing(_, _) => Error("buffer overflow", other)

            }
          }(dec))
      }

      def moreInput[A](k: K[E, A]): Iteratee[E, Iteratee[E, A]] = {
        val in: Future[Input[E]] = atomic { implicit txn =>
          state() match {
            case Queueing(q, l) =>
              if (!q.isEmpty) {
                val (e, newB) = q.dequeue
                state() = Queueing(newB, l - length(e))
                Future.successful(e)
              } else {
                val p = Promise[Input[E]]()
                state() = Waiting(p)
                p.future
              }
            case _ => throw new Exception("can't get here")
          }
        }
        Iteratee.flatten(in.map { in => (new CheckDone[E, E] { def continue[A](cont: K[E, A]) = moreInput(cont) } &> k(in)) }(dec))

      }
      (new CheckDone[E, E] { def continue[A](cont: K[E, A]) = moreInput(cont) } &> it).unflatten.onComplete {
        case Success(it) =>
          state.single() = DoneIt(it.it)
          last.success(it.it)
        case Failure(e) =>
          state.single() = DoneIt(Iteratee.flatten(Future.failed[Iteratee[E, Iteratee[E, A]]](e)))
          last.failure(e)

      }(dec)
      Cont(step)

    }
  }

  /**
   * An enumeratee that consumes all input immediately, and passes it to the iteratee only if the iteratee is ready to
   * handle it within the given timeout, otherwise it drops it.
   *
   * @param duration The time to wait for the iteratee to be ready
   * @param unit The timeunit
   */
  def dropInputIfNotReady[E](duration: Long, unit: java.util.concurrent.TimeUnit = java.util.concurrent.TimeUnit.MILLISECONDS): Enumeratee[E, E] = new Enumeratee[E, E] {

    val busy = scala.concurrent.stm.Ref(false)
    def applyOn[A](it: Iteratee[E, A]): Iteratee[E, Iteratee[E, A]] = {

      def step(inner: Iteratee[E, A])(in: Input[E]): Iteratee[E, Iteratee[E, A]] = {

        in match {
          case Input.EOF =>
            Done(inner, Input.Empty)

          case in =>
            if (!busy.single()) {
              val readyOrNot: Future[Either[Iteratee[E, Iteratee[E, A]], Unit]] = Future.firstCompletedOf(
                Seq(
                  inner.pureFold[Iteratee[E, Iteratee[E, A]]] {
                    case Step.Done(a, e) => Done(Done(a, e), Input.Empty)
                    case Step.Cont(k) => Cont { in =>
                      val next = k(in)
                      Cont(step(next))
                    }
                    case Step.Error(msg, e) => Done(Error(msg, e), Input.Empty)
                  }(dec).map(i => { busy.single() = false; Left(i) })(dec),
                  timeoutFuture(Right(()), duration, unit)
                )
              )(dec)

              Iteratee.flatten(readyOrNot.map {
                case Left(ready) =>
                  Iteratee.flatten(ready.feed(in))
                case Right(_) =>
                  busy.single() = true
                  Cont(step(inner))
              }(dec))
            } else Cont(step(inner))
        }
      }

      Cont(step(it))
    }
  }

  /**
   * Create an enumerator that allows imperative style pushing of input into a single iteratee.
   *
   * The enumerator may be used multiple times, each time will cause a new invocation of `onStart`, which will pass a
   * [[play.api.libs.iteratee.Concurrent.Channel]] that can be used to feed input into the iteratee.  However, note that
   * there is no way for the caller to know which iteratee is finished or encountered an error in the `onComplete` or
   * `onError` functions.
   *
   * @param onStart Called when an enumerator is applied to an iteratee, providing the channel to feed input into that
   *                iteratee.
   * @param onComplete Called when an iteratee is done.
   * @param onError Called when an iteratee encounters an error, supplying the error and the input that caused the error.
   * $paramEcMultiple
   */
  def unicast[E](
    onStart: Channel[E] => Unit,
    onComplete: => Unit = (),
    onError: (String, Input[E]) => Unit = (_: String, _: Input[E]) => ())(implicit ec: ExecutionContext) = new Enumerator[E] {
    val pec = ec.prepare()

    import scala.concurrent.stm.Ref

    def apply[A](it: Iteratee[E, A]): Future[Iteratee[E, A]] = {
      val promise: scala.concurrent.Promise[Iteratee[E, A]] = Promise[Iteratee[E, A]]()
      val iteratee: Ref[Future[Option[Input[E] => Iteratee[E, A]]]] = Ref(it.pureFold { case Step.Cont(k) => Some(k); case other => promise.success(other.it); None }(dec))

      val pushee = new Channel[E] {
        def close() {
          iteratee.single.swap(Future.successful(None)).onComplete {
            case Success(maybeK) => maybeK.foreach { k =>
              promise.success(k(Input.EOF))
            }
            case Failure(e) => promise.failure(e)
          }(dec)
        }

        def end(e: Throwable) {
          iteratee.single.swap(Future.successful(None)).onComplete {
            case Success(maybeK) =>
              maybeK.foreach(_ => promise.failure(e))
            case Failure(e) => promise.failure(e)
          }(dec)
        }

        def end() {
          iteratee.single.swap(Future.successful(None)).onComplete { maybeK =>
            maybeK.get.foreach(k => promise.success(Cont(k)))
          }(dec)
        }

        def push(item: Input[E]) {
          val eventuallyNext = Promise[Option[Input[E] => Iteratee[E, A]]]()
          iteratee.single.swap(eventuallyNext.future).onComplete {
            case Success(None) => eventuallyNext.success(None)
            case Success(Some(k)) =>
              val n = {
                val next = k(item)
                next.fold {
                  case Step.Done(a, in) => {
                    Future(onComplete)(pec).map { _ =>
                      promise.success(next)
                      None
                    }(dec)
                  }
                  case Step.Error(msg, e) =>
                    Future(onError(msg, e))(pec).map { _ =>
                      promise.success(next)
                      None
                    }(dec)
                  case Step.Cont(k) =>
                    Future.successful(Some(k))
                }(dec)
              }
              eventuallyNext.completeWith(n)
            case Failure(e) =>
              promise.failure(e)
              eventuallyNext.success(None)
          }(dec)
        }
      }
      Future(onStart(pushee))(pec).flatMap(_ => promise.future)(dec)
    }

  }

  /**
   * Create a broadcaster from the given enumerator.  This allows iteratees to attach (and unattach by returning a done
   * state) to a single enumerator.  Iteratees will only receive input sent from the enumerator after they have
   * attached to the broadcasting enumerator.
   *
   * @param e The enumerator to broadcast
   * @param interestIsDownToZero Function that is invoked when all iteratees are done.  May be invoked multiple times.
   * $paramEcSingle
   * @return A tuple of the broadcasting enumerator, that can be applied to each iteratee that wants to receive the
   *         input, and the broadcaster.
   */
  def broadcast[E](e: Enumerator[E], interestIsDownToZero: Broadcaster => Unit = _ => ())(implicit ec: ExecutionContext): (Enumerator[E], Broadcaster) = {
    val pec = ec.prepare()
    lazy val h: Hub[E] = hub(e, () => interestIsDownToZero(h))(pec)
    (h.getPatchCord(), h)
  }

  /**
   * A broadcaster.  Used to control a broadcasting enumerator.
   */
  trait Broadcaster {
    /**
     * Are there any iteratees that are still receiving input?
     */
    def noCords(): Boolean

    /**
     * Close the broadcasting enumerator.
     */
    def close()

    /**
     * Whether this broadcaster is closed.
     */
    def closed(): Boolean

  }

  @scala.deprecated("use Concurrent.broadcast instead", "2.1.0")
  private trait Hub[E] extends Broadcaster {

    def getPatchCord(): Enumerator[E]

  }

  @scala.deprecated("use Concurrent.broadcast instead", "2.1.0")
  private def hub[E](e: Enumerator[E], interestIsDownToZero: () => Unit = () => ())(implicit ec: ExecutionContext): Hub[E] = {
    val pec = ec.prepare()

    import scala.concurrent.stm._

    val iteratees: Ref[List[(Iteratee[E, _], Promise[Iteratee[E, _]])]] = Ref(List())

    val started = Ref(false)

    var closeFlag = false

    def step(in: Input[E]): Iteratee[E, Unit] = {
      val interested: List[(Iteratee[E, _], Promise[Iteratee[E, _]])] = iteratees.single.swap(List())

      val commitReady: Ref[List[(Int, (Iteratee[E, _], Promise[Iteratee[E, _]]))]] = Ref(List())

      val commitDone: Ref[List[Int]] = Ref(List())

      val ready = interested.zipWithIndex.map {
        case (t, index) =>
          val p = t._2
          t._1.fold {
            case Step.Done(a, e) =>
              p.success(Done(a, e))
              commitDone.single.transform(_ :+ index)
              Future.successful(())

            case Step.Cont(k) =>
              val next = k(in)
              next.pureFold {
                case Step.Done(a, e) => {
                  p.success(Done(a, e))
                  commitDone.single.transform(_ :+ index)
                }
                case Step.Cont(k) => commitReady.single.transform(_ :+ (index, (Cont(k), p)))
                case Step.Error(msg, e) => {
                  p.success(Error(msg, e))
                  commitDone.single.transform(_ :+ index)
                }
              }(dec)

            case Step.Error(msg, e) =>
              p.success(Error(msg, e))
              commitDone.single.transform(_ :+ index)
              Future.successful(())
          }(dec).andThen {
            case Success(a) => a
            case Failure(e) => p.failure(e)
          }(dec)
      }.fold(Future.successful(())) { (s, p) => s.flatMap(_ => p)(dec) }

      Iteratee.flatten(ready.flatMap { _ =>

        val downToZero = atomic { implicit txn =>
          val ready = commitReady().toMap
          iteratees.transform(commitReady().map(_._2) ++ _)
          (interested.length > 0 && iteratees().length <= 0)

        }
        def result(): Iteratee[E, Unit] = if (in == Input.EOF || closeFlag) Done((), Input.Empty) else Cont(step)
        if (downToZero) Future(interestIsDownToZero())(pec).map(_ => result())(dec) else Future.successful(result())

      }(dec))
    }

    new Hub[E] {

      def noCords() = iteratees.single().isEmpty

      def close() {
        closeFlag = true
      }

      def closed() = closeFlag

      val redeemed = Ref(None: Option[Try[Iteratee[E, Unit]]])
      def getPatchCord() = new Enumerator[E] {

        def apply[A](it: Iteratee[E, A]): Future[Iteratee[E, A]] = {
          val result = Promise[Iteratee[E, A]]()
          val alreadyStarted = !started.single.compareAndSet(false, true)
          if (!alreadyStarted) {
            val promise = (e |>> Cont(step))
            promise.onComplete { v =>
              val its = atomic { implicit txn =>
                redeemed() = Some(v)
                iteratees.swap(List())
              }
              v match {
                case Failure(e) =>
                  its.foreach { case (_, p) => p.failure(e) }

                case Success(_) =>
                  its.foreach { case (it, p) => p.success(it) }
              }
            }(dec)
          }
          val finished = atomic { implicit txn =>
            redeemed() match {
              case None =>
                iteratees.transform(_ :+ ((it, (result: Promise[Iteratee[E, A]]).asInstanceOf[Promise[Iteratee[E, _]]])))
                None
              case Some(notWaiting) => Some(notWaiting)
            }
          }
          finished.foreach {
            case Success(_) => result.success(it)
            case Failure(e) => result.failure(e)
            case _ => throw new RuntimeException("should be either Redeemed or Thrown")
          }
          result.future
        }

      }

    }
  }

  /**
   * Allows patching in enumerators to an iteratee.
   */
  trait PatchPanel[E] {

    /**
     * Patch in the given enumerator into the iteratee.
     *
     * @return Whether the enumerator was successfully patched in.  Will return false if the patch panel is closed.
     */
    def patchIn(e: Enumerator[E]): Boolean

    /**
     * Whether the patch panel is closed.
     *
     * The patch panel will become closed when the iteratee it is feeding is done or is error.
     */
    def closed(): Boolean

  }

  /**
   * An enumerator that allows patching in enumerators to supply it with input.
   *
   * @param patcher A function that passes a patch panel whenever the enumerator is applied to an iteratee.
   * $paramEcSingle
   */
  def patchPanel[E](patcher: PatchPanel[E] => Unit)(implicit ec: ExecutionContext): Enumerator[E] = new Enumerator[E] {
    val pec = ec.prepare()

    import scala.concurrent.stm._

    def apply[A](it: Iteratee[E, A]): Future[Iteratee[E, A]] = {
      val result = Promise[Iteratee[E, A]]()
      var isClosed: Boolean = false

      result.future.onComplete(_ => isClosed = true)(dec)

      def refIteratee(ref: Ref[Iteratee[E, Option[A]]]): Iteratee[E, Option[A]] = {
        val next = Promise[Iteratee[E, Option[A]]]()
        val current = ref.single.swap(Iteratee.flatten(next.future))
        current.pureFlatFold {
          case Step.Done(a, e) => {
            a.foreach(aa => result.success(Done(aa, e)))
            next.success(Done(a, e))
            Done(a, e)
          }
          case Step.Cont(k) => {
            next.success(current)
            Cont(step(ref))
          }
          case Step.Error(msg, e) => {
            result.success(Error(msg, e))
            next.success(Error(msg, e))
            Error(msg, e)

          }
        }(dec)

      }

      def step(ref: Ref[Iteratee[E, Option[A]]])(in: Input[E]): Iteratee[E, Option[A]] = {
        val next = Promise[Iteratee[E, Option[A]]]()
        val current = ref.single.swap(Iteratee.flatten(next.future))
        current.pureFlatFold {
          case Step.Done(a, e) => {
            next.success(Done(a, e))
            Done(a, e)
          }
          case Step.Cont(k) => {
            val n = k(in)
            next.success(n)
            n.pureFlatFold {
              case Step.Done(a, e) => {
                a.foreach(aa => result.success(Done(aa, e)))
                Done(a, e)
              }
              case Step.Cont(k) => Cont(step(ref))
              case Step.Error(msg, e) => {
                result.success(Error(msg, e))
                Error(msg, e)
              }
            }(dec)
          }
          case Step.Error(msg, e) => {
            next.success(Error(msg, e))
            Error(msg, e)
          }
        }(dec)
      }

      Future(patcher(new PatchPanel[E] {
        val ref: Ref[Ref[Iteratee[E, Option[A]]]] = Ref(Ref(it.map(Some(_))(dec)))

        def closed() = isClosed

        def patchIn(e: Enumerator[E]): Boolean = {
          !(closed() || {
            val newRef = atomic { implicit txn =>
              val enRef = ref()
              val it = enRef.swap(Done(None, Input.Empty))
              val newRef = Ref(it)
              ref() = newRef
              newRef
            }
            e |>> refIteratee(newRef) //TODO maybe do something if the enumerator is done, maybe not
            false
          })
        }
      }))(pec).flatMap(_ => result.future)(dec)

    }
  }

  /**
   * Create a joined iteratee enumerator pair.
   *
   * When the enumerator is applied to an iteratee, the iteratee subsequently consumes whatever the iteratee in the pair
   * is applied to.  Consequently the enumerator is "one shot", applying it to subsequent iteratees will throw an
   * exception.
   */
  def joined[A]: (Iteratee[A, Unit], Enumerator[A]) = {
    val promisedIteratee = Promise[Iteratee[A, Unit]]()
    val enumerator = new Enumerator[A] {
      def apply[B](i: Iteratee[A, B]) = {
        val doneIteratee = Promise[Iteratee[A, B]]()

        // Equivalent to map, but allows us to handle failures
        def wrap(delegate: Iteratee[A, B]): Iteratee[A, B] = new Iteratee[A, B] {
          def fold[C](folder: (Step[A, B]) => Future[C])(implicit ec: ExecutionContext) = {
            val toReturn = delegate.fold {
              case done @ Step.Done(a, in) => {
                doneIteratee.success(done.it)
                folder(done)
              }
              case Step.Cont(k) => {
                folder(Step.Cont(k.andThen(wrap)))
              }
              case err => folder(err)
            }(ec)
            toReturn.onFailure {
              case e => doneIteratee.failure(e)
            }(dec)
            toReturn
          }
        }

        if (promisedIteratee.trySuccess(wrap(i).map(_ => ())(dec))) {
          doneIteratee.future
        } else {
          throw new IllegalStateException("Joined enumerator may only be applied once")
        }
      }
    }
    (Iteratee.flatten(promisedIteratee.future), enumerator)
  }

  /**
   * Run the enumerator, and produce the remaining enumerator as part the result.
   *
   * The result will be the result of the iteratee, and an enumerator containing the remaining input.
   */
  def runPartial[E, A](enumerator: Enumerator[E], iteratee: Iteratee[E, A]): Future[(A, Enumerator[E])] = {
    val result = Promise[(A, Enumerator[E])]()

    (enumerator |>>> iteratee.flatMap { a =>
      val (consumeRemaining, remaining) = Concurrent.joined[E]
      result.success((a, remaining))
      consumeRemaining
    }(dec)).onFailure {
      case e => result.tryFailure(e)
    }(dec)

    result.future
  }
}
