/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.cache.caffeine

import scala.concurrent.duration.Duration

case class ExpirableCacheValue[V](value: V, durationMaybe: Option[Duration] = None)
