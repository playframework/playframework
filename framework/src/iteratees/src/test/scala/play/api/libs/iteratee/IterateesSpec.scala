/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
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

  def checkFoldTryResult[A, E](i: Iteratee[A, E], expected: Try[Step[A, E]]) = {
    mustExecute(0) { foldEC =>
      Try(await(i.fold(s => Future.successful(s))(foldEC))) must equalTo(expected)
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

    "return errors in flattened failed future" in {
      val ex = new Exception("exception message")
      val flattenedFailedFuture = Iteratee.flatten(Future.failed(ex))
      checkFoldTryResult[Nothing, Nothing](flattenedFailedFuture, Failure(ex))
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

    "not overflow the stack when called recursively" in {
      // Find how much recursion is needed to overflow the stack
      // on the current Java runtime.
      def overflows(n: Int): Boolean = {
        def recurseTimes(n: Int): Unit = {
          if (n == 0) () else identity(recurseTimes(n - 1))
        }
        try {
          recurseTimes(n)
          false // Didn't overflow
        } catch {
          case _: StackOverflowError => true
        }
      }
      val overflowDepth: Int = (12 until 20).map(1 << _).find(overflows).get

      import ExecutionContext.Implicits.global
      val unitDone: Iteratee[Unit, Unit] = Done(())
      val flatMapped: Iteratee[Unit, Unit] = (0 until overflowDepth).foldLeft[Iteratee[Unit, Unit]](Cont(_ => unitDone)) {
        case (it, _) => it.flatMap(_ => unitDone)
      }
      await(await(flatMapped.feed(Input.EOF)).unflatten) must equalTo(Step.Done((), Input.Empty))
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

  "Iteratee.recover" should {

    val expected = "expected"
    val unexpected = "should not be returned"

    "do nothing on a Done iteratee" in {
      mustExecute(1) { implicit foldEC =>
        val it = done(expected).recover { case t: Throwable => unexpected }
        val actual = await(Enumerator(unexpected) |>>> it)
        actual must equalTo(expected)
      }
    }

    "do nothing on an eventually Done iteratee" in {
      mustExecute(1) { implicit foldEC =>
        val it = delayed(done(expected)).recover { case t: Throwable => unexpected }
        val actual = await(Enumerator(unexpected) |>>> it)
        actual must equalTo(expected)
      }
    }

    "recover with the expected fallback value from an Error iteratee" in {
      mustExecute(2) { implicit foldEC =>
        val it = error(unexpected).recover { case t: Throwable => expected }
        val actual = await(Enumerator(unexpected) |>>> it)
        actual must equalTo(expected)
      }
    }

    "leave the Error iteratee unchanged if the Exception type doesn't match the partial function" in {
      mustExecute(1) { implicit foldEC =>
        val it = error(expected).recover { case t: IllegalArgumentException => unexpected }
        val actual = await((Enumerator(unexpected) |>>> it).failed)
        actual.getMessage must equalTo(expected)
      }
    }

    "recover with the expected fallback value from an eventually Error iteratee" in {
      mustExecute(2) { implicit foldEC =>
        val it = delayed(error(unexpected)).recover { case t: Throwable => expected }
        val actual = await(Enumerator(unexpected) |>>> it)
        actual must equalTo(expected)
      }
    }

    "recover with the expected fallback value from an iteratee that eventually throws an exception" in {
      mustExecute(2) { implicit foldEC =>
        val it = delayed(throw new RuntimeException(unexpected)).recover { case t: Throwable => expected }
        val actual = await(Enumerator(unexpected) |>>> it)
        actual must equalTo(expected)
      }
    }

    "do nothing on a Cont iteratee that becomes Done with input" in {
      mustExecute(2) { implicit foldEC =>
        val it = cont(input => done(input)).recover { case t: Throwable => unexpected }
        val actual = await(Enumerator(expected) |>>> it)
        actual must equalTo(expected)
      }
    }

    "do nothing on an eventually Cont iteratee that becomes Done with input" in {
      mustExecute(2) { implicit foldEC =>
        val it = delayed(cont(input => done(input))).recover { case t: Throwable => unexpected }
        val actual = await(Enumerator(expected) |>>> it)
        actual must equalTo(expected)
      }
    }

    "do nothing on a Cont iteratee that eventually becomes Done with input" in {
      mustExecute(2) { implicit foldEC =>
        val it = cont(input => delayed(done(input))).recover { case t: Throwable => unexpected }
        val actual = await(Enumerator(expected) |>>> it)
        actual must equalTo(expected)
      }
    }

    "do nothing on an Cont iteratee that eventually becomes Done with input after several steps" in {
      mustExecute(4) { implicit foldEC =>
        val it = delayed(
          cont(input1 => delayed(
            cont(input2 => delayed(
              cont(input3 => delayed(
                done(input1 + input2 + input3)
              ))
            ))
          ))
        ).recover { case t: Throwable => unexpected }
        val actual = await(Enumerator(expected, expected, expected) |>>> it)
        actual must equalTo(expected * 3)
      }
    }

    "recover with the expected fallback value from a Cont iteratee that eventually becomes an Error iteratee after several steps" in {
      mustExecute(5) { implicit foldEC =>
        val it = delayed(
          cont(input1 => delayed(
            cont(input2 => delayed(
              cont(input3 => delayed(
                error(input1 + input2 + input3)
              ))
            ))
          ))
        ).recover { case t: Throwable => expected }
        val actual = await(Enumerator(unexpected, unexpected, unexpected) |>>> it)
        actual must equalTo(expected)
      }
    }
  }

  "Iteratee.recoverM" should {

    val expected = "expected"
    val unexpected = "should not be returned"

    "do nothing on a Done iteratee" in {
      mustExecute(1) { implicit foldEC =>
        val it = done(expected).recoverM { case t: Throwable => Future.successful(unexpected) }
        val actual = await(Enumerator(unexpected) |>>> it)
        actual must equalTo(expected)
      }
    }

    "do nothing on a Done iteratee even if the recover block gets a failed Future" in {
      mustExecute(1) { implicit foldEC =>
        val it = done(expected).recoverM { case t: Throwable => Future.failed(new RuntimeException(unexpected)) }
        val actual = await(Enumerator(unexpected) |>>> it)
        actual must equalTo(expected)
      }
    }

    "recover with the expected fallback Future from an Error iteratee" in {
      mustExecute(2) { implicit foldEC =>
        val it = error(unexpected).recoverM { case t: Throwable => Future.successful(expected) }
        val actual = await(Enumerator(unexpected) |>>> it)
        actual must equalTo(expected)
      }
    }

    "leave the Error iteratee unchanged if the Exception type doesn't match the partial function" in {
      mustExecute(1) { implicit foldEC =>
        val it = error(expected).recoverM { case t: IllegalArgumentException => Future.successful(unexpected) }
        val actual = await((Enumerator(unexpected) |>>> it).failed)
        actual.getMessage must equalTo(expected)
      }
    }

    "return a failed Future if you try to recover from an Error iteratee with a failed Future" in {
      mustExecute(2) { implicit foldEC =>
        val exception = new RuntimeException(expected)
        val it = error(unexpected).recoverM { case t: Throwable => Future.failed(exception) }
        val actual = await((Enumerator(unexpected) |>>> it).failed)
        actual must equalTo(exception)
      }
    }
  }

  "Iteratee.recoverWith" should {

    val expected = "expected"
    val unexpected = "should not be returned"

    "do nothing on a Done iteratee" in {
      mustExecute(1) { implicit foldEC =>
        val it = done(expected).recoverWith { case t: Throwable => done(unexpected) }
        val actual = await(Enumerator(unexpected) |>>> it)
        actual must equalTo(expected)
      }
    }

    "do nothing on a Done iteratee even if the recover block gets an error Iteratee" in {
      mustExecute(1) { implicit foldEC =>
        val it = done(expected).recoverWith { case t: Throwable => error(unexpected) }
        val actual = await(Enumerator(unexpected) |>>> it)
        actual must equalTo(expected)
      }
    }

    "recover with the expected fallback Iteratee from an Error iteratee" in {
      mustExecute(1) { implicit foldEC =>
        val it = error(unexpected).recoverWith { case t: Throwable => done(expected) }
        val actual = await(Enumerator(unexpected) |>>> it)
        actual must equalTo(expected)
      }
    }

    "leave the Error iteratee unchanged if the Exception type doesn't match the partial function" in {
      mustExecute(1) { implicit foldEC =>
        val it = error(expected).recoverWith { case t: IllegalArgumentException => done(unexpected) }
        val actual = await((Enumerator(unexpected) |>>> it).failed)
        actual.getMessage must equalTo(expected)
      }
    }

    "return a failed Future if you try to recover from an Error iteratee with an Error iteratee" in {
      mustExecute(1) { implicit foldEC =>
        val it = error(unexpected).recoverWith { case t: Throwable => error(expected) }
        val actual = await((Enumerator(unexpected) |>>> it).failed)
        actual.getMessage must equalTo(expected)
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

  "Iteratee.takeUpTo" should {

    def takenAndNotTaken[E](n: Int): Iteratee[E, (Seq[E], Seq[E])] = {
      import ExecutionContext.Implicits.global
      for {
        seq1 <- Iteratee.takeUpTo(n)
        seq2 <- Iteratee.getChunks
      } yield (seq1, seq2)
    }

    "take 0 elements from 0" in {
      await(Enumerator() |>>> takenAndNotTaken(0)) must equalTo((Seq(), Seq()))
    }

    "take 0 elements from 0 when asked for 2" in {
      await(Enumerator() |>>> takenAndNotTaken(2)) must equalTo((Seq(), Seq()))
    }

    "take 1 element from 2" in {
      await(Enumerator(1, 2) |>>> takenAndNotTaken(1)) must equalTo((Seq(1), Seq(2)))
    }

    "take 2 elements from 2" in {
      await(Enumerator(1, 2) |>>> takenAndNotTaken(2)) must equalTo((Seq(1, 2), Seq()))
    }

    "take 2 elements from 2 when asked for 3" in {
      await(Enumerator(1, 2) |>>> takenAndNotTaken(3)) must equalTo((Seq(1, 2), Seq()))
    }

    "skip Input.Empty when taking elements" in {
      val enum = Enumerator(1, 2) >>> Enumerator.enumInput(Input.Empty) >>> Enumerator(3, 4)
      await(enum |>>> takenAndNotTaken(3)) must equalTo((Seq(1, 2, 3), Seq(4)))
    }

  }

  "Iteratee.isEmpty" should {

    def isEmptyThenRest[E]: Iteratee[E, (Boolean, Seq[E])] = {
      import ExecutionContext.Implicits.global
      for {
        empty <- Iteratee.isEmpty
        seq <- Iteratee.getChunks
      } yield (empty, seq)
    }

    "be true for a stream with only EOF" in {
      await(Enumerator() |>>> isEmptyThenRest) must equalTo((true, Seq()))
    }

    "be true for a stream with Empty and EOF" in {
      val enum = Enumerator.enumInput(Input.Empty) >>> Enumerator.eof
      await(enum |>>> isEmptyThenRest) must equalTo((true, Seq()))
    }

    "be false for a stream with one element" in {
      await(Enumerator(1) |>>> isEmptyThenRest) must equalTo((false, Seq(1)))
    }

    "be false for a stream with two elements" in {
      await(Enumerator(1, 2) |>>> isEmptyThenRest) must equalTo((false, Seq(1, 2)))
    }

    "be false for a stream with empty and element inputs" in {
      val enum = Enumerator.enumInput(Input.Empty) >>> Enumerator(1, 2)
      await(enum |>>> isEmptyThenRest) must equalTo((false, Seq(1, 2)))
    }

  }

  "Iteratee.takeUpTo and Iteratee.isEmpty" should {

    def process[E](n: Int): Iteratee[E, (Seq[E], Boolean, Seq[E])] = {
      import ExecutionContext.Implicits.global
      for {
        seq1 <- Iteratee.takeUpTo(n)
        emptyAfterSeq1 <- Iteratee.isEmpty
        seq2 <- Iteratee.getChunks
      } yield (seq1, emptyAfterSeq1, seq2)
    }

    "take 0 elements and be empty from 0" in {
      await(Enumerator() |>>> process(0)) must equalTo((Seq(), true, Seq()))
    }

    "take 0 elements from 0 and be empty when asked for 2" in {
      await(Enumerator() |>>> process(2)) must equalTo((Seq(), true, Seq()))
    }

    "take 1 element and not be empty from 2" in {
      await(Enumerator(1, 2) |>>> process(1)) must equalTo((Seq(1), false, Seq(2)))
    }

    "take 2 elements and be empty from 2" in {
      await(Enumerator(1, 2) |>>> process(2)) must equalTo((Seq(1, 2), true, Seq()))
    }

    "take 2 elements and be empty from 2 when asked for 3" in {
      await(Enumerator(1, 2) |>>> process(3)) must equalTo((Seq(1, 2), true, Seq()))
    }

    "skip Input.Empty when taking elements" in {
      val enum = Enumerator(1, 2) >>> Enumerator.enumInput(Input.Empty) >>> Enumerator(3, 4)
      await(enum |>>> process(3)) must equalTo((Seq(1, 2, 3), false, Seq(4)))
    }

  }

  "Iteratee.ignore" should {

    "never throw an OutOfMemoryError when consuming large input" in {
      // Work out how many arrays we'd need to create to trigger an OutOfMemoryError
      val arraySize = 1000000
      val tooManyArrays = (Runtime.getRuntime.maxMemory / arraySize).toInt + 1
      val iterator = Iterator.range(0, tooManyArrays).map(_ => new Array[Byte](arraySize))
      import play.api.libs.iteratee.Execution.Implicits.defaultExecutionContext
      await(Enumerator.enumerate(iterator) |>>> Iteratee.ignore[Array[Byte]]) must_== (())
    }

  }

}
