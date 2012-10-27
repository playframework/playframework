package play.api.libs.iteratee

import scala.concurrent.Future
import scala.concurrent.Promise
import scala.util.{Try, Failure, Success}
import Enumerator.Pushee
import java.util.concurrent.{ TimeUnit }

import play.api.libs.iteratee.internal.defaultExecutionContext

object Concurrent {

  private val timer = new java.util.Timer()

  private def timeoutFuture[A](v:A, delay:Long, unit:TimeUnit):Future[A] = {

    val p = Promise[A]()
    timer.schedule( new java.util.TimerTask{
      def run(){
        p.success(v)
      }
    },unit.toMillis(delay) )
    p.future
  } 

  trait Channel[E] {

    def push(chunk: Input[E])

    def push(item: E) { push(Input.El(item)) }

    def end(e: Throwable)

    def end()

    def eofAndEnd() {
      push(Input.EOF)
      end()
    }
  }

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
              }
            }
            case Step.Error(msg, e) => Future.successful(Left(Error(msg, e)))
          }.map {
            case Left(s) =>
              p.success(s)
              None
            case Right(s) =>
              Some(s)
          }.recover {
            case e:Throwable =>
              p.failure(e)
              None
          }
      }

      Iteratee.flatten(Future.sequence(ready).map { commitReady =>

        val downToZero = atomic { implicit txn =>
          iteratees.transform(commitReady.collect { case Some(s) => s } ++ _)
          (interested.length > 0 && iteratees().length <= 0)
        }

        if (in == Input.EOF) Done((), Input.Empty) else Cont(step)

      })
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
        }

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
        }
      }

      def end(e: Throwable) {
        val current: Iteratee[E, Unit] = mainIteratee.single.swap(Done((), Input.Empty))
        def endEveryone() = {
          val its = atomic { implicit txn =>
            redeemed() = Some(Failure(e))
            iteratees.swap(List())
          }
          its.foreach { case (it, p) => p.failure(e) }
        }

        current.pureFold { case _ => endEveryone() }
      }

      def end() {
        val current: Iteratee[E, Unit] = mainIteratee.single.swap(Done((), Input.Empty))
        def endEveryone() = {
          val its = atomic { implicit txn =>
            redeemed() = Some(Success(()))
            iteratees.swap(List())
          }
          its.foreach { case (it, p) => p.success(it) }
        }
        current.pureFold { case _ => endEveryone() }
      }

    }
    (enumerator, toPush)
  }

  def lazyAndErrIfNotReady[E](timeout: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): Enumeratee[E, E] = new Enumeratee[E, E] {

    def applyOn[A](inner: Iteratee[E, A]): Iteratee[E, Iteratee[E, A]] = {
      def step(it: Iteratee[E, A]): K[E, Iteratee[E, A]] = {
        case Input.EOF => Done(it, Input.EOF)

        case other => Iteratee.flatten(it.unflatten.map(Left(_)).either(timeoutFuture(Right(()), timeout, unit)).map {
          case Left(Step.Cont(k)) => Cont(step(k(other)))

          case Left(done) => Done(done.it, other)

          case Right(_) => Error("iteratee is taking too long", other)

        })
      }
      Cont(step(inner))
    }
  }
  def buffer[E](maxBuffer: Int): Enumeratee[E, E] = buffer[E](maxBuffer, length = (_: Input[E]) => 1)

  def buffer[E](maxBuffer: Int, length: Input[E] => Int): Enumeratee[E, E] = new Enumeratee[E, E] {

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
          val chunkLength = length(other)
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
        Iteratee.flatten(in.map { in => (new CheckDone[E, E] { def continue[A](cont: K[E, A]) = moreInput(cont) } &> k(in)) })

      }
      (new CheckDone[E, E] { def continue[A](cont: K[E, A]) = moreInput(cont) } &> it).unflatten.onComplete {
        case Success(it) =>
          state.single() = DoneIt(it.it)
          last.success(it.it)
        case Failure(e) =>
          state.single() = DoneIt(Iteratee.flatten(Future.failed[Iteratee[E, Iteratee[E, A]]](e)))
          last.failure(e)

      }
      Cont(step)

    }
  }

  def dropInputIfNotReady[E](duration: Long, unit: java.util.concurrent.TimeUnit = java.util.concurrent.TimeUnit.MILLISECONDS): Enumeratee[E, E] = new Enumeratee[E, E] {

    val busy = scala.concurrent.stm.Ref(false)
    def applyOn[A](it: Iteratee[E, A]): Iteratee[E, Iteratee[E, A]] = {

      def step(inner: Iteratee[E, A])(in: Input[E]): Iteratee[E, Iteratee[E, A]] = {

        in match {
          case Input.EOF =>
            Done(inner, Input.Empty)

          case in =>
            if (!busy.single()) {
              val readyOrNot: Future[Either[Iteratee[E, Iteratee[E, A]], Unit]] = inner.pureFold[Iteratee[E, Iteratee[E, A]]] {
                case Step.Done(a, e) => Done(Done(a, e), Input.Empty)
                case Step.Cont(k) => Cont { in =>
                  val next = k(in)
                  Cont(step(next))
                }
                case Step.Error(msg, e) => Done(Error(msg, e), Input.Empty)
              }.map(i => { busy.single() = false; i }).map(Left(_)).either(timeoutFuture(Right(()), duration, unit))

              Iteratee.flatten(readyOrNot.map {
                case Left(ready) =>
                  Iteratee.flatten(ready.feed(in))
                case Right(_) =>
                  busy.single() = true
                  Cont(step(inner))
              })
            } else Cont(step(inner))
        }
      }

      Cont(step(it))
    }
  }

  def unicast[E](
    onStart: Channel[E] => Unit,
    onComplete: => Unit = (),
    onError: (String, Input[E]) => Unit = (_: String, _: Input[E]) => ()) = new Enumerator[E] {

    import scala.concurrent.stm.Ref

    def apply[A](it: Iteratee[E, A]): Future[Iteratee[E, A]] = {
      val promise: scala.concurrent.Promise[Iteratee[E, A]] = Promise[Iteratee[E, A]]()
      val iteratee: Ref[Future[Option[Input[E] => Iteratee[E, A]]]] = Ref(it.pureFold { case  Step.Cont(k) => Some(k); case other => promise.success(other.it); None})

      val pushee = new Channel[E] {
        def close() {
          iteratee.single.swap(Future.successful(None)).onComplete{
            case Success(maybeK) => maybeK.foreach { k => 
              promise.success(k(Input.EOF))
            }
            case Failure(e) => promise.failure(e)
          }
        }

        def end(e: Throwable) {
          iteratee.single.swap(Future.successful(None)).onComplete { 
            case Success(maybeK) =>
              maybeK.foreach(_ => promise.failure(e))
            case Failure(e) => promise.failure(e)
          }
        }

        def end() {
          iteratee.single.swap(Future.successful(None)).onComplete { maybeK =>
            maybeK.get.foreach(k => promise.success(Cont(k)))
          }
        }

        def push(item: Input[E]) {
          val eventuallyNext = Promise[Option[Input[E] => Iteratee[E,A]]]()
          iteratee.single.swap(eventuallyNext.future).onComplete {
            case Success(None) => eventuallyNext.success(None)
            case Success(Some(k)) =>
               val n = {
                  val next = k(item)
                  next.pureFold {
                    case Step.Done(a, in) => {
                      onComplete
                      promise.success(next)
                      None
                    }
                    case Step.Error(msg, e) =>
                      onError(msg, e)
                      promise.success(next)
                      None
                    case Step.Cont(k) =>
                      Some(k)
                  }
                }
              eventuallyNext.completeWith(n)
          case Failure(e) => 
            promise.failure(e)
            eventuallyNext.success(None)
          }
        }
      }
      onStart(pushee)
      promise.future
    }

    }

  def broadcast[E](e: Enumerator[E], interestIsDownToZero: Broadcaster => Unit = _ => ()): (Enumerator[E], Broadcaster) = { lazy val h: Hub[E] = hub(e, () => interestIsDownToZero(h)); (h.getPatchCord(), h) }

  trait Broadcaster {
    def noCords(): Boolean

    def close()

    def closed(): Boolean

  }

  @scala.deprecated("use Concurrent.broadcast instead", "2.1.0")
  trait Hub[E] extends Broadcaster {

    def getPatchCord(): Enumerator[E]

  }

  @scala.deprecated("use Concurrent.broadcast instead", "2.1.0")
  def hub[E](e: Enumerator[E], interestIsDownToZero: () => Unit = () => ()): Hub[E] = {

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
              }

            case Step.Error(msg, e) =>
              p.success(Error(msg, e))
              commitDone.single.transform(_ :+ index)
              Future.successful(())
          }.andThen {
            case Success(a) => a
            case Failure(e) => p.failure(e)
          }
      }.fold(Future.successful(())) { (s, p) => s.flatMap(_ => p) }

      Iteratee.flatten(ready.map { _ =>

        val downToZero = atomic { implicit txn =>
          val ready = commitReady().toMap
          iteratees.transform(commitReady().map(_._2) ++ _)
          (interested.length > 0 && iteratees().length <= 0)

        }
        if (downToZero) interestIsDownToZero()
        if (in == Input.EOF || closeFlag) Done((), Input.Empty) else Cont(step)

      })
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
            }
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

  trait PatchPanel[E] {

    def patchIn(e: Enumerator[E]): Boolean

    def closed(): Boolean

  }
  def patchPanel[E](patcher: PatchPanel[E] => Unit): Enumerator[E] = new Enumerator[E] {

    import scala.concurrent.stm._

    def apply[A](it: Iteratee[E, A]): Future[Iteratee[E, A]] = {
      val result = Promise[Iteratee[E, A]]()
      var isClosed: Boolean = false

      result.future.onComplete(_ => isClosed = true);

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
        }

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
            }
          }
          case Step.Error(msg, e) => {
            next.success(Error(msg, e))
            Error(msg, e)
          }
        }
      }

      patcher(new PatchPanel[E] {
        val ref: Ref[Ref[Iteratee[E, Option[A]]]] = Ref(Ref(it.map(Some(_))))

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
      })

      result.future

    }
  }
}
