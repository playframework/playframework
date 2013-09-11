package play.api.libs.iteratee

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit._
import java.util.concurrent.atomic.AtomicInteger
import org.specs2.mutable._
import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

object ConcurrentSpec extends Specification
  with IterateeSpecification with ExecutionSpecification {

  val timer = new java.util.Timer
  def timeout[A](a: => A, d: Duration)(implicit e: ExecutionContext): Future[A] = {
    val p = Promise[A]()
    timer.schedule(new java.util.TimerTask {
      def run() {
        p.success(a)
      }
    }, d.toMillis)
    p.future
  }

  "Concurrent.broadcast (0-arg)" should {
    "broadcast the same to already registered iteratees" in {
      mustExecute(38) { foldEC =>
        val (broadcaster, pushHere) = Concurrent.broadcast[String]
        val results = Future.sequence(Range(1, 20).map(_ => Iteratee.fold[String, String]("") { (s, e) => s + e }(foldEC)).map(broadcaster.apply).map(_.flatMap(_.run)))
        pushHere.push("beep")
        pushHere.push("beep")
        pushHere.eofAndEnd()
        Await.result(results, Duration.Inf) must equalTo(Range(1, 20).map(_ => "beepbeep"))
      }
    }
  }

  "Concurrent.buffer" should {

    def now = System.currentTimeMillis()

    "not slow down the enumerator if the iteratee is slow" in {
      mustExecute(10, 10, 10) { (foldEC, mapEC, bufferEC) =>
        val slowIteratee = Iteratee.foldM(List[Long]()) { (s, e: Long) => timeout(s :+ e, Duration(100, MILLISECONDS)) }(foldEC)
        val fastEnumerator = Enumerator[Long](1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        val result =
          fastEnumerator &>
            Enumeratee.scanLeft((now, 0L)) { case ((s, v), _) => val ms = now; (ms, (ms - s)) } &>
            Enumeratee.map((_: (Long, Long))._2)(mapEC) &>
            Concurrent.buffer(20, (_: Input[Long]) => 1)(bufferEC) |>>>
            slowIteratee

        Await.result(result, Duration.Inf).max must beLessThan(1000L)
      }
    }

    "throw an exception when buffer is full" in {
      testExecution { foldEC =>
        val foldCount = new AtomicInteger()
        val p = Promise[List[Long]]()
        val stuckIteratee = Iteratee.foldM(List[Long]()) { (s, e: Long) => foldCount.incrementAndGet(); p.future }(foldEC)
        val fastEnumerator = Enumerator[Long](1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        val result =
          fastEnumerator &>
            Concurrent.buffer(7) |>>>
            stuckIteratee

        Await.result(result, Duration.Inf) must throwAn[Exception]("buffer overflow")
        foldEC.executionCount must equalTo(foldCount.get())
      }
    }

    "drop intermediate unused input, swallow even the unused eof forcing u to pass it twice" in {
      testExecution { (flatMapEC, mapEC) =>
        val p = Promise[List[Long]]()
        val slowIteratee = Iteratee.flatten(timeout(Cont[Long, List[Long]] { case Input.El(e) => Done(List(e), Input.Empty) }, Duration(100, MILLISECONDS)))
        val fastEnumerator = Enumerator[Long](1, 2, 3, 4, 5, 6, 7, 8, 9, 10) >>> Enumerator.eof
        val preparedMapEC = mapEC.prepare()
        val result =
          fastEnumerator |>>>
            (Concurrent.buffer(20) &>>
              slowIteratee).flatMap { l => println(l); Iteratee.getChunks.map(l ++ (_: List[Long]))(preparedMapEC) }(flatMapEC)

        Await.result(result, Duration.Inf) must not equalTo (List(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
        flatMapEC.executionCount must beGreaterThan(0)
        mapEC.executionCount must equalTo(flatMapEC.executionCount)
      }
    }

  }

  "Concurrent.lazyAndErrIfNotReady" should {

    "return an error if the iteratee is taking too long" in {

      val slowIteratee = Iteratee.flatten(timeout(Cont[Long,List[Long]]{case _ => Done(List(1),Input.Empty)}, Duration(1000, MILLISECONDS)))
      val fastEnumerator = Enumerator[Long](1,2,3,4,5,6,7,8,9,10) >>> Enumerator.eof
      val result = (fastEnumerator &> Concurrent.lazyAndErrIfNotReady(50) |>>> slowIteratee)

      Await.result(result, Duration.Inf) must throwA[Exception]("iteratee is taking too long")
    }

  }

  "Concurrent.unicast" should {
    "allow to push messages and end" in {
      mustExecute(2, 2) { (unicastEC, foldEC) =>
        val a = "FOO"
        val b = "bar"
        val startCount = new AtomicInteger()
        val completeCount = new AtomicInteger()
        val errorCount = new AtomicInteger()
        val enumerator = Concurrent.unicast[String](
          c => {
            startCount.incrementAndGet()
            c.push(a)
            c.push(b)
            c.eofAndEnd()
          },
          () => completeCount.incrementAndGet(),
          (_: String, _: Input[String]) => errorCount.incrementAndGet())(unicastEC)
        val promise = (enumerator |>> Iteratee.fold[String, String]("")(_ ++ _)(foldEC)).flatMap(_.run)

        Await.result(promise, Duration.Inf) must equalTo(a + b)
        startCount.get() must equalTo(1)
        completeCount.get() must equalTo(0)
        errorCount.get() must equalTo(0)
      }
    }

    "call the onComplete callback when the iteratee is done" in {
      mustExecute(2) { unicastEC =>
        val completed = Promise[String]

        val enumerator = Concurrent.unicast[String](onStart = { c =>
          c.push("foo")
          c.push("bar")
        }, onComplete = {
          completed.success("called")
        })(unicastEC)

        val future = enumerator |>>> Cont {
          case Input.El(data) => Done(data)
          case _ => Done("didn't get data")
        }

        Await.result(future, Duration.Inf) must_== "foo"
        Await.result(completed.future, Duration.Inf) must_== "called"
      }
    }

    "call the onError callback when the iteratee encounters an error" in {
      mustExecute(2) { unicastEC =>
        val error = Promise[String]

        val enumerator = Concurrent.unicast[String](onStart = { c =>
          c.push("foo")
          c.push("bar")
        }, onError = { (err, input) =>
          error.success(err)
        })(unicastEC)

        enumerator |>> Cont {
          case Input.El(data) => Error(data, Input.Empty)
          case in => Error("didn't get data", in)
        }

        Await.result(error.future, Duration.Inf) must_== "foo"
      }
    }
  }

  "Concurrent.broadcast (2-arg)" should {
    "call callback in the correct ExecutionContext" in {
      mustExecute(1) { callbackEC =>
        val (e0, c) = Concurrent.broadcast[Int]
        val interestCount = new AtomicInteger()
        val interestDone = new CountDownLatch(1)
        val (e2, b) = Concurrent.broadcast(e0, { f =>
          interestCount.incrementAndGet()
          interestDone.countDown()
          })(callbackEC)
        val i = e2 |>>> Iteratee.getChunks[Int]
        c.push(1)
        c.push(2)
        c.push(3)
        c.eofAndEnd()
        Await.result(i, Duration.Inf) must equalTo(List(1, 2, 3))
        interestDone.await(30, SECONDS) must beTrue
        interestCount.get() must equalTo(1)
      }
    }
  } 
  
  "Concurrent.patchPanel" should {

    "perform patching in the correct ExecutionContext" in {
      mustExecute(1) { ppEC =>
        val e = Concurrent.patchPanel[Int] { pp =>
          pp.patchIn(Enumerator.eof)
        }(ppEC)
        Await.result(e |>>> Iteratee.getChunks[Int], Duration.Inf) must equalTo(Nil)
      }
    }
  }

  "Concurrent.joined" should {
    "join the iteratee and enumerator if the enumerator is applied first" in {
      val (iteratee, enumerator) = Concurrent.joined[String]
      val result = enumerator |>>> Iteratee.getChunks[String]
      val unitResult = Enumerator("foo", "bar") |>>> iteratee
      await(result) must_== Seq("foo", "bar")
      await(unitResult) must_== ()
    }
    "join the iteratee and enumerator if the iteratee is applied first" in {
      val (iteratee, enumerator) = Concurrent.joined[String]
      val unitResult = Enumerator("foo", "bar") |>>> iteratee
      val result = enumerator |>>> Iteratee.getChunks[String]
      await(result) must_== Seq("foo", "bar")
      await(unitResult) must_== ()
    }
    "join the iteratee and enumerator if the enumerator is applied during the iteratees run" in {
      val (iteratee, enumerator) = Concurrent.joined[String]
      val (broadcast, channel) = Concurrent.broadcast[String]
      val unitResult = broadcast |>>> iteratee
      channel.push("foo")
      Thread.sleep(10)
      val result = enumerator |>>> Iteratee.getChunks[String]
      channel.push("bar")
      channel.end()
      await(result) must_== Seq("foo", "bar")
      await(unitResult) must_== ()
    }
    "break early from infinite enumerators" in {
      val (iteratee, enumerator) = Concurrent.joined[String]
      val infinite = Enumerator.repeat("foo")
      val unitResult = infinite |>>> iteratee
      val head = enumerator |>>> Iteratee.head
      await(head) must beSome("foo")
      await(unitResult) must_== ()
    }
  }

  "Concurrent.runPartial" should {
    "redeem the iteratee with the result and the partial enumerator" in {
      val (a, remaining) = await(Concurrent.runPartial(Enumerator("foo", "bar"), Iteratee.head[String]))
      a must beSome("foo")
      await(remaining |>>> Iteratee.getChunks[String]) must_== Seq("bar")
    }
    "work when there is no input left in the enumerator" in {
      val (a, remaining) = await(Concurrent.runPartial(Enumerator("foo", "bar"), Iteratee.getChunks[String]))
      a must_== Seq("foo", "bar")
      await(remaining |>>> Iteratee.getChunks[String]) must_== Nil
    }
  }
}
