/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2006-2011, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package scala.util.parsing.input1
import scala.util.parsing.input._

/**
 * An interface for streams of values that have positions.
 *
 * @author Martin Odersky, Adriaan Moors
 */
trait Reader[+T] {
  type R <: Reader[T]

  /**
   * If this is a reader over character sequences, the underlying char sequence
   *  If not, throws a <code>NoSuchMethodError</code> exception.
   */
  def source: java.lang.CharSequence =
    throw new NoSuchMethodError("not a char sequence reader")

  def offset: Int =
    throw new NoSuchMethodError("not a char sequence reader")

  /**
   * Returns the first element of the reader
   */
  def first: T

  /**
   * Returns an abstract reader consisting of all elements except the first
   *
   * @return If <code>atEnd</code> is <code>true</code>, the result will be
   *         <code>this'; otherwise, it's a <code>Reader</code> containing
   *         more elements.
   */
  def rest: R

  /*
 
  /** Returns an abstract reader consisting of all elements except the first
   *  <code>n</code> elements.
   */ 
  def drop(n: Int):R = {
   throw new RuntimeException()
    /* var r:R = rest
    var cnt = n-1
    while (cnt > 0) {
      r = r.rest; cnt -= 1
    }
    r*/
  }
*/

  /**
   * The position of the first element in the reader
   */
  def pos: Position

  /**
   * true iff there are no more elements in this reader
   */
  def atEnd: Boolean
}
