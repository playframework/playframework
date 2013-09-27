package play.api.libs.iteratee

import org.specs2.mutable._
import java.io.OutputStream
import java.util.concurrent.{ CountDownLatch, TimeUnit }
import scala.concurrent.{ ExecutionContext, Promise, Future, Await }
import scala.util.{ Failure, Success, Try }

object IterateesSpec extends Specification
  with IterateeSpecification with ExecutionSpecification {

  def checkFoldResult[A, E](i: Iteratee[A, E], expected: Step[A, E]) = {
    mustExecute(1) { foldEC =>
      await(i.fold(s => Future.successful(s))(foldEC)) must equalTo(expected)
    }
  }

  def checkUnflattenResult[A, E](i: Iteratee[A, E], expected: Step[A, E]) = {
    await(i.unflatten) must equalTo(expected)
  }

  def checkImmediateFoldFailure[A, E](i: Iteratee[A, E]) = {
    mustExecute(1) { foldEC =>
      val e = new Exception("exception")
      val result = ready(i.fold(_ => throw e)(foldEC))
      result.value must equalTo(Some(Failure(e)))
    }
  }

  def checkFutureFoldFailure[A, E](i: Iteratee[A, E]) = {
    mustExecute(1, 1) { (foldEC, folderEC) =>
      val e = new Exception("exception")
      val preparedFolderEC = folderEC.prepare()
      val result = ready(i.fold(_ => Future(throw e)(preparedFolderEC))(foldEC))
      result.value must equalTo(Some(Failure(e)))
    }
  }

  def mustTranslate3To(x: Int)(f: Iteratee[Int, Int] => Iteratee[Int, Int]) = {
    await(f(Done(3)).unflatten) must equalTo(Step.Done(x, Input.Empty))
  }

  "Flattened iteratees" should {

    val i = Iteratee.flatten(Future.successful(Done(1, Input.El("x"))))

    "delegate folding to their promised iteratee" in {
      checkFoldResult(i, Step.Done(1, Input.El("x")))
    }

    "return immediate fold errors in promise" in {
      checkImmediateFoldFailure(i)
    }

    "return future fold errors in promise" in {
      checkFutureFoldFailure(i)
    }

    "support unflattening their state" in {
      checkUnflattenResult(i, Step.Done(1, Input.El("x")))
    }

  }

  "Done iteratees" should {

    val i = Done(1, Input.El("x"))

    "fold with their result and unused input" in {
      checkFoldResult(i, Step.Done(1, Input.El("x")))
    }

    "return immediate fold errors in promise" in {
      checkImmediateFoldFailure(i)
    }

    "return future fold errors in promise" in {
      checkFutureFoldFailure(i)
    }

    "fold input with fold1" in {
      mustExecute(1) { foldEC =>
        mustTranslate3To(5)(it => Iteratee.flatten(it.fold1(
          (a, i) => Future.successful(Done(a + 2, i)),
          _ => ???,
          (_, _) => ???)(foldEC)))
      }
    }

    "fold input with pureFold" in {
      mustExecute(1) { foldEC =>
        mustTranslate3To(9)(it => Iteratee.flatten(it.pureFold(_ => Done[Int, Int](9))(foldEC)))
      }
    }

    "fold input with pureFlatFold" in {
      mustExecute(1) { foldEC =>
        mustTranslate3To(9)(_.pureFlatFold(_ => Done[Int, Int](9))(foldEC))
      }
    }

    "fold input with flatFold0" in {
      mustExecute(1) { foldEC =>
        mustTranslate3To(9)(_.flatFold0(_ => Future.successful(Done[Int, Int](9)))(foldEC))
      }
    }

    "fold input with flatFold" in {
      mustExecute(1) { foldEC =>
        mustTranslate3To(9)(_.flatFold(
          (_, _) => Future.successful(Done[Int, Int](9)),
          _ => ???,
          (_, _) => ???)(foldEC))
      }
    }

    "support unflattening their state" in {
      checkUnflattenResult(i, Step.Done(1, Input.El("x")))
    }

    "flatMap directly to result when no remaining input" in {
      mustExecute(1) { flatMapEC =>
        await(Done(3).flatMap((x: Int) => Done[Int, Int](x * 2))(flatMapEC).unflatten) must equalTo(Step.Done(6, Input.Empty))
      }
    }

    "flatMap result and process remaining input with Done" in {
      mustExecute(1) { flatMapEC =>
        await(Done(3, Input.El("remaining")).flatMap((x: Int) => Done[String, Int](x * 2))(flatMapEC).unflatten) must equalTo(Step.Done(6, Input.El("remaining")))
      }
    }

    "flatMap result and process remaining input with Cont" in {
      mustExecute(1) { flatMapEC =>
        await(Done(3, Input.El("remaining")).flatMap((x: Int) => Cont(in => Done[String, Int](x * 2, in)))(flatMapEC).unflatten) must equalTo(Step.Done(6, Input.El("remaining")))
      }
    }

    "flatMap result and process remaining input with Error" in {
      mustExecute(1) { flatMapEC =>
        await(Done(3, Input.El("remaining")).flatMap((x: Int) => Error("error", Input.El("bad")))(flatMapEC).unflatten) must equalTo(Step.Error("error", Input.El("bad")))
      }
    }

    "flatMap result with flatMapM" in {
      mustExecute(1) { flatMapEC =>
        mustTranslate3To(6)(_.flatMapM((x: Int) => Future.successful(Done[Int, Int](x * 2)))(flatMapEC))
      }
    }

    "fold result with flatMapInput" in {
      mustExecute(1) { flatMapEC =>
        mustTranslate3To(1)(_.flatMapInput(_ => Done(1))(flatMapEC))
      }
    }

    "concatenate unused input with flatMapTraversable" in {
      mustExecute(1) { flatMapEC =>
        await(Done(3, Input.El(List(1, 2))).flatMapTraversable(_ => Done[List[Int], Int](4, Input.El(List(3, 4))))(
          implicitly[List[Int] => scala.collection.TraversableLike[Int, List[Int]]],
          implicitly[scala.collection.generic.CanBuildFrom[List[Int], Int, List[Int]]],
          flatMapEC).unflatten) must equalTo(Step.Done(4, Input.El(List(1, 2, 3, 4))))
      }
    }

  }

  "Cont iteratees" should {

    val k: Input[String] => Iteratee[String, Int] = x => ???
    val i = Cont(k)

    "fold with their continuation" in {
      checkFoldResult(i, Step.Cont(k))
    }

    "return immediate fold errors in promise" in {
      checkImmediateFoldFailure(i)
    }

    "return future fold errors in promise" in {
      checkFutureFoldFailure(i)
    }

    "support unflattening their state" in {
      checkUnflattenResult(i, Step.Cont(k))
    }

    "flatMap recursively" in {
      mustExecute(1) { flatMapEC =>
        await(Iteratee.flatten(Cont[Int, Int](_ => Done(3)).flatMap((x: Int) => Done[Int, Int](x * 2))(flatMapEC).feed(Input.El(11))).unflatten) must equalTo(Step.Done(6, Input.Empty))
      }
    }

  }

  "Error iteratees" should {

    val i = Error("msg", Input.El("x"))

    "fold with their message and the input that caused the error" in {
      checkFoldResult(i, Step.Error("msg", Input.El("x")))
    }

    "return immediate fold errors in promise" in {
      checkImmediateFoldFailure(i)
    }

    "return future fold errors in promise" in {
      checkFutureFoldFailure(i)
    }

    "support unflattening their state" in {
      checkUnflattenResult(i, Step.Error("msg", Input.El("x")))
    }

    "flatMap to an error" in {
      mustExecute(0) { flatMapEC =>
        await(Error("msg", Input.El("bad")).flatMap((x: Int) => Done("done"))(flatMapEC).unflatten) must equalTo(Step.Error("msg", Input.El("bad")))
      }
    }

  }

  "Iteratees fed multiple inputs" should {

    // TODO: mapDone is deprecated, remove in 2.3.
    "map the final iteratee's result (with mapDone)" in {
      mustExecute(4, 1) { (foldEC, mapEC) =>
        await(Enumerator(1, 2, 3, 4) |>>> Iteratee.fold[Int, Int](0)(_ + _)(foldEC).mapDone(_ * 2)(mapEC)) must equalTo(20)
      }
    }

    // TODO: mapDone is deprecated, remove in 2.3.
    "map the final iteratee's result (with mapDone)" in {
      mustExecute(4, 1) { (foldEC, mapEC) =>
        await(Enumerator(1, 2, 3, 4) |>>> Iteratee.fold[Int, Int](0)(_ + _)(foldEC).mapDone(_ * 2)(mapEC)) must equalTo(20)
      }
    }

    "map the final iteratee's result (with map)" in {
      mustExecute(4, 1) { (foldEC, mapEC) =>
        await(Enumerator(1, 2, 3, 4) |>>> Iteratee.fold[Int, Int](0)(_ + _)(foldEC).map(_ * 2)(mapEC)) must equalTo(20)
      }
    }

    "map the final iteratee's result (with mapM)" in {
      mustExecute(4, 1) { (foldEC, mapEC) =>
        await(Enumerator(1, 2, 3, 4) |>>> Iteratee.fold[Int, Int](0)(_ + _)(foldEC).mapM(x => Future.successful(x * 2))(mapEC)) must equalTo(20)
      }
    }

  }

  "Iteratee.fold" should {

    "fold input" in {
      mustExecute(4) { foldEC =>
        await(Enumerator(1, 2, 3, 4) |>>> Iteratee.fold[Int, Int](0)(_ + _)(foldEC)) must equalTo(10)
      }
    }

  }

  "Iteratee.foldM" should {

    "fold input" in {
      mustExecute(4) { foldEC =>
        await(Enumerator(1, 2, 3, 4) |>>> Iteratee.foldM[Int, Int](0)((x, y) => Future.successful(x + y))(foldEC)) must equalTo(10)
      }
    }

  }

  "Iteratee.fold2" should {

    "fold input" in {
      mustExecute(4) { foldEC =>
        val folder = (x: Int, y: Int) => Future.successful((x + y, false))
        await(Enumerator(1, 2, 3, 4) |>>> Iteratee.fold2[Int, Int](0)(folder)(foldEC)) must equalTo(10)
      }
    }

    "fold input, stopping early" in {
      mustExecute(3) { foldEC =>
        val folder = (x: Int, y: Int) => Future.successful((x + y, (y > 2)))
        await(Enumerator(1, 2, 3, 4) |>>> Iteratee.fold2[Int, Int](0)(folder)(foldEC)) must equalTo(6)
      }
    }

  }

  "Iteratee.foldM" should {

    "fold input" in {
      mustExecute(4) { foldEC =>
        await(Enumerator(1, 2, 3, 4) |>>> Iteratee.fold1[Int, Int](Future.successful(0))((x, y) => Future.successful(x + y))(foldEC)) must equalTo(10)
      }
    }

  }

  "Iteratee.consume" should {

    "return its concatenated input" in {
      val s = List(List(1, 2), List(3), List(4, 5))
      val r = List(1, 2, 3, 4, 5)
      await(Enumerator.enumerateSeq1(s) |>>> Iteratee.consume[List[Int]]()) must equalTo(r)
    }

  }

  "Iteratee.getChunks" should {

    "return its input as a list" in {
      val s = List(1, 2, 3, 4, 5)
      await(Enumerator.enumerateSeq1(s) |>>> Iteratee.getChunks[Int]) must equalTo(s)
    }

  }

}