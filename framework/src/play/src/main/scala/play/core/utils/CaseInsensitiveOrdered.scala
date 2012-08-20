package play.core.utils

/**
 * Case Insensitive Ordering
 */

object CaseInsensitiveOrdered extends Ordering[String] {
  def compare(x: String, y: String): Int = x.compareToIgnoreCase(y)
}

