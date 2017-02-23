/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.system

import java.util.concurrent.atomic.AtomicLong

private[play] object RequestIdProvider {
  val requestIDs: AtomicLong = new AtomicLong(0)
}
