package play.api.libs.iteratee

import play.api.libs.concurrent._
import Enumerator.Pushee

object Concurrent {

  trait Channel[E] {

    def push(chunk: Input[E])

    def push(item: E){ push(Input.El(item)) }

    def end(e:Throwable)

    def end()

    def eofAndEnd(){
      push(Input.EOF)
      end()
    }
  }

  def broadcast[E]:(Enumerator[E],Channel[E]) = {

    import scala.concurrent.stm._

    val iteratees: Ref[List[(Iteratee[E, _], Redeemable[Iteratee[E, _]])]] = Ref(List())

    def step(in: Input[E]): Iteratee[E, Unit] = {
      val interested = iteratees.single.swap(List())

      val ready = interested.map {
        case (it,p) =>
          it.fold {
            case Step.Done(a, e) => Promise.pure(Left(Done(a,e)))
            case Step.Cont(k) => {
              val next = k(in)
              next.pureFold {
                case Step.Done(a, e) => Left(Done(a, e))
                case Step.Cont(k) => Right((Cont(k),p))
                case Step.Error(msg, e) => Left(Error(msg, e))
              }
            }
            case Step.Error(msg, e) => Promise.pure(Left(Error(msg, e)))
          }.extend1 {
              case Redeemed(Left(s)) => 
                p.redeem(s)
                None
              case Redeemed(Right(s)) =>
                Some(s)
              case Thrown(e) => 
                p.throwing(e)
                None
            }
      }

      Iteratee.flatten(Promise.sequence(ready).map { commitReady =>

        val downToZero = atomic { implicit txn =>
          iteratees.transform(commitReady.collect{case Some(s) => s} ++ _)
          (interested.length > 0 && iteratees().length <= 0)
        }

        if (in == Input.EOF) Done((), Input.Empty) else Cont(step)

      })
    }

    val redeemed = Ref(Waiting: PromiseValue[Unit])

    val enumerator = new Enumerator[E] {

        def apply[A](it: Iteratee[E, A]): Promise[Iteratee[E, A]] = {
          val result = Promise[Iteratee[E, A]]()
         

          val finished = atomic { implicit txn =>
            redeemed() match {
              case Waiting =>
                iteratees.transform(_ :+ ((it, result.asInstanceOf[Redeemable[Iteratee[E, _]]])))
                None
              case notWaiting:NotWaiting[_] => Some(notWaiting)
            }
          }
          finished.foreach {
            case Redeemed(_) => result.redeem(it)
            case Thrown(e) => result.throwing(e)
          }
          result.future
        }

    }

    val mainIteratee =  Ref(Cont(step))

    val toPush = new Channel[E]{

      def push(chunk: Input[E]) {

        val itPromise = Promise[Iteratee[E,Unit]]()

        val current: Iteratee[E,Unit] = mainIteratee.single.swap(Iteratee.flatten(itPromise.future))

        val next = current.pureFold {
          case Step.Done(a, e) => Done(a,e)
          case Step.Cont(k) => k(chunk)
          case Step.Error(msg, e) => Error(msg,e)
        }

        next.extend1{
          case Redeemed(it) =>  itPromise.redeem(it)
          case Thrown(e) => {
            val its = atomic { implicit txn =>
              redeemed() = Thrown(e)
              iteratees.swap(List())
            }
            itPromise.throwing(e)
            its.foreach { case (it, p) => p.redeem(it)}
          }
        }
      }

      def end(e:Throwable){
        val current: Iteratee[E,Unit] = mainIteratee.single.swap(Done((),Input.Empty))
        def endEveryone() = {
          val its = atomic { implicit txn =>
            redeemed() = Thrown(e)
            iteratees.swap(List())
                          }
          its.foreach { case (it, p) => p.throwing(e)}
        }

        current.pureFold { case _ => endEveryone() }
      }

      def end() {
        val current: Iteratee[E,Unit] = mainIteratee.single.swap(Done((),Input.Empty))
        def endEveryone() = {
          val its = atomic { implicit txn =>
            redeemed() = Redeemed(())
            iteratees.swap(List())
          }
          its.foreach { case (it, p) => p.redeem(it)}
        }
        current.pureFold { case _ => endEveryone() }
      }

    }
    (enumerator,toPush)
  }


