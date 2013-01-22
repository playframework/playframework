package play.api.libs.iteratee

import org.specs2.mutable._
import scala.concurrent.ExecutionContext.Implicits.global
import concurrent._
import concurrent.duration.Duration
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
      val slowIteratee = Iteratee.foldM(List[Long]()){ (s,e:Long) => timeout(s :+ e, Duration(100, MILLISECONDS)) }
      val fastEnumerator = Enumerator[Long](1,2,3,4,5,6,7,8,9,10)
      val result = 
        fastEnumerator &>
        Enumeratee.scanLeft((now,0L)){ case ((s,v),_) => val ms = now;  (ms,(ms - s)) } &>
        Enumeratee.map(_._2) &>
        Concurrent.buffer(20) |>>>
        slowIteratee

      Await.result(result, Duration.Inf).max must beLessThan (1000L)
    }

    "throw an exception when buffer is full" in {
      val p = Promise[List[Long]]()
      val stuckIteratee = Iteratee.foldM(List[Long]()){ (s,e:Long) => p.future }
      val fastEnumerator = Enumerator[Long](1,2,3,4,5,6,7,8,9,10)
      val result = 
        fastEnumerator &>
        Concurrent.buffer(7) |>>>
        stuckIteratee

      Await.result(result, Duration.Inf) must throwAn[Exception]("buffer overflow")
    }

    "drop intermediate unused input, swallow even the unused eof forcing u to pass it twice" in {
      val p = Promise[List[Long]]()
      val slowIteratee = Iteratee.flatten(timeout(Cont[Long,List[Long]]{case Input.El(e) => Done(List(e),Input.Empty)}, Duration(100, MILLISECONDS)))
      val fastEnumerator = Enumerator[Long](1,2,3,4,5,6,7,8,9,10) >>> Enumerator.eof
      val result = 
        fastEnumerator |>>>
        (Concurrent.buffer(20) &>>
        slowIteratee).flatMap( l => Iteratee.getChunks.map(l ++ _))

      Await.result(result, Duration.Inf) must not equalTo (List(1,2,3,4,5,6,7,8,9,10))
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
      val a = "FOO"
      val b = "bar"
      val enumerator = Concurrent.unicast[String] { c =>
        c.push(a)
        c.push(b)
        c.eofAndEnd()
      }
      val promise = (enumerator |>> Iteratee.fold[String, String]("")(_ ++ _)).flatMap(_.run)

      Await.result(promise, Duration.Inf) must equalTo (a + b)
    }

    "call the onComplete callback when the iteratee is done" in {
      val completed = Promise[String]

      val enumerator = Concurrent.unicast[String](onStart = { c =>
        c.push("foo")
        c.push("bar")
      }, onComplete = {
        completed.success("called")
      })

      val future = enumerator |>>> Cont {
        case Input.El(data) => Done(data)
        case _ => Done("didn't get data")
      }

      Await.result(future, Duration.Inf) must_== "foo"
      Await.result(completed.future, Duration.Inf) must_== "called"
    }

    "call the onError callback when the iteratee encounters an error" in {
      val error = Promise[String]

      val enumerator = Concurrent.unicast[String](onStart = { c =>
        c.push("foo")
        c.push("bar")
      }, onError = { (err, input) =>
        error.success(err)
      })

      enumerator |>> Cont {
        case Input.El(data) => Error(data, Input.Empty)
        case in => Error("didn't get data", in)
      }

      Await.result(error.future, Duration.Inf) must_== "foo"
    }
  }

}
