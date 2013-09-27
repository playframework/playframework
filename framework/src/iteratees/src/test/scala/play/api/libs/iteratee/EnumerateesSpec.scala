package play.api.libs.iteratee

import play.api.libs.iteratee.Execution.Implicits.{ defaultExecutionContext => dec }
import scala.concurrent._
import scala.concurrent.duration.Duration

import org.specs2.mutable._

object EnumerateesSpec extends Specification
  with IterateeSpecification with ExecutionSpecification {

  "Enumeratee.zip" should {
    
    "combine the final results into a pair" in {
      Await.result(Enumeratee.zip(Done[Int, Int](2), Done[Int, Int](3)).unflatten, Duration.Inf) must equalTo(Step.Done((2, 3), Input.Empty))
    }
    
  }

  "Enumeratee.zipWith" should {
    
    "combine the final results" in {
      mustExecute(1) { zipEC =>
        Await.result(Enumeratee.zipWith(Done[Int, Int](2), Done[Int, Int](3))(_ * _)(zipEC).unflatten, Duration.Inf) must equalTo(Step.Done(6, Input.Empty))
      }
    }
    
  }

  "Enumeratee.mapInput" should {
    
    "transform each input" in {
      mustExecute(2) { mapEC =>
        mustTransformTo(1, 2)(2, 4)(Enumeratee.mapInput[Int](_.map(_ * 2))(mapEC))
      }
    }
    
  }

  "Enumeratee.mapConcatInput" should {
    
    "transform each input element into a sequence of inputs" in {
      mustExecute(2) { mapEC =>
        mustTransformTo(1, 2)(1, 1, 2, 2)(Enumeratee.mapConcatInput[Int](x => List(Input.El(x), Input.Empty, Input.El(x)))(mapEC))
      }
    }
    
  }

  "Enumeratee.mapConcat" should {
    
    "transform each input element into a sequence of input elements" in {
      mustExecute(2) { mapEC =>
        mustTransformTo(1, 2)(1, 1, 2, 2)(Enumeratee.mapConcat[Int](x => List(x, x))(mapEC))
      }
    }
    
  }

  "Enumeratee.mapFlatten" should {
    
    "transform each input element into the output of an enumerator" in {
      mustExecute(2) { mapFlattenEC =>
        mustTransformTo(1, 2)(1, 1, 2, 2)(Enumeratee.mapFlatten[Int](x => Enumerator(x, x))(mapFlattenEC))
      }
    }
    
  }

  "Enumeratee.mapInputFlatten" should {

    "transform each input" in {
      mustExecute(2) { mapEC =>
        val eee = Enumeratee.mapInputFlatten[Int][Int] {
          case Input.El(x) => Enumerator(x * 2)
          case Input.Empty => Enumerator.empty
          case Input.EOF => Enumerator.empty
        }(mapEC)
        mustTransformTo(1, 2)(2, 4)(eee compose Enumeratee.take(2))
      }
    }

  }

  "Enumeratee.mapInputM" should {
    
    "transform each input" in {
      mustExecute(2) { mapEC =>
        mustTransformTo(1, 2)(2, 4)(Enumeratee.mapInputM[Int]((i: Input[Int]) => Future.successful(i.map(_ * 2)))(mapEC))
      }
    }
    
  }

  "Enumeratee.mapM" should {
    
    "transform each input element" in {
      mustExecute(2) { mapEC =>
        mustTransformTo(1, 2)(2, 4)(Enumeratee.mapM[Int]((x: Int) => Future.successful(x * 2))(mapEC))
      }
    }
    
  }
  
  "Enumeratee.drop" should {

    "ignore 3 chunkes when applied with 3" in {
      
      val drop3AndConsume = Enumeratee.drop[String](3) &>>  Iteratee.consume[String]()
      val enumerator = Enumerator(Range(1,20).map(_.toString) :_*)
      Await.result(enumerator |>>> drop3AndConsume, Duration.Inf) must equalTo(Range(4,20).map(_.toString).mkString)

    }

  }

  "Enumeratee.dropWhile" should {

    "ignore chunks while predicate is valid" in {
      mustExecute(4) { dropWhileEC =>
        val drop3AndConsume = Enumeratee.dropWhile[String](_ != "4")(dropWhileEC) &>> Iteratee.consume[String]()
        val enumerator = Enumerator(Range(1, 20).map(_.toString): _*)
        Await.result(enumerator |>>> drop3AndConsume, Duration.Inf) must equalTo(Range(4, 20).map(_.toString).mkString)
      }
    }

  }

  "Enumeratee.take" should {

    "pass only first 3 chunks to Iteratee when applied with 3" in {
      
      val take3AndConsume = Enumeratee.take[String](3) &>>  Iteratee.consume()
      val enumerator = Enumerator(Range(1,20).map(_.toString) :_*)
      Await.result(enumerator |>>> take3AndConsume, Duration.Inf) must equalTo(List(1,2,3).map(_.toString).mkString)

    }

    "passes along what's left of chunks after taking 3" in {
      mustExecute(1) { flatMapEC =>
        val take3AndConsume = (Enumeratee.take[String](3) &>> Iteratee.consume()).flatMap(_ => Iteratee.consume())(flatMapEC)
        val enumerator = Enumerator(Range(1, 20).map(_.toString): _*)
        Await.result(enumerator |>>> take3AndConsume, Duration.Inf) must equalTo(Range(4, 20).map(_.toString).mkString)
      }
    }

    "not execute callback on take 0" in {
      mustExecute(0, 0) { (generateEC, foldEC) =>
        var triggered = false
        val enumerator = Enumerator.generateM {
          triggered = true
          Future(Some(1))(dec)
        }(generateEC)
        Await.result(enumerator &> Enumeratee.take(0) |>>> Iteratee.fold(0)((_: Int) + (_: Int))(foldEC), Duration.Inf) must equalTo(0)
        triggered must beFalse
      }
    }

  }

  "Enumeratee.takeWhile" should {

    "pass chunks until condition is not met" in {
      mustExecute(4) { takeWhileEC =>
        val take3AndConsume = Enumeratee.takeWhile[String](_ != "4")(takeWhileEC) &>> Iteratee.consume()
        val enumerator = Enumerator(Range(1, 20).map(_.toString): _*)
        Await.result(enumerator |>>> take3AndConsume, Duration.Inf) must equalTo(List(1, 2, 3).map(_.toString).mkString)
      }
    }

    "passes along what's left of chunks after taking" in {
      mustExecute(4, 1, 0) { (takeWhileEC, consumeFlatMapEC, generateEC) =>
        val take3AndConsume = (Enumeratee.takeWhile[String](_ != "4")(takeWhileEC) &>> Iteratee.consume()).flatMap(_ => Iteratee.consume())(consumeFlatMapEC)
        val enumerator = Enumerator(Range(1, 20).map(_.toString): _*)
        Await.result(enumerator |>>> take3AndConsume, Duration.Inf) must equalTo(Range(4, 20).map(_.toString).mkString)
      }
    }

  }

  "Enumeratee.breakE" should {
    
    "pass input through until the predicate is met" in {
      mustExecute(3) { breakEC =>
        mustTransformTo(1, 2, 3, 2, 1)(1, 2)(Enumeratee.breakE[Int](_ > 2)(breakEC))
      }
    }
    
  }

  "Enumeratee.onIterateeDone" should {

    "call the callback when the iteratee is done" in {
      mustExecute(1) { doneEC =>
        val count = new java.util.concurrent.atomic.AtomicInteger()
        mustTransformTo(1, 2, 3)(1, 2, 3)(Enumeratee.onIterateeDone(() => count.incrementAndGet())(doneEC))
        count.get() must equalTo(1)
      }
    }

  }

  "Enumeratee.onEOF" should {

    "call the callback on EOF" in {
      mustExecute(1) { eofEC =>
        val count = new java.util.concurrent.atomic.AtomicInteger()
        mustTransformTo(1, 2, 3)(1, 2, 3)(Enumeratee.onEOF(() => count.incrementAndGet())(eofEC))
        count.get() must equalTo(1)
      }
    }

  }

  "Traversable.take" should {

    "pass only first 3 elements to Iteratee when applied with 3" in {
      
      val take3AndConsume = Traversable.take[String](3) &>>  Iteratee.consume()
      val enumerator = Enumerator("he","ybbb","bbb")  
      Await.result(enumerator |>>> take3AndConsume, Duration.Inf) must equalTo("hey")

    }

    "pass along what's left after taking 3 elements" in {
      mustExecute(1) { consumeFlatMapEC =>
        val take3AndConsume = (Traversable.take[String](3) &>> Iteratee.consume()).flatMap(_ => Iteratee.consume())(consumeFlatMapEC)
        val enumerator = Enumerator("he", "ybbb", "bbb")
        Await.result(enumerator |>>> take3AndConsume, Duration.Inf) must equalTo("bbbbbb")
      }
    }

  }

  "Enumeratee.map" should {

    "add one to each of the ints enumerated" in {
      mustExecute(4) { mapEC =>
        val add1AndConsume = Enumeratee.map[Int](i => List(i+1))(mapEC) &>> Iteratee.consume[List[Int]]()
        val enumerator = Enumerator(1,2,3,4)  
        Await.result(enumerator |>>> add1AndConsume, Duration.Inf) must equalTo(Seq(2,3,4,5))
      }
    }


    "infer its types correctly from previous enumeratee" in {
      mustExecute(0, 0) { (map1EC, map2EC) =>
        val add1AndConsume = Enumeratee.map[Int](i => i+1)(map1EC) ><> Enumeratee.map[Int](i => List(i))(map2EC) &>>
          Iteratee.consume[List[Int]]()
        add1AndConsume : Iteratee[Int,List[Int]]
        true //this test is about compilation and if it compiles it means we got it right
      }
    }

    "infer its types correctly from the preceeding enumerator" in {
      mustExecute(0) { mapEC =>
        val addOne = Enumerator(1,2,3,4) &> Enumeratee.map[Int](i => i+1)(mapEC)
        addOne : Enumerator[Int]
        true //this test is about compilation and if it compiles it means we got it right
      }
    }

  }
  
  "Enumeratee.flatten" should {

    "passAlong a future enumerator" in {
      mustExecute(9) { sumEC =>
        val passAlongFuture = Enumeratee.flatten {
          concurrent.future {
            Enumeratee.passAlong[Int]
          }(ExecutionContext.global)
        }
        val sum = Iteratee.fold[Int, Int](0)(_ + _)(sumEC)
        val enumerator = Enumerator(1, 2, 3, 4, 5, 6, 7, 8, 9)
        Await.result(enumerator |>>> passAlongFuture &>> sum, Duration.Inf) must equalTo(45)
      }
    }

  }

  "Enumeratee.filter" should {

    "only enumerate input that satisfies the predicate" in {
      mustExecute(6) { filterEC =>
        mustTransformTo("One", "Two", "Three", "Four", "Five", "Six")("One", "Two", "Six")(Enumeratee.filter[String](_.length < 4)(filterEC))
      }
    }

  }

  "Enumeratee.filterNot" should {

    "only enumerate input that doesn't satisfy the predicate" in {
      mustExecute(6) { filterEC =>
        mustTransformTo("One", "Two", "Three", "Four", "Five", "Six")("Three", "Four", "Five")(Enumeratee.filterNot[String](_.length < 4)(filterEC))
      }
    }

  }

  "Enumeratee.collect" should {

    "ignores input that doesn't satisfy the predicate and transform the input when matches" in {
      mustExecute(6) { collectEC =>
        mustTransformTo("One", "Two", "Three", "Four", "Five", "Six")("ONE", "TWO", "SIX")(Enumeratee.collect[String]{ case e@("One" | "Two" | "Six") => e.toUpperCase }(collectEC))
      }
    }

  }

  "Enumeratee.grouped" should {

    "group input elements according to a folder iteratee" in {
      mustExecute(9, 7, 3) { (mapInputEC, foldEC, mapEC) =>
        val folderIteratee =
          Enumeratee.mapInput[String] {
            case Input.El("Concat") => Input.EOF;
            case other => other
          }(mapInputEC) &>>
            Iteratee.fold[String, String]("")((s, e) => s + e)(foldEC)

        val result =
          Enumerator("He", "ll", "o", "Concat", "Wo", "r", "ld", "Concat", "!") &>
            Enumeratee.grouped(folderIteratee) ><>
            Enumeratee.map[String](List(_))(mapEC) |>>>
            Iteratee.consume[List[String]]()
        Await.result(result, Duration.Inf) must equalTo(List("Hello", "World", "!"))
      }
    }

  }

  "Enumeratee.grouped" should {
    "pass along what is consumed by the last folder iteratee on EOF" in {
      mustExecute(4, 3) { (splitEC, mapEC) =>
        val upToSpace = Traversable.splitOnceAt[String, Char](c => c != '\n')(implicitly[String => scala.collection.TraversableLike[Char, String]], splitEC) &>> Iteratee.consume()

        val result = (Enumerator("dasdasdas ", "dadadasda\nshouldb\neinnext") &> Enumeratee.grouped(upToSpace) ><> Enumeratee.map[String](_ + "|")(mapEC)) |>>> Iteratee.consume[String]()
        Await.result(result, Duration.Inf) must equalTo("dasdasdas dadadasda|shouldb|einnext|")
      }
    }
  }

  "Enumeratee.scanLeft" should {

    "transform elements using a sate" in {
      mustExecute(4) { mapEC =>
        val result =
          Enumerator(1, 2, 3, 4) &>
            Enumeratee.scanLeft[Int](0)(_ + _) ><>
            Enumeratee.map[Int](List(_))(mapEC) |>>>
            Iteratee.consume[List[Int]]()

        Await.result(result, Duration.Inf) must equalTo(List(1, 3, 6, 10))
      }
    }

  }

  "Enumeratee.recover" should {

    "perform computations and log errors" in {
      mustExecute(3, 3) { (recoverEC, mapEC) =>
        val eventuallyInput = Promise[Input[Int]]()
        val result = Enumerator(0, 2, 4) &> Enumeratee.recover[Int] { (_, input) =>
          eventuallyInput.success(input)
        }(recoverEC) &> Enumeratee.map[Int] { i =>
          8 / i
        }(mapEC) |>>> Iteratee.getChunks // => List(4, 2)

        Await.result(result, Duration.Inf) must equalTo(List(4, 2))
        Await.result(eventuallyInput.future, Duration.Inf) must equalTo(Input.El(0))
      }
    }
  }

}
