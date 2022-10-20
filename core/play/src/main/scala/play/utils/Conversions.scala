/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.utils

/**
 * provides conversion helpers
 */
object Conversions {
  def newMap[A, B](data: (A, B)*) = Map(data: _*)
}
