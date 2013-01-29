package play.api.libs.iteratee

import org.specs2.mutable._

import scala.concurrent.ExecutionContext.Implicits.global
import concurrent.{Promise, Future, Await}
import concurrent.duration.Duration
import java.io.OutputStream

object EnumeratorsSpec extends Specification {


"Enumerator's interleave" should {

  "mix it with another enumerator into one" in {
      val e1 = Enumerator(List(1),List(3),List(5),List(7))
      val e2 = Enumerator(List(2),List(4),List(6),List(8))
      val e = e1 interleave e2
      val kk = e |>>> Iteratee.fold(List.empty[Int])((r, e) => r ++ e)
      val result = Await.result(kk, Duration.Inf)
      println("interleaved enumerators result is: "+result)
      result.diff(Seq(1,2,3,4,5,6,7,8)) must equalTo (Seq())
    }

  "yield when both enumerators EOF" in {
      val e1 = Enumerator(List(1),List(3),List(5),List(7)) >>> Enumerator.enumInput(Input.EOF)
      val e2 = Enumerator(List(2),List(4),List(6),List(8))  >>> Enumerator.enumInput(Input.EOF)
      val e = e1 interleave e2
      val kk = e |>>> Iteratee.fold(List.empty[Int])((r, e) => r ++ e)
      val result = Await.result(kk, Duration.Inf)
      result.diff(Seq(1,2,3,4,5,6,7,8)) must equalTo (Seq())
    }

  "yield when iteratee is done!" in {
      val e1 = Enumerator(List(1),List(3),List(5),List(7))
      val e2 = Enumerator(List(2),List(4),List(6),List(8))
      val e = e1 interleave e2
      val kk = e |>>> Enumeratee.take(7) &>> Iteratee.fold(List.empty[Int])((r, e) => r ++ e)
      val result = Await.result(kk, Duration.Inf)
      result.length must equalTo (7)
    }

  "not necessarily go alternatively between two enumerators" in {
    val firstDone = Promise[Unit]
    val e1 = Enumerator(1, 2, 3, 4).onDoneEnumerating(firstDone.success(Unit))
    val e2 = Enumerator.unfoldM[Boolean, Int](true) { first => if (first) firstDone.future.map(_ => Some(false, 5)) else Future.successful(None)}
    val result = Await.result((e1 interleave e2) |>>> Iteratee.getChunks[Int], Duration.Inf)
    result must_== Seq(1, 2, 3, 4, 5)
  }

}

"Enumerator.enumerate " should {
  "generate an Enumerator from a singleton Iterator" in {
    val iterator = scala.collection.Iterator.single[Int](3)
    val futureOfResult = Enumerator.enumerate(iterator) |>>> 
                         Enumeratee.take(1) &>> 
                         Iteratee.fold(List.empty[Int])((r, e) => e::r)
    val result = Await.result(futureOfResult, Duration.Inf)
    result(0) must equalTo(3)
    result.length must equalTo(1)
  }

  "take as much element as in the iterator in the right order" in {
    val iterator = scala.collection.Iterator.range(0, 50)
    val futureOfResult = Enumerator.enumerate(iterator) |>>> 
                         Enumeratee.take(100) &>> 
                         Iteratee.fold(Seq.empty[Int])((r, e) => r :+ e)
    val result = Await.result(futureOfResult, Duration.Inf)
    result.length must equalTo(50)
    result(0) must equalTo(0)
    result(49) must equalTo(49)
  }
  "work with Seq too" in {
    val seq = List(1, 2, 3, 7, 42, 666)
    val futureOfResult = Enumerator.enumerate(seq) |>>> 
                         Enumeratee.take(100) &>> 
                         Iteratee.fold(Seq.empty[Int])((r, e) => r :+ e)
    val result = Await.result(futureOfResult, Duration.Inf)
    result.length must equalTo(6)
    result(0) must equalTo(1)
    result(4) must equalTo(42)
  }
}

"Enumerator's Hub" should {

  "share Enumerator with different iteratees" in {
    var pp:Enumerator.Pushee[Int] = null
    val e = Enumerator.pushee[Int]((p => pp =p ))
    val hub = Concurrent.hub(e)
    val i1 = Iteratee.fold[Int,Int](0){(s,i) => println(i);s+i}
    val c = Iteratee.fold[Int,Int](0){(s,i) => s+1}
    val sum = hub.getPatchCord() |>>> i1
    pp.push(1)
    val count = hub.getPatchCord() |>>> c
    pp.push(1); pp.push(1); pp.push(1); pp.close()

    Await.result(sum, Duration.Inf) must equalTo (4)
    Await.result(count, Duration.Inf) must equalTo (3)

  }

}

/*"Enumerator's PatchPanel" should {

  "allow to patch in different Enumerators" in {
      import play.api.libs.concurrent.Promise
    val pp = Promise[Concurrent.PatchPanel[Int]]()
    val e = Concurrent.patchPanel[Int](p => pp.redeem(p))
    val i1 = Iteratee.fold[Int,Int](0){(s,i) => println(i);s+i}
    val sum = e |>> i1
    val p = pp.future.await.get
    p.patchIn(Enumerator(1,2,3,4))
    p.patchIn(Enumerator.eof)
    sum.flatMap(_.run).value1.get must equalTo(10)
  }

}*/

"Enumerator" should {
  "be transformed to another Enumerator using flatMap" in {
    val e = for {
      i <- Enumerator(10, 20, 30)
      j <- Enumerator((i until i + 10): _*)
    } yield j
    val it = Iteratee.fold[Int, Int](0)((sum, x) => sum + x)
    Await.result(e |>>> it, Duration.Inf) must equalTo ((10 until 40).sum)
  }
}

"Enumerator.generateM" should {
  "generate a stream of values until the expression is None" in {

    val a = (0 to 10).toList
    val it = a.iterator

    val enumerator = Enumerator.generateM(Future(if(it.hasNext) Some(it.next()) else None))

    Await.result(enumerator |>>> Iteratee.fold[Int,String]("")(_ + _), Duration.Inf) must equalTo ("012345678910")

  }

}

"Enumerator.generateM" should {
  "Can be composed with another enumerator (doesn't send EOF)" in {

    val a = (0 to 10).toList
    val it = a.iterator

    val enumerator = Enumerator.generateM(Future(if(it.hasNext) Some(it.next()) else None)) >>> Enumerator(12)

    Await.result(enumerator |>>> Iteratee.fold[Int,String]("")(_ + _), Duration.Inf) must equalTo ("01234567891012")

  }

}

"Enumerator.unfoldM" should {
  "Can be composed with another enumerator (doesn't send EOF)" in {

    val enumerator = Enumerator.unfoldM[Int,Int](0)( s => Future(if(s > 10) None else Some((s+1,s+1)))) >>> Enumerator(12)

    Await.result(enumerator |>>> Iteratee.fold[Int,String]("")(_ + _), Duration.Inf) must equalTo ("123456789101112")

  }

}

"Enumerator.broadcast" should {
  "broadcast the same to already registered iteratees" in {

    val (broadcaster,pushHere) = Concurrent.broadcast[String]
    val results = Future.sequence(Range(1,20).map(_ => Iteratee.fold[String,String](""){(s,e) => s + e }).map(broadcaster.apply).map(_.flatMap(_.run)))
    pushHere.push("beep")
    pushHere.push("beep")
    pushHere.eofAndEnd()
    Await.result(results, Duration.Inf) must equalTo (Range(1,20).map(_ => "beepbeep"))

  }
}

"Enumerator.outputStream" should {
  "produce the same value written in the OutputStream" in {
    val a = "FOO"
    val b = "bar"
    val enumerator = Enumerator.outputStream { outputStream =>
      outputStream.write(a.toArray.map(_.toByte))
      outputStream.write(b.toArray.map(_.toByte))
      outputStream.close()
    }
    val promise = (enumerator |>>> Iteratee.fold[Array[Byte],Array[Byte]](Array[Byte]())(_ ++ _))

    Await.result(promise, Duration.Inf).map(_.toChar).foldLeft("")(_+_) must equalTo (a+b)
  }

  "not block" in {
    var os: OutputStream = null
    val enumerator = Enumerator.outputStream(o => os = o)
    val promiseIteratee = Promise[Iteratee[Array[Byte], Array[Byte]]]
    val future = enumerator |>>> Iteratee.flatten(promiseIteratee.future)
    // os should now be set
    os.write("hello".getBytes)
    os.write(" ".getBytes)
    os.write("world".getBytes)
    os.close()
    promiseIteratee.success(Iteratee.consume[Array[Byte]]())
    Await.result(future, Duration("10s")) must_== "hello world".getBytes
  }
}



}