  import java.util.concurrent.{ TimeUnit }
  def lazyAndErrIfNotReady[E](timeout:Int, unit: TimeUnit = TimeUnit.MILLISECONDS): Enumeratee[E,E] = new Enumeratee[E,E] {

    def applyOn[A](inner: Iteratee[E,A]): Iteratee[E, Iteratee[E,A]] = {
      def step(it:Iteratee[E,A]):K[E, Iteratee[E,A]] = {
        case Input.EOF => Done(it,Input.EOF)

        case other => Iteratee.flatten(it.unflatten.orTimeout((),timeout,unit).map{
          case Left(Step.Cont(k)) => Cont(step( k(other)) )

          case Left(done) => Done(done.it,other)

          case Right(_) => Error("iteratee is taking too long", other)

        })
      }
      Cont(step(inner))
    }
  }

  def buffer[E](maxBuffer:Int): Enumeratee[E,E] = new Enumeratee[E,E] {

    import scala.collection.immutable.Queue
    import scala.concurrent.stm._
    import play.api.libs.iteratee.Enumeratee.CheckDone

    def applyOn[A](it: Iteratee[E,A]): Iteratee[E, Iteratee[E,A]] = {

      val last = Promise[Iteratee[E,Iteratee[E,A]]]()

      sealed trait State
      case class Queueing(q:Queue[Input[E]]) extends State
      case class Waiting(p:scala.concurrent.Promise[Input[E]]) extends State
      case class DoneIt(s:Iteratee[E,Iteratee[E,A]]) extends State

      val state: Ref[State] = Ref(Queueing(Queue[Input[E]]()))

      def step:K[E, Iteratee[E,A]] = {
        case in@Input.EOF =>
          state.single.getAndTransform {
            case Queueing(q) => Queueing(q.enqueue(in))

            case Waiting(p) => Queueing(Queue())

            case d@DoneIt(it) => d

        } match {
            case Waiting(p) =>
              p.redeem(in)
            case _ =>

          }
          Iteratee.flatten(last.future)

        case other => 
          val s = state.single.getAndTransform {
            case Queueing(q) if maxBuffer > 0 && q.length <= maxBuffer => Queueing(q.enqueue(other))

            case Queueing(q) => Queueing(Queue(Input.EOF))

            case Waiting(p) => Queueing(Queue())

            case d@DoneIt(it) => d

            }
          s match {
            case Waiting(p) =>
              p.redeem(other)
              Cont(step)
            case DoneIt(it) => it
            case Queueing(q) if maxBuffer > 0 && q.length <= maxBuffer => Cont(step)
            case Queueing(_) => Error("buffer overflow", other)

          }
      }

      def moreInput[A](k: K[E, A]): Iteratee[E,Iteratee[E,A]] = {
        val in: Promise[Input[E]] = atomic { implicit txn =>
            state() match {
              case Queueing(q) =>
                if(!q.isEmpty){
                  val (e,newB) = q.dequeue
                  state() = Queueing(newB)
                  Promise.pure(e)
                } else {
                  val p = Promise[Input[E]]()
                  state() = Waiting(p)
                  p.future
                }
              case _ => throw new Exception("can't get here")
            }
         }
         Iteratee.flatten(in.map { in => (new CheckDone[E,E] { def continue[A](cont: K[E, A]) = moreInput(cont) } &> k(in)) })
            
      }
      (new CheckDone[E,E] { def continue[A](cont: K[E, A]) = moreInput(cont) } &> it).unflatten.extend1 {
        case Redeemed(it) =>
          state.single() = DoneIt(it.it)
          last.redeem(it.it)
        case Thrown(e) =>
          state.single() = DoneIt(Iteratee.flatten(Promise.pure[Iteratee[E,Iteratee[E,A]]]( throw e)))
          last.throwing(e)


      }
      Cont(step)

    }
  }

