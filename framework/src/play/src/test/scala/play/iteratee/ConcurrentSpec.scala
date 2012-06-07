package play.api.libs.iteratee

import org.specs2.mutable._
import play.api.libs.concurrent.Promise
import play.api.libs.concurrent._

object ConcurrentSpec extends Specification {

  "Concurrent.buffer" should {

    def now = System.currentTimeMillis()

    "not slow down the enumerator if the iteratee is slow" in {
      Promise.timeout((),1)
      val slowIteratee = Iteratee.foldM(List[Long]()){ (s,e:Long) => Promise.timeout(s :+ e, 100) }
      val fastEnumerator = Enumerator[Long](1,2,3,4,5,6,7,8,9,10)
      val result = 
        fastEnumerator &>
        Enumeratee.scanLeft((now,0L)){ case ((s,v),_) => val ms = now;  (ms,(ms - s)) } &>
        Enumeratee.map(_._2) &>
        Concurrent.buffer(20) |>>>
        slowIteratee

      result.value.get.max must beLessThan(1000L)
    }

    "throw an exception when buffer is full" in {
      val p = Promise[List[Long]]()
      val stuckIteratee = Iteratee.foldM(List[Long]()){ (s,e:Long) => p.future }
      val fastEnumerator = Enumerator[Long](1,2,3,4,5,6,7,8,9,10)
      val result = 
        fastEnumerator &>
        Concurrent.buffer(7) |>>>
        stuckIteratee

      result.await.get must throwAn[Exception]("buffer overflow")
    }

    "drop intermediate unused input, swallow even the unused eof forcing u to pass it twice" in {
      val p = Promise[List[Long]]()
      val slowIteratee = Iteratee.flatten(Promise.timeout(Cont[Long,List[Long]]{case Input.El(e) => Done(List(e),Input.Empty)},100))
      val fastEnumerator = Enumerator[Long](1,2,3,4,5,6,7,8,9,10) >>> Enumerator.eof
      val result = 
        fastEnumerator |>>>
        (Concurrent.buffer(20) &>>
        slowIteratee).flatMap( l => Iteratee.getChunks.map(l ++ _))

      result.await.get must not equalTo(List(1,2,3,4,5,6,7,8,9,10))
    }

  }

  "Concurrent.lazyAndErrIfNotReady" should {

    "return an error if the iteratee is taking too long" in {

      val slowIteratee = Iteratee.flatten(Promise.timeout(Cont[Long,List[Long]]{case _ => Done(List(1),Input.Empty)},1000))
      val fastEnumerator = Enumerator[Long](1,2,3,4,5,6,7,8,9,10) >>> Enumerator.eof
      (fastEnumerator &> Concurrent.lazyAndErrIfNotReady(50) |>>> slowIteratee).await.get must throwA[Exception]("iteratee is taking too long")

    }

  }

}
