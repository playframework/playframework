package play.api.libs.iteratee

import scala.concurrent.ExecutionContext.Implicits.global

import play.api.libs.concurrent._

import org.specs2.mutable._

object EnumerateesSpec extends Specification {

  "Enumeratee.drop" should {

    "ignore 3 chunkes when applied with 3" in {
      
      val drop3AndConsume = Enumeratee.drop[String](3) &>>  Iteratee.consume[String]()
      val enumerator = Enumerator(Range(1,20).map(_.toString) :_*)  
      (enumerator |>> drop3AndConsume).flatMap(_.run).value1.get must equalTo(Range(4,20).map(_.toString).mkString)

    }

  }

  "Enumeratee.dropWhile" should {

    "ignore chunkes while predicate is valid" in {
      
      val drop3AndConsume = Enumeratee.dropWhile[String](_ != "4") &>>  Iteratee.consume[String]()
      val enumerator = Enumerator(Range(1,20).map(_.toString) :_*)  
      (enumerator |>> drop3AndConsume).flatMap(_.run).value1.get must equalTo(Range(4,20).map(_.toString).mkString)

    }

  }

  "Enumeratee.take" should {

    "pass only first 3 chunkes to Iteratee when applied with 3" in {
      
      val take3AndConsume = Enumeratee.take[String](3) &>>  Iteratee.consume()
      val enumerator = Enumerator(Range(1,20).map(_.toString) :_*)  
      (enumerator |>> take3AndConsume).flatMap(_.run).value1.get must equalTo(List(1,2,3).map(_.toString).mkString)

    }

    "passes along what's left of chunks after taking 3" in {
      
      val take3AndConsume = (Enumeratee.take[String](3) &>>  Iteratee.consume()).flatMap(_ => Iteratee.consume())
      val enumerator = Enumerator(Range(1,20).map(_.toString) :_*)  
      (enumerator |>> take3AndConsume).flatMap(_.run).value1.get must equalTo(Range(4,20).map(_.toString).mkString)

    }

  }

  "Enumeratee.takeWhile" should {

    "pass chunks until condition is not met" in {
      val take3AndConsume = Enumeratee.takeWhile[String](_ != "4" ) &>>  Iteratee.consume()
      val enumerator = Enumerator(Range(1,20).map(_.toString) :_*)  
      (enumerator |>> take3AndConsume).flatMap(_.run).value1.get must equalTo(List(1,2,3).map(_.toString).mkString)
    }

    "passes along what's left of chunks after taking" in {
      
      val take3AndConsume = (Enumeratee.takeWhile[String](_ != "4") &>>  Iteratee.consume()).flatMap(_ => Iteratee.consume())
      val enumerator = Enumerator(Range(1,20).map(_.toString) :_*)  
      (enumerator |>> take3AndConsume).flatMap(_.run).value1.get must equalTo(Range(4,20).map(_.toString).mkString)

    }

  }


  "Traversable.take" should {

    "pass only first 3 elements to Iteratee when applied with 3" in {
      
      val take3AndConsume = Traversable.take[String](3) &>>  Iteratee.consume()
      val enumerator = Enumerator("he","ybbb","bbb")  
      (enumerator |>> take3AndConsume).flatMap(_.run).value1.get must equalTo("hey")

    }

    "pass alnog what's left after taking 3 elements" in {
      
      val take3AndConsume = (Traversable.take[String](3) &>>  Iteratee.consume()).flatMap(_ => Iteratee.consume())
      val enumerator = Enumerator("he","ybbb","bbb")  
      (enumerator |>> take3AndConsume).flatMap(_.run).value1.get must equalTo("bbbbbb")

    }

  }

  "Enumeratee.map" should {

    "add one to each of the ints enumerated" in {
      
      val add1AndConsume = Enumeratee.map[Int](i => List(i+1)) &>>  Iteratee.consume()
      val enumerator = Enumerator(1,2,3,4)  
      (enumerator |>> add1AndConsume).flatMap(_.run).value1.get must equalTo(Seq(2,3,4,5))

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

  "Enumeratee.filter" should {

    "ignore input that doesn't satisfy the predicate" in {
      
      val takesOnlyStringsWithLessThan4Chars = Enumeratee.filter[String](_.length < 4) &>>  Iteratee.consume()
      val enumerator = Enumerator("One","Two","Three","Four", "Five", "Six")  
      (enumerator |>> takesOnlyStringsWithLessThan4Chars).flatMap(_.run).value1.get must equalTo("OneTwoSix")

    }

  }

  "Enumeratee.collect" should {

    "ignores input that doesn't satisfy the predicate and transform the input when matches" in {
      
      val takesOnlyStringsWithLessThan4Chars = Enumeratee.collect[String]{ case e@("One" | "Two" | "Six") => e.toUpperCase } &>>  Iteratee.consume()
      val enumerator = Enumerator("One","Two","Three","Four", "Five", "Six")  
      (enumerator |>> takesOnlyStringsWithLessThan4Chars).flatMap(_.run).value1.get must equalTo("ONETWOSIX")

    }

  }

  "Enumeratee.grouped" should {

    "group input elements according to a folder iteratee" in {
      val folderIteratee = 
        Enumeratee.mapInput[String]{ 
          case Input.El("Concat") => Input.EOF;
          case other => other } &>>
        Iteratee.fold("")((s,e) => s + e)

      val result = 
        Enumerator("He","ll","o","Concat", "Wo", "r", "ld", "Concat","!") &>
        Enumeratee.grouped(folderIteratee) ><>
        Enumeratee.map(List(_)) |>>
        Iteratee.consume()
      result.flatMap(_.run).value1.get must equalTo(List("Hello","World","!"))

    }

  }

  "Enumeratee.grouped" should {
    "pass along what is consumed by the last folder iteratee on EOF" in {

      val upToSpace = Traversable.splitOnceAt[String,Char](c => c != '\n')  &>> Iteratee.consume()

      val result = (Enumerator("dasdasdas ", "dadadasda\nshouldb\neinnext") &> Enumeratee.grouped(upToSpace) ><> Enumeratee.map(_+"|")) |>> Iteratee.consume[String]()
      result.flatMap(_.run).value1.get must equalTo("dasdasdas dadadasda|shouldb|einnext|")
    }
  }

  "Enumeratee.scanLeft" should {

    "transform elements using a sate" in {
      val result = 
        Enumerator(1,2,3,4) &> 
        Enumeratee.scanLeft[Int](0)(_ + _) ><>
        Enumeratee.map(List(_)) |>>
        Iteratee.consume()

      result.flatMap(_.run).value1.get must equalTo(List(1,3,6,10))

    }

  }

}
