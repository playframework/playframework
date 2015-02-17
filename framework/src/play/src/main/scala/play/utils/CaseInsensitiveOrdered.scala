/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.utils

/**
 * Case Insensitive Ordering
 */

object CaseInsensitiveOrdered extends Ordering[String] {
  def compare(x: String, y: String): Int = x.compareToIgnoreCase(y)
}