  def dropInputIfNotReady[E](duration: Long, unit: java.util.concurrent.TimeUnit = java.util.concurrent.TimeUnit.MILLISECONDS): Enumeratee[E, E] = new Enumeratee[E, E] {

    def applyOn[A](it: Iteratee[E, A]): Iteratee[E, Iteratee[E, A]] = {

      def step(inner: Iteratee[E, A])(in: Input[E]): Iteratee[E, Iteratee[E, A]] = {

        in match {
          case Input.EOF =>
            Done(inner, Input.Empty)

          case in =>
            val readyOrNot: Promise[Either[Iteratee[E, Iteratee[E, A]], Unit]] = inner.pureFold[Iteratee[E, Iteratee[E, A]]] {
              case Step.Done(a, e) => Done(Done(a, e), Input.Empty)
              case Step.Cont(k) => Cont { in =>
                val next = k(in)
                Cont(step(next))
              }
              case Step.Error(msg, e) => Done(Error(msg, e), Input.Empty)}.orTimeout((), duration, unit)

            Iteratee.flatten(readyOrNot.map {
              case Left(ready) => Iteratee.flatten(ready.feed(in))
              case Right(_) => Cont(step(inner))
            })
        }
      }

      Cont(step(it))
    }
  }

  def unicast[E](
    onStart: Channel[E] => Unit,
    onComplete: => Unit,
    onError: (String, Input[E]) => Unit = (_: String, _: Input[E]) => ()) = new Enumerator[E] {

    def apply[A](it: Iteratee[E, A]): Promise[Iteratee[E, A]] = {
      var iteratee: Iteratee[E, A] = it
      var promise: scala.concurrent.Promise[Iteratee[E, A]] = Promise[Iteratee[E, A]]()

      val pushee = new Channel[E] {
        def close() {
          if (iteratee != null) {
            iteratee.feed(Input.EOF).map(result => promise.redeem(result))
            iteratee = null
            promise = null
          }
        }

        def end(e:Throwable){
          if (iteratee != null) {
            promise.throwing(e)
            iteratee = null
            promise = null
          }
        }

        def end(){
          if (iteratee != null) {
            promise.redeem(iteratee)
            iteratee = null
            promise = null
          }
        }

        def push(item: Input[E]) {
            iteratee = iteratee.pureFlatFold[E, A] {

              case Step.Done(a, in) => {
                onComplete
                Done(a, in)
              }

              case Step.Cont(k) => {
                val next = k(item)
                next.pureFlatFold {
                  case Step.Done(a, in) => {
                    onComplete
                    next
                  }
                  case Step.Error(msg,e) =>
                    onError(msg,e)
                    next
                  case _ => next
                }
              }

              case Step.Error(e, in) => {
                onError(e, in)
                Error(e, in)
              }
            }
        }
      }
      onStart(pushee)
      promise.future
    }

  }

  def broadcast[E](e: Enumerator[E],interestIsDownToZero: => Unit = ()): (Enumerator[E],Broadcaster) = {val h = hub(e,() => interestIsDownToZero); (h.getPatchCord(),h) }

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

    val iteratees: Ref[List[(Iteratee[E, _], Redeemable[Iteratee[E, _]])]] = Ref(List())

    val started = Ref(false)

    var closeFlag = false

    def step(in: Input[E]): Iteratee[E, Unit] = {
      val interested: List[(Iteratee[E, _], Redeemable[Iteratee[E, _]])] = iteratees.single.swap(List())

      val commitReady: Ref[List[(Int, (Iteratee[E, _], Redeemable[Iteratee[E, _]]))]] = Ref(List())

      val commitDone: Ref[List[Int]] = Ref(List())

      val ready = interested.zipWithIndex.map {
        case (t, index) =>
          val p = t._2
          t._1.fold {
            case Step.Done(a, e) =>
              p.redeem(Done(a, e))
              commitDone.single.transform(_ :+ index)
              Promise.pure(())

            case Step.Cont(k) =>
              val next = k(in)
              next.pureFold {
                case Step.Done(a, e) => {
                  p.redeem(Done(a, e))
                  commitDone.single.transform(_ :+ index)
                }
                case Step.Cont(k) => commitReady.single.transform(_ :+ (index, (Cont(k), p)))
                case Step.Error(msg, e) => {
                  p.redeem(Error(msg, e))
                  commitDone.single.transform(_ :+ index)
                }
              }

            case Step.Error(msg, e) =>
              p.redeem(Error(msg, e))
              commitDone.single.transform(_ :+ index)
              Promise.pure(())
          }.extend1 {
              case Redeemed(a) => a
              case Thrown(e) => p.throwing(e)
              case _ => throw new RuntimeException("should be either Redeemed or Thrown at this point")

            }
      }.fold(Promise.pure()) { (s, p) => s.flatMap(_ => p) }

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

      val redeemed = Ref(Waiting: PromiseValue[Iteratee[E, Unit]])
      def getPatchCord() = new Enumerator[E] {

        def apply[A](it: Iteratee[E, A]): Promise[Iteratee[E, A]] = {
          val result = Promise[Iteratee[E, A]]()
          val alreadyStarted = !started.single.compareAndSet(false, true)
          if (!alreadyStarted) {
            val promise = (e |>> Cont(step))
            promise.extend1 { v =>
              val its = atomic { implicit txn =>
                redeemed() = v
                iteratees.swap(List())
              }
              v match {
                case Thrown(e) =>
                  its.foreach { case (_, p) => p.throwing(e) }

                case Redeemed(_) =>
                  its.foreach { case (it, p) => p.redeem(it) }
              }
            }
          }
          val finished = atomic { implicit txn =>
            redeemed() match {
              case Waiting =>
                iteratees.transform(_ :+ ((it, result.asInstanceOf[Redeemable[Iteratee[E, _]]])))
                None
              case notWaiting => Some(notWaiting)
            }
          }
          finished.foreach {
            case Redeemed(_) => result.redeem(it)
            case Thrown(e) => result.throwing(e)
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

    def apply[A](it: Iteratee[E, A]): Promise[Iteratee[E, A]] = {
      val result = Promise[Iteratee[E, A]]()
      var isClosed: Boolean = false

      result.future.extend1(_ => isClosed = true);

      def refIteratee(ref: Ref[Iteratee[E, Option[A]]]): Iteratee[E, Option[A]] = {
        val next = Promise[Iteratee[E, Option[A]]]()
        val current = ref.single.swap(Iteratee.flatten(next.future))
        current.pureFlatFold {
          case Step.Done(a, e) => {
            a.foreach(aa => result.redeem(Done(aa, e)))
            next.redeem(Done(a, e))
            Done(a, e)
          }
          case Step.Cont(k) => {
            next.redeem(current)
            Cont(step(ref))
          }
          case Step.Error(msg, e) => {
            result.redeem(Error(msg, e))
            next.redeem(Error(msg, e))
            Error(msg, e)

          }
        }

      }

      def step(ref: Ref[Iteratee[E, Option[A]]])(in: Input[E]): Iteratee[E, Option[A]] = {
        val next = Promise[Iteratee[E, Option[A]]]()
        val current = ref.single.swap(Iteratee.flatten(next.future))
        current.pureFlatFold {
          case Step.Done(a, e) => {
            next.redeem(Done(a, e))
            Done(a, e)
          }
          case Step.Cont(k) => {
            val n = k(in)
            next.redeem(n)
            n.pureFlatFold {
              case Step.Done(a, e) => {
                a.foreach(aa => result.redeem(Done(aa, e)))
                Done(a, e)
              }
              case Step.Cont(k) => Cont(step(ref))
              case Step.Error(msg, e) => {
                result.redeem(Error(msg, e))
                Error(msg, e)
              }
            }
          }
          case Step.Error(msg, e) => {
            next.redeem(Error(msg, e))
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
