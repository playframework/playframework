/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.system

import java.util.concurrent.atomic.AtomicLong

private[play] object RequestIdProvider {
  private val requestIDs: AtomicLong = new AtomicLong(0)
  def freshId(): Long = requestIDs.incrementAndGet()
}
