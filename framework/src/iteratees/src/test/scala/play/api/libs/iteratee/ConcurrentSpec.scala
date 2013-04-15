package play.api.libs.iteratee

import org.specs2.mutable._
import scala.concurrent.ExecutionContext.Implicits.global
import concurrent._
import concurrent.duration.Duration
import scala.language.reflectiveCalls
import java.util.concurrent.TimeUnit._

object ConcurrentSpec extends Specification {

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

  "Concurrent.buffer" should {

    def now = System.currentTimeMillis()

    "not slow down the enumerator if the iteratee is slow" in {
      val foldMEC = TestExecutionContext()
      val slowIteratee = Iteratee.foldM(List[Long]()){ (s,e:Long) => timeout(s :+ e, Duration(100, MILLISECONDS)) }(foldMEC)
      val fastEnumerator = Enumerator[Long](1,2,3,4,5,6,7,8,9,10)
      val result = 
        fastEnumerator &>
        Enumeratee.scanLeft((now,0L)){ case ((s,v),_) => val ms = now;  (ms,(ms - s)) } &>
        Enumeratee.map(_._2) &>
        Concurrent.buffer(20) |>>>
        slowIteratee

      Await.result(result, Duration.Inf).max must beLessThan (1000L)
      foldMEC.executionCount must equalTo(10)
      
    }

    "throw an exception when buffer is full" in {
      val foldMEC = TestExecutionContext()
      val foldCount = new java.util.concurrent.atomic.AtomicInteger()
      val p = Promise[List[Long]]()
      val stuckIteratee = Iteratee.foldM(List[Long]()){ (s,e:Long) => foldCount.incrementAndGet(); p.future }(foldMEC)
      val fastEnumerator = Enumerator[Long](1,2,3,4,5,6,7,8,9,10)
      val result = 
        fastEnumerator &>
        Concurrent.buffer(7) |>>>
        stuckIteratee

      Await.result(result, Duration.Inf) must throwAn[Exception]("buffer overflow")
      foldMEC.executionCount must equalTo(foldCount.get())
    }

    "drop intermediate unused input, swallow even the unused eof forcing u to pass it twice" in {
      val p = Promise[List[Long]]()
      val slowIteratee = Iteratee.flatten(timeout(Cont[Long,List[Long]]{case Input.El(e) => Done(List(e),Input.Empty)}, Duration(100, MILLISECONDS)))
      val fastEnumerator = Enumerator[Long](1,2,3,4,5,6,7,8,9,10) >>> Enumerator.eof
      val flatMapEC = TestExecutionContext()
      val mapEC = TestExecutionContext()
      val result = 
        fastEnumerator |>>>
        (Concurrent.buffer(20) &>>
        slowIteratee).flatMap{ l => println(l); Iteratee.getChunks.map(l ++ (_: List[Long]))(mapEC)}(flatMapEC)

      Await.result(result, Duration.Inf) must not equalTo (List(1,2,3,4,5,6,7,8,9,10))
      flatMapEC.executionCount must beGreaterThan(0)
      mapEC.executionCount must equalTo(flatMapEC.executionCount)
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
      val unicastEC = TestExecutionContext()
      val a = "FOO"
      val b = "bar"
      val enumerator = Concurrent.unicast[String] { c =>
        c.push(a)
        c.push(b)
        c.eofAndEnd()
      }(unicastEC)
      val foldEC = TestExecutionContext()
      val promise = (enumerator |>> Iteratee.fold[String, String]("")(_ ++ _)(foldEC)).flatMap(_.run)

      Await.result(promise, Duration.Inf) must equalTo (a + b)
      unicastEC.executionCount must equalTo(2) // onStart/onComplete
      foldEC.executionCount must equalTo(2) // once for each string
    }

    "call the onComplete callback when the iteratee is done" in {
      val completed = Promise[String]
      val unicastEC = TestExecutionContext()

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
      unicastEC.executionCount must equalTo(2) // onStart/onComplete
    }

    "call the onError callback when the iteratee encounters an error" in {
      val error = Promise[String]
      val unicastEC = TestExecutionContext()

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
      unicastEC.executionCount must equalTo(2) // onStart/onError
    }
  }

}
