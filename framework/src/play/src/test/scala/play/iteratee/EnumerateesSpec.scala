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

  }

}
