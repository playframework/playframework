/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.utils

/**
 * provides conversion helpers
 */
object Conversions {

  def newMap[A, B](data: (A, B)*) = Map(data: _*)

}
