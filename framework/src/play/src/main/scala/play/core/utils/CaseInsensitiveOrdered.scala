/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.utils

/**
 * Case Insensitive Ordering. We first compare by length, then
 * use a case insensitive lexicographic order. This allows us to
 * use a much faster length comparison before we even start looking
 * at the content of the strings.
 */
private[play] object CaseInsensitiveOrdered extends Ordering[String] {
  def compare(x: String, y: String): Int = {
    val xl = x.length
    val yl = y.length
    if (xl < yl) -1 else if (xl > yl) 1 else x.compareToIgnoreCase(y)
  }
}

