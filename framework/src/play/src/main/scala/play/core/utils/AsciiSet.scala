/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.utils

import java.util.{ BitSet => JBitSet }

object AsciiSet {
  def apply(c: Char): AsciiChar = new AsciiChar(c)
  def apply(c: Char, cs: Char*): AsciiSet = cs.foldLeft[AsciiSet](apply(c)) {
    case (acc, c1) => acc ||| apply(c1)
  }

  val empty = new AsciiSet {
    override def get(i: Int): Boolean = false
  }

  object Sets {
    // Core Rules (https://tools.ietf.org/html/rfc5234#appendix-B.1).
    // These are used in HTTP (https://tools.ietf.org/html/rfc7230#section-1.2).
    val Digit: AsciiSet = new AsciiRange('0', '9')
    val Lower: AsciiSet = new AsciiRange('a', 'z')
    val Upper: AsciiSet = new AsciiRange('A', 'Z')
    val Alpha: AsciiSet = Lower ||| Upper
    val AlphaDigit: AsciiSet = Alpha ||| Digit
    val VChar: AsciiSet = new AsciiRange(0x21, 0x7e)
  }
}

trait AsciiSet {
  def get(i: Int): Boolean
  def |||(that: AsciiSet): AsciiUnion = new AsciiUnion(this, that)
  def ---(that: AsciiSet): AsciiDifference = new AsciiDifference(this, that)
  def toBitSet: AsciiBitSet = {
    val bitSet = new JBitSet(256)
    for (i <- (0 until 256)) {
      if (this.get(i)) { bitSet.set(i) }
    }
    new AsciiBitSet(bitSet)
  }
}
/** An inclusive range of ASCII characters */
private[play] final class AsciiRange(first: Int, last: Int) extends AsciiSet {
  assert(first >= 0 && first < last && last < 256)
  override def toString: String = s"(${Integer.toHexString(first)}- ${Integer.toHexString(last)})"
  override def get(i: Int): Boolean = i >= first && i <= last
}
//object AsciiRange {
//  def apply(first: Char, last: Char): AsciiRange = new AsciiRange(first, last)
//}
private[play] final class AsciiChar(i: Int) extends AsciiSet {
  assert(i >= 0 && i < 256)
  override def get(i: Int): Boolean = i == this.i
}
private[play] final class AsciiUnion(a: AsciiSet, b: AsciiSet) extends AsciiSet {
  require(a != null && b != null)
  override def get(i: Int): Boolean = a.get(i) || b.get(i)
}
private[play] final class AsciiDifference(a: AsciiSet, b: AsciiSet) extends AsciiSet {
  require(a != null && b != null)
  override def get(i: Int): Boolean = a.get(i) && !b.get(i)
}
private[play] final class AsciiBitSet private[utils] (bitSet: JBitSet) extends AsciiSet {
  override def get(i: Int): Boolean = bitSet.get(i)
  override def toBitSet: AsciiBitSet = this
}

//
//private[play] class AsciiSet(ranges: Seq[AsciiRange]) {
//  private val bitSet = new JBitSet(256)
//
//  def get(i: Int): Boolean = {
//    assert(i >= 0 && i < 256)
//    bitSet.get(i)
//  }
//  def get(b: Byte): Boolean = get(b & 0xff)
//  def get(c: Char): Boolean = get(c.toInt)
//
//  def set(i: Int): Unit = {
//    assert(i >= 0 && i < 256)
//    bitSet.set(i)
//  }
//  def set(b: Byte): Unit = set(b & 0xff)
//  def set(c: Char): Unit   = set(c.toInt)
//}
//
//private[play] object AsciiSet {
//  def apply(chars: Seq[Char]): AsciiSet = {
//    val as = new AsciiSet()
//    chars.foreach(as.set)
//    as
//  }
//}
//
//private[play] case class AsciiRange(first: Int, last: Int) {
//  def range: Range.Inclusive = first to last
//}
//
//object AsciiRange {
//  def apply(c: Char) = AsciiRange(c, c)
//  val Lower = AsciiRange('a', 'z')
//  val Upper = AsciiRange('A', 'Z')
//  val Digit = AsciiRange('0', '9')
//}