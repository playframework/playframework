/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.utils

/**
 * provides conversion helpers
 */
object Conversions {

  def newMap[A, B](data: (A, B)*) = Map(data: _*)

}