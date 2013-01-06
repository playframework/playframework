package play.api.libs.iteratee

import org.specs2.mutable._

object EnumeratorsSpec extends Specification {


"Enumerator's interleave" should {

  "mix it with another enumerator into one" in {
      import play.api.libs.concurrent.Promise
      val e1 = Enumerator(List(1),List(3),List(5),List(7)) >>> Enumerator.enumInput(Input.EOF)
      val e2 = Enumerator(List(2),List(4),List(6),List(8))  >>> Enumerator.enumInput(Input.EOF)
      val p = play.api.libs.concurrent.Promise[List[Int]]()
      val e = e1 interleave e2
      val kk =e(Iteratee.fold1(p)((p,e) => Promise.pure(p ++ e))).flatMap(_.run)
      p.redeem(List())
      val result = kk.value.get
      println("interleaved enumerators result is: "+result)
      result.diff(Seq(1,2,3,4,5,6,7,8)) must equalTo(Seq())
    }

  "yield when both enumerators EOF" in {
      import play.api.libs.concurrent.Promise
      val e1 = Enumerator(List(1),List(3),List(5),List(7)) >>> Enumerator.enumInput(Input.EOF)
      val e2 = Enumerator(List(2),List(4),List(6),List(8))  >>> Enumerator.enumInput(Input.EOF)
      val p = play.api.libs.concurrent.Promise[List[Int]]()
      val e = e1 interleave e2
      val kk =e(Iteratee.fold1(p)((p,e) => Promise.pure(p ++ e))).flatMap(_.run)
      p.redeem(List())
      val result = kk.value.get
      result.diff(Seq(1,2,3,4,5,6,7,8)) must equalTo(Seq())
    }

  "yield when iteratee is done!" in {
      import play.api.libs.concurrent.Promise
      val e1 = Enumerator(List(1),List(3),List(5),List(7))
      val e2 = Enumerator(List(2),List(4),List(6),List(8))
      val p = play.api.libs.concurrent.Promise[List[Int]]()
      val e = e1 interleave e2
      val kk = (e |>> Enumeratee.take(7) &>> Iteratee.fold1(p)((p,e) => Promise.pure(p ++ e))).flatMap(_.run)
      p.redeem(List())
      val result = kk.value.get
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
    var pp:Concurrent.PatchPanel[Int] = null
    val e = Concurrent.patchPanel[Int](p => pp = p)
    val i1 = Iteratee.fold[Int,Int](0){(s,i) => println(i);s+i}
    val sum = e |>> i1
    pp.patchIn(Enumerator(1,2,3,4))
    pp.patchIn(Enumerator.eof)
    sum.flatMap(_.run).value.get must equalTo(10)

  }

}*/
}

