package play.api.libs.iteratee

import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.language.reflectiveCalls

import org.specs2.mutable._

object EnumerateesSpec extends Specification {

  "Enumeratee.drop" should {

    "ignore 3 chunkes when applied with 3" in {
      
      val drop3AndConsume = Enumeratee.drop[String](3) &>>  Iteratee.consume[String]()
      val enumerator = Enumerator(Range(1,20).map(_.toString) :_*)
      Await.result(enumerator |>>> drop3AndConsume, Duration.Inf) must equalTo(Range(4,20).map(_.toString).mkString)

    }

  }

  "Enumeratee.dropWhile" should {

    "ignore chunkes while predicate is valid" in {
      
      val dropWhileEC = TestExecutionContext()
      val drop3AndConsume = Enumeratee.dropWhile[String](_ != "4")(dropWhileEC) &>>  Iteratee.consume[String]()
      val enumerator = Enumerator(Range(1,20).map(_.toString) :_*)
      Await.result(enumerator |>>> drop3AndConsume, Duration.Inf) must equalTo(Range(4,20).map(_.toString).mkString)
      dropWhileEC.executionCount must equalTo(4)

    }

  }

  "Enumeratee.take" should {

    "pass only first 3 chunks to Iteratee when applied with 3" in {
      
      val take3AndConsume = Enumeratee.take[String](3) &>>  Iteratee.consume()
      val enumerator = Enumerator(Range(1,20).map(_.toString) :_*)
      Await.result(enumerator |>>> take3AndConsume, Duration.Inf) must equalTo(List(1,2,3).map(_.toString).mkString)

    }

    "passes along what's left of chunks after taking 3" in {
      val tec = TestExecutionContext()
      val take3AndConsume = (Enumeratee.take[String](3) &>>  Iteratee.consume()).flatMap(_ => Iteratee.consume())(tec)
      val enumerator = Enumerator(Range(1,20).map(_.toString) :_*)
      Await.result(enumerator |>>> take3AndConsume, Duration.Inf) must equalTo(Range(4,20).map(_.toString).mkString)
      tec.executionCount must equalTo(1)

    }

    "not execute callback on take 0" in {
      val generateEC = TestExecutionContext()
      val foldEC = TestExecutionContext()
      var triggered = false
      val enumerator = Enumerator.generateM {
          triggered = true
          Future(Some(1))(generateEC)
      }
      Await.result(enumerator &> Enumeratee.take(0) |>>> Iteratee.fold(0)((_: Int) + (_: Int))(foldEC), Duration.Inf) must equalTo(0)
      triggered must beFalse
      generateEC.executionCount must equalTo(0)
      foldEC.executionCount must equalTo(0)
    }

  }

  "Enumeratee.takeWhile" should {

    "pass chunks until condition is not met" in {
      val takeWhileEC = TestExecutionContext()
      val take3AndConsume = Enumeratee.takeWhile[String](_ != "4" )(takeWhileEC) &>>  Iteratee.consume()
      val enumerator = Enumerator(Range(1,20).map(_.toString) :_*)  
      Await.result(enumerator |>>> take3AndConsume, Duration.Inf) must equalTo(List(1,2,3).map(_.toString).mkString)
      takeWhileEC.executionCount must equalTo(4)
    }

    "passes along what's left of chunks after taking" in {
      val takeWhileEC = TestExecutionContext()
      val consumeFlatMapEC = TestExecutionContext()
      val generateEC = TestExecutionContext()
      val take3AndConsume = (Enumeratee.takeWhile[String](_ != "4")(takeWhileEC) &>>  Iteratee.consume()).flatMap(_ => Iteratee.consume())(consumeFlatMapEC)
      val enumerator = Enumerator(Range(1,20).map(_.toString) :_*)  
      Await.result(enumerator |>>> take3AndConsume, Duration.Inf) must equalTo(Range(4,20).map(_.toString).mkString)
      takeWhileEC.executionCount must equalTo(4)
      consumeFlatMapEC.executionCount must equalTo(1)

    }

  }


  "Traversable.take" should {

    "pass only first 3 elements to Iteratee when applied with 3" in {
      
      val take3AndConsume = Traversable.take[String](3) &>>  Iteratee.consume()
      val enumerator = Enumerator("he","ybbb","bbb")  
      Await.result(enumerator |>>> take3AndConsume, Duration.Inf) must equalTo("hey")

    }

    "pass along what's left after taking 3 elements" in {

      val consumeFlatMapEC = TestExecutionContext()
      val take3AndConsume = (Traversable.take[String](3) &>>  Iteratee.consume()).flatMap(_ => Iteratee.consume())(consumeFlatMapEC)
      val enumerator = Enumerator("he","ybbb","bbb")  
      Await.result(enumerator |>>> take3AndConsume, Duration.Inf) must equalTo("bbbbbb")
      consumeFlatMapEC.executionCount must equalTo(1)

    }

  }

