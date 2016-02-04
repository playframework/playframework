/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.system

import java.util.concurrent.atomic.AtomicLong

private[play] object RequestIdProvider {
  val requestIDs: AtomicLong = new AtomicLong(0)
}
