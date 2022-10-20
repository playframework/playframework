/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.http

/**
 * A port.  This class is defined so that ports can be passed around implicitly.
 */
class Port(val value: Int) extends AnyVal {
  override def toString = value.toString
}
