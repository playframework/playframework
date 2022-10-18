/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.system

import java.util.concurrent.atomic.AtomicLong

private[play] object RequestIdProvider {
  private val requestIDs: AtomicLong = new AtomicLong(0)
  def freshId(): Long                = requestIDs.incrementAndGet()
}