  "Enumeratee.map" should {

    "add one to each of the ints enumerated" in {
      
      val add1AndConsume = Enumeratee.map[Int](i => List(i+1)) &>>  Iteratee.consume()
      val enumerator = Enumerator(1,2,3,4)  
      Await.result(enumerator |>>> add1AndConsume, Duration.Inf) must equalTo(Seq(2,3,4,5))

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
  
  "Enumeratee.flatten" should {

    "passAlong a future enumerator" in {

      val passAlongFuture = Enumeratee.flatten {
        concurrent.future { 
          Enumeratee.passAlong[Int]
        } (ExecutionContext.global)
      }
      val sumEC = TestExecutionContext()
      val sum = Iteratee.fold[Int, Int](0)(_+_)(sumEC)
      val enumerator = Enumerator(1,2,3,4,5,6,7,8,9)
      Await.result(enumerator |>>> passAlongFuture &>> sum, Duration.Inf) must equalTo(45)
      sumEC.executionCount must equalTo(9)
    }

  }

  "Enumeratee.filter" should {

    "ignore input that doesn't satisfy the predicate" in {
      
      val takesOnlyStringsWithLessThan4Chars = Enumeratee.filter[String](_.length < 4) &>>  Iteratee.consume()
      val enumerator = Enumerator("One","Two","Three","Four", "Five", "Six")
      Await.result(enumerator |>>> takesOnlyStringsWithLessThan4Chars, Duration.Inf) must equalTo("OneTwoSix")

    }

  }

  "Enumeratee.collect" should {

    "ignores input that doesn't satisfy the predicate and transform the input when matches" in {
      
      val takesOnlyStringsWithLessThan4Chars = Enumeratee.collect[String]{ case e@("One" | "Two" | "Six") => e.toUpperCase } &>>  Iteratee.consume()
      val enumerator = Enumerator("One","Two","Three","Four", "Five", "Six")  
      Await.result(enumerator |>>> takesOnlyStringsWithLessThan4Chars, Duration.Inf) must equalTo("ONETWOSIX")

    }

  }

  "Enumeratee.grouped" should {

    "group input elements according to a folder iteratee" in {
      val foldEC = TestExecutionContext()
      val folderIteratee = 
        Enumeratee.mapInput[String]{ 
          case Input.El("Concat") => Input.EOF;
          case other => other } &>>
        Iteratee.fold[String, String]("")((s,e) => s + e)(foldEC)

      val result = 
        Enumerator("He","ll","o","Concat", "Wo", "r", "ld", "Concat","!") &>
        Enumeratee.grouped(folderIteratee) ><>
        Enumeratee.map(List(_)) |>>>
        Iteratee.consume()
      Await.result(result, Duration.Inf) must equalTo(List("Hello","World","!"))
      foldEC.executionCount must equalTo(7)
    }

  }

  "Enumeratee.grouped" should {
    "pass along what is consumed by the last folder iteratee on EOF" in {

      val upToSpace = Traversable.splitOnceAt[String,Char](c => c != '\n')  &>> Iteratee.consume()

      val result = (Enumerator("dasdasdas ", "dadadasda\nshouldb\neinnext") &> Enumeratee.grouped(upToSpace) ><> Enumeratee.map(_+"|")) |>>> Iteratee.consume[String]()
      Await.result(result, Duration.Inf) must equalTo("dasdasdas dadadasda|shouldb|einnext|")
    }
  }

  "Enumeratee.scanLeft" should {

    "transform elements using a sate" in {
      val result = 
        Enumerator(1,2,3,4) &> 
        Enumeratee.scanLeft[Int](0)(_ + _) ><>
        Enumeratee.map(List(_)) |>>>
        Iteratee.consume()

      Await.result(result, Duration.Inf) must equalTo(List(1,3,6,10))

    }

  }

  "Enumeratee.recover" should {

    "perform computations and log errors" in {
      val eventuallyInput = Promise[Input[Int]]()
      val recoverEC = TestExecutionContext()
      val result = Enumerator(0, 2, 4) &> Enumeratee.recover[Int] { (_, input) =>
        eventuallyInput.success(input)
      }(recoverEC) &> Enumeratee.map { i =>
          8 / i
      } |>>> Iteratee.getChunks // => List(4, 2)

      Await.result(result, Duration.Inf) must equalTo(List(4, 2))
      Await.result(eventuallyInput.future, Duration.Inf) must equalTo(Input.El(0))
    }
  }

}
