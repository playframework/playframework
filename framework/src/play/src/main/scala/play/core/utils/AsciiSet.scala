/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.utils

import java.util.{ BitSet => JBitSet }

object AsciiSet {

  /** Create a set of a single character. */
  def apply(c: Char): AsciiChar = new AsciiChar(c)
  /** Create a set of more than one character. */
  def apply(c: Char, cs: Char*): AsciiSet = cs.foldLeft[AsciiSet](apply(c)) {
    case (acc, c1) => acc ||| apply(c1)
  }

  /** Some useful sets of ASCII characters. */
  object Sets {
    // Core Rules (https://tools.ietf.org/html/rfc5234#appendix-B.1).
    // These are used in HTTP (https://tools.ietf.org/html/rfc7230#section-1.2).
    val Digit: AsciiSet = new AsciiRange('0', '9')
    val Lower: AsciiSet = new AsciiRange('a', 'z')
    val Upper: AsciiSet = new AsciiRange('A', 'Z')
    val Alpha: AsciiSet = Lower ||| Upper
    val AlphaDigit: AsciiSet = Alpha ||| Digit
    // https://en.wikipedia.org/wiki/ASCII#Printable_characters
    val VChar: AsciiSet = new AsciiRange(0x20, 0x7e)
  }
}

/**
 * A set of ASCII characters. The set should be built out of [[AsciiRange]],
 * [[AsciiChar]], [[AsciiUnion]], etc then converted to an [[AsciiBitSet]]
 * using `toBitSet` for fast querying.
 */
trait AsciiSet {
  /**
   * The internal method used to query for set membership.
   * Doesn't do any bounds checks. Also may be slow, so to
   * query from outside this package you should convert to an
   * [[AsciiBitSet]] using `toBitSet`.
   */
  private[utils] def getInternal(i: Int): Boolean
  /** Join together two sets. */
  def |||(that: AsciiSet): AsciiUnion = new AsciiUnion(this, that)
  /** Convert into an [[AsciiBitSet]] for fast querying. */
  def toBitSet: AsciiBitSet = {
    val bitSet = new JBitSet(256)
    for (i <- (0 until 256)) {
      if (this.getInternal(i)) { bitSet.set(i) }
    }
    new AsciiBitSet(bitSet)
  }
}
/** An inclusive range of ASCII characters */
private[play] final class AsciiRange(first: Int, last: Int) extends AsciiSet {
  assert(first >= 0 && first < last && last < 256)
  override def toString: String = s"(${Integer.toHexString(first)}- ${Integer.toHexString(last)})"
  private[utils] override def getInternal(i: Int): Boolean = i >= first && i <= last
}
private[play] object AsciiRange {
  /** Helper to construct an [[AsciiRange]]. */
  def apply(first: Int, last: Int): AsciiRange = new AsciiRange(first, last)
}
/** A set with a single ASCII character in it. */
private[play] final class AsciiChar(i: Int) extends AsciiSet {
  assert(i >= 0 && i < 256)
  private[utils] override def getInternal(i: Int): Boolean = i == this.i
}
/** A union of two [[AsciiSet]]s. */
private[play] final class AsciiUnion(a: AsciiSet, b: AsciiSet) extends AsciiSet {
  require(a != null && b != null)
  private[utils] override def getInternal(i: Int): Boolean = a.getInternal(i) || b.getInternal(i)
}
/**
 * An efficient representation of a set of ASCII characters. Created by
 * building an [[AsciiSet]] then calling `toBitSet` on it.
 */
private[play] final class AsciiBitSet private[utils] (bitSet: JBitSet) extends AsciiSet {
  final def get(i: Int): Boolean = {
    if (i < 0 || i > 255) throw new IllegalArgumentException(s"Character $i cannot match AsciiSet because it is out of range")
    getInternal(i)
  }
  private[utils] override def getInternal(i: Int): Boolean = bitSet.get(i)
  override def toBitSet: AsciiBitSet = this
}