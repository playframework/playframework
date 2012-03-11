package play.api.libs.iteratee

import play.api.libs.concurrent._

object Concurrent {

  def dropInputIfNotReady[E](duration: Long, unit:  java.util.concurrent.TimeUnit =  java.util.concurrent.TimeUnit.MILLISECONDS): Enumeratee[E,E] = new Enumeratee[E,E] {

    def applyOn[A](it: Iteratee[E,A]): Iteratee[E,Iteratee[E,A]] = {

      def step(inner: Iteratee[E,A])(in:Input[E]):Iteratee[E,Iteratee[E,A]] = {

        in match {
          case Input.EOF =>
            Done(inner,Input.Empty)

          case in => 
            val readyOrNot: Promise[Either[Iteratee[E,Iteratee[E,A]],Unit]] =  inner.pureFold[Iteratee[E,Iteratee[E,A]]](
              (a,e) => Done(Done(a,e),Input.Empty),
              k => Cont{ in => 
                val next = k(in)
                Cont(step(next))
                      },
              (msg,e) => Done(Error(msg,e),Input.Empty)).orTimeout((),duration,unit)

            Iteratee.flatten( readyOrNot.map {
              case Left(ready) => Iteratee.flatten(ready.feed(in))
              case Right(_) => Cont(step(inner))
            })
        }
      }

      Cont(step(it))
    }
  }

  trait Hub[E] {

    def getPatchCord(): Enumerator[E]

    def noCords(): Boolean

    def close()

    def closed():Boolean
  }



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
          t._1.fold(
            (a, e) => {
              p.redeem(Done(a, e))
              commitDone.single.transform(_ :+ index)
              Promise.pure(())
            },
            k => {
              val next = k(in)
              next.pureFold(
                (a, e) => {
                  p.redeem(Done(a, e))
                  commitDone.single.transform(_ :+ index)
                },
                k => commitReady.single.transform(_ :+ (index, (Cont(k), p))),
                (msg, e) => {
                  p.redeem(Error(msg, e))
                  commitDone.single.transform(_ :+ index)
                })
            },
            (msg, e) => {
              p.redeem(Error(msg, e))
              commitDone.single.transform(_ :+ index)
              Promise.pure(())
            }).extend1 {
              case Redeemed(a) => a
              case Thrown(e) => p.throwing(e)

            }
      }.fold(Promise.pure()) { (s, p) => s.flatMap(_ => p) }

      Iteratee.flatten(ready.map { _ =>

        val (hanging, downToZero) = atomic { implicit txn =>
          val responsive = (commitReady().map(_._1) ++ commitDone()).toSet
          val hangs = interested.zipWithIndex.collect { case (e, i) if !responsive.contains(i) => e }
          //send EOF to hanging
          val ready = commitReady().toMap
          iteratees.transform(commitReady().map(_._2) ++ _)
          (hangs, (interested.length > 0 && iteratees().length <= 0))

        }
        hanging.map {
          case (h, p) => h.feed(Input.EOF).extend1 {
            case Redeemed(it) => p.redeem(it)
            case Thrown(e) => p.redeem(throw e)

          }
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

      val redeemed = Ref(Waiting : PromiseValue[Iteratee[E,Unit]])
      def getPatchCord() = new Enumerator[E] {

        def apply[A](it: Iteratee[E, A]): Promise[Iteratee[E, A]] = {
          val result = Promise[Iteratee[E, A]]()
          val alreadyStarted = ! started.single.compareAndSet(false, true)
          if(!alreadyStarted) {
            val promise = (e |>> Cont(step))
            promise.extend1{ v =>
              val its = atomic{ implicit txn =>
                redeemed() = v
                iteratees.swap(List())
              }
              v match {
              case Thrown(e) => 
                its.foreach{ case (_,p) => p.throwing(e) }
                
              case Redeemed(_) =>
                its.foreach{ case (it,p) => p.redeem(it)}
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
          finished.foreach{
            case Redeemed(_) => result.redeem(it)
            case Thrown(e) => result.throwing(e)
          }
          result
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

      result.onRedeem(_ => isClosed = true);

      def refIteratee(ref: Ref[Iteratee[E, Option[A]]]): Iteratee[E, Option[A]] = {
        val next = Promise[Iteratee[E, Option[A]]]()
        val current = ref.single.swap(Iteratee.flatten(next))
        current.pureFlatFold(
          (a, e) => {
            a.foreach(aa => result.redeem(Done(aa, e)))
            next.redeem(Done(a, e))
            Done(a, e)
          },
          k => {
            next.redeem(current)
            Cont(step(ref))
          },
          (msg, e) => {
            result.redeem(Error(msg, e))
            next.redeem(Error(msg, e))
            Error(msg, e)

          })

      }

      def step(ref: Ref[Iteratee[E, Option[A]]])(in: Input[E]): Iteratee[E, Option[A]] = {
        val next = Promise[Iteratee[E, Option[A]]]()
        val current = ref.single.swap(Iteratee.flatten(next))
        current.pureFlatFold(
          (a, e) => {
            next.redeem(Done(a, e))
            Done(a, e)
          },
          k => {
            val n = k(in)
            next.redeem(n)
            n.pureFlatFold(
              (a, e) => {
                a.foreach(aa => result.redeem(Done(aa, e)))
                Done(a, e)
              },
              k => Cont(step(ref)),
              (msg, e) => {
                result.redeem(Error(msg, e))
                Error(msg, e)
              })
          },
          (msg, e) => {
            next.redeem(Error(msg, e))
            Error(msg, e)
          })
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

      result

    }
  }
  }
