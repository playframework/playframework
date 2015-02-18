/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.iteratee

import java.lang.{ Boolean => JBoolean }
import java.util.concurrent.LinkedBlockingQueue
import play.libs.F
import scala.concurrent.duration._

/**
 * Implementation of Concurrent.Channel for testing.
 * Can be queried for expected chunks and end calls.
 */
class TestChannel[A](defaultTimeout: Duration) extends Concurrent.Channel[A] {
  def this() = this(5.seconds)

  private val chunks = new LinkedBlockingQueue[Input[A]]
  private val ends = new LinkedBlockingQueue[Option[Throwable]]

  def push(chunk: Input[A]): Unit = {
    chunks.offer(chunk)
  }

  def end(e: Throwable): Unit = {
    ends.offer(Some(e))
  }

  def end(): Unit = {
    ends.offer(None)
  }

  private def takeChunk(timeout: Duration): Input[A] = {
    if (timeout.isFinite)
      chunks.poll(timeout.length, timeout.unit)
    else
      chunks.take
  }

  def expect(expected: A): A = expect(expected, defaultTimeout)

  def expect(expected: A, timeout: Duration): A = expect(expected, timeout, _ == _)

  def expect(expected: A, test: F.Function2[A, A, JBoolean]): A = expect(expected, defaultTimeout, test)

  def expect(expected: A, timeout: Duration, test: F.Function2[A, A, JBoolean]): A = expect(expected, timeout, test.apply(_, _))

  def expect(expected: A, test: (A, A) => Boolean): A = expect(expected, defaultTimeout, test)

  def expect(expected: A, timeout: Duration, test: (A, A) => Boolean): A = {
    takeChunk(timeout) match {
      case null =>
        throw new AssertionError(s"timeout ($timeout) waiting for $expected")
      case Input.El(input) =>
        assert(test(expected, input), s"expected [$expected] but found [$input]")
        input
      case other =>
        throw new AssertionError(s"expected Input.El [$expected] but found [$other]")
    }
  }

  def expectEOF(): Input[A] = expectEOF(defaultTimeout)

  def expectEOF(timeout: Duration): Input[A] = {
    takeChunk(timeout) match {
      case null => throw new AssertionError(s"timeout ($timeout) waiting for EOF")
      case eof @ Input.EOF => eof
      case other => throw new AssertionError(s"expected EOF but found [$other]")
    }
  }

  private def takeEnd(timeout: Duration): Option[Throwable] = {
    if (timeout.isFinite)
      ends.poll(timeout.length, timeout.unit)
    else
      ends.take
  }

  def expectEnd[T](expected: Class[T]): T = expectEnd(expected, defaultTimeout)

  def expectEnd[T](expected: Class[T], timeout: Duration): T = {
    val end = takeEnd(timeout)
    assert(end ne null, s"timeout ($timeout) waiting for end with failure [$expected]")
    end match {
      case Some(throwable) =>
        assert(expected isInstance throwable, s"expected end with failure [$expected] but found [$throwable]")
        throwable.asInstanceOf[T]
      case None =>
        throw new AssertionError(s"expected end with failure [$expected] but found end (without failure)")
    }
  }

  def expectEnd(): Unit = expectEnd(defaultTimeout)

  def expectEnd(timeout: Duration): Unit = {
    val end = takeEnd(timeout)
    assert(end ne null, s"timeout ($timeout) waiting for end")
    if (end.isDefined)
      throw new AssertionError(s"expected end (without failure) but found [${end.get}]")
  }

  def expectEmpty(): Unit = {
    assert(chunks.isEmpty, s"expected empty chunks but found $chunks")
    assert(ends.isEmpty, s"expected empty ends but found $ends")
  }
}
