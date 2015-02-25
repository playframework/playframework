/*
 * Copyright (C) 2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.system

import java.util.concurrent.atomic.AtomicLong;

private[play] object RequestIdProvider {
  val requestIDs: AtomicLong = new AtomicLong(0)
}
