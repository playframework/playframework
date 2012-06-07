package play.api.libs.iteratee

import org.specs2.mutable._

import play.api.libs.concurrent._

import play.api.libs.concurrent.execution.defaultContext

object EnumeratorsSpec extends Specification {


"Enumerator's interleave" should {

  "mix it with another enumerator into one" in {
      import play.api.libs.concurrent.Promise
      val e1 = Enumerator(List(1),List(3),List(5),List(7))
      val e2 = Enumerator(List(2),List(4),List(6),List(8))
      val p = play.api.libs.concurrent.Promise[List[Int]]()
      val e = e1 interleave e2
      val kk =e(Iteratee.fold1(p.future)((p,e) => Promise.pure(p ++ e))).flatMap(_.run)
      p.redeem(List())
      val result = kk.await.get
      println("interleaved enumerators result is: "+result)
      result.diff(Seq(1,2,3,4,5,6,7,8)) must equalTo(Seq())
    }

  "yield when both enumerators EOF" in {
      import play.api.libs.concurrent.Promise
      val e1 = Enumerator(List(1),List(3),List(5),List(7)) >>> Enumerator.enumInput(Input.EOF)
      val e2 = Enumerator(List(2),List(4),List(6),List(8))  >>> Enumerator.enumInput(Input.EOF)
      val p = play.api.libs.concurrent.Promise[List[Int]]()
      val e = e1 interleave e2
      val kk =e(Iteratee.fold1(p.future)((p,e) => Promise.pure(p ++ e))).flatMap(_.run)
      p.redeem(List())
      val result = kk.await.get
      result.diff(Seq(1,2,3,4,5,6,7,8)) must equalTo(Seq())
    }

  "yield when iteratee is done!" in {
      import play.api.libs.concurrent.Promise
      val e1 = Enumerator(List(1),List(3),List(5),List(7))
      val e2 = Enumerator(List(2),List(4),List(6),List(8))
      val p = play.api.libs.concurrent.Promise[List[Int]]()
      val e = e1 interleave e2
      val kk = (e |>> Enumeratee.take(7) &>> Iteratee.fold1(p.future)((p,e) => Promise.pure(p ++ e))).flatMap(_.run)
      p.redeem(List())
      val result = kk.await.get
      result.length must equalTo(7)
    }

}

"Enumerator's Hub" should {

  "share Enumerator with different iteratees" in {
      import play.api.libs.concurrent.Promise
    var pp:Enumerator.Pushee[Int] = null
    val e = Enumerator.pushee[Int]((p => pp =p ))
    val hub = Concurrent.hub(e)
    val i1 = Iteratee.fold[Int,Int](0){(s,i) => println(i);s+i}
    val c = Iteratee.fold[Int,Int](0){(s,i) => s+1}
    val sum = hub.getPatchCord() |>> i1
    pp.push(1);
    val count = hub.getPatchCord() |>> c
  pp.push(1); pp.push(1); pp.push(1); pp.close()

    sum.flatMap(_.run).value.get must equalTo(4)
    count.flatMap(_.run).value.get must equalTo(3)

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
    sum.flatMap(_.run).value.get must equalTo(10)
  }

}*/

"Enumerator" should {
  "be transformed to another Enumerator using flatMap" in {
    val e = for {
      i <- Enumerator(10, 20, 30)
      j <- Enumerator((i until i + 10): _*)
    } yield j
    val it = Iteratee.fold[Int, Int](0)((sum, x) => sum + x)
    (e |>> it).flatMap(_.run).value.get must equalTo ((10 until 40).sum)
  }
}

"Enumerator.generateM" should {
  "generate a stream of values until the expression is None" in {

    val a = 0 to 10 toList
    val it = a.iterator

    val enumerator = Enumerator.generateM( play.api.libs.concurrent.Promise.pure(if(it.hasNext) Some(it.next) else None))

    (enumerator |>> Iteratee.fold[Int,String]("")(_ + _)).flatMap(_.run).value.get must equalTo("012345678910")

  }

}

"Enumerator.generateM" should {
  "Can be composed with another enumerator (doesn't send EOF)" in {

    val a = 0 to 10 toList
    val it = a.iterator

    val enumerator = Enumerator.generateM( play.api.libs.concurrent.Promise.pure(if(it.hasNext) Some(it.next) else None)) >>> Enumerator(12)

    (enumerator |>> Iteratee.fold[Int,String]("")(_ + _)).flatMap(_.run).value.get must equalTo("01234567891012")

  }

}

"Enumerator.unfoldM" should {
  "Can be composed with another enumerator (doesn't send EOF)" in {

    val enumerator = Enumerator.unfoldM[Int,Int](0)( s => play.api.libs.concurrent.Promise.pure(if(s > 10) None else Some((s+1,s+1)))) >>> Enumerator(12)

    (enumerator |>> Iteratee.fold[Int,String]("")(_ + _)).flatMap(_.run).value.get must equalTo("123456789101112")

  }

}

"Enumerator.broadcast" should {
  "broadcast the same to already registered iteratees" in {

    val (broadcaster,pushHere) = Concurrent.broadcast[String]
    val results = play.api.libs.concurrent.Promise.sequence(Range(1,20).map(_ => Iteratee.fold[String,String](""){(s,e) => s + e }).map(broadcaster.apply).map(_.flatMap(_.run)))
    pushHere.push("beep")
    pushHere.push("beep")
    pushHere.eofAndEnd()
    results.value.get must equalTo(Range(1,20).map(_ => "beepbeep"))

  }
}
}

