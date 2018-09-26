/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.http

/**
 * A port.  This class is defined so that ports can be passed around implicitly.
 */
class Port(val value: Int) extends AnyVal {
  override def toString = value.toString
}
