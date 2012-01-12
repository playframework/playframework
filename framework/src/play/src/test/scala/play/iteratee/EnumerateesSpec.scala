package play.api.libs.iteratee

import org.specs2.mutable._

object EnumerateesSpec extends Specification {

  "Enumeratee.drop" should {

    "ignore 3 chunkes when applied with 3" in {
      
      val drop3AndConsume = Enumeratee.drop[String](3) &>>  Iteratee.consume[String]()
      val enumerator = Enumerator(Range(1,20).map(_.toString) :_*)  
      (enumerator |>> drop3AndConsume).flatMap(_.run).value.get must equalTo(Range(4,20).map(_.toString).mkString)

    }

  }

  "Enumeratee.take" should {

    "pass only first 3 chunkes to Iteratee when applied with 3" in {
      
      val take3AndConsume = Enumeratee.take[String](3) &>>  Iteratee.consume()
      val enumerator = Enumerator(Range(1,20).map(_.toString) :_*)  
      (enumerator |>> take3AndConsume).flatMap(_.run).value.get must equalTo(List(1,2,3).map(_.toString).mkString)

    }

    "passes along what's left of chunks after taking 3" in {
      
      val take3AndConsume = (Enumeratee.take[String](3) &>>  Iteratee.consume()).flatMap(_ => Iteratee.consume())
      val enumerator = Enumerator(Range(1,20).map(_.toString) :_*)  
      (enumerator |>> take3AndConsume).flatMap(_.run).value.get must equalTo(Range(4,20).map(_.toString).mkString)

    }

  }


  "Traversable.take" should {

    "pass only first 3 elements to Iteratee when applied with 3" in {
      
      val take3AndConsume = Traversable.take[String](3) &>>  Iteratee.consume()
      val enumerator = Enumerator("he","ybbb","bbb")  
      (enumerator |>> take3AndConsume).flatMap(_.run).value.get must equalTo("hey")

    }

    "pass alnog what's left after taking 3 elements" in {
      
      val take3AndConsume = (Traversable.take[String](3) &>>  Iteratee.consume()).flatMap(_ => Iteratee.consume())
      val enumerator = Enumerator("he","ybbb","bbb")  
      (enumerator |>> take3AndConsume).flatMap(_.run).value.get must equalTo("bbbbbb")

    }

  }

  "Enumeratee.map" should {

    "add one to each of the ints enumerated" in {
      
      val add1AndConsume = Enumeratee.map[Int](i => List(i+1)) &>>  Iteratee.consume()
      val enumerator = Enumerator(1,2,3,4)  
      (enumerator |>> add1AndConsume).flatMap(_.run).value.get must equalTo(Seq(2,3,4,5))

    }


    "infer its types correctly from previous enumeratee" in {
      
      val add1AndConsume = Enumeratee.map[Int](i => i+1) ><> Enumeratee.map(i => List(i)) &>>  Iteratee.consume()
      add1AndConsume : Iteratee[Int,List[Int]]
      true //this test is about compilation and if it compiles it means we got it right
    }

    "infer its types correctly from the preceeding enumerator" in {
      
      val addOne = Enumerator(1,2,3,4) &> Enumeratee.map(i => i+1) 
      addOne : Enumerator[Int]
      true //this test is about compilation and if it compiles it means we got it right
    }

    "interleave should mix two enumerators into one" in {
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

  }

}
