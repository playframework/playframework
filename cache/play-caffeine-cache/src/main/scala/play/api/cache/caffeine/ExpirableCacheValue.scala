/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.cache.caffeine

import akka.annotation.InternalApi

import scala.concurrent.duration.Duration

@InternalApi
private[caffeine] case class ExpirableCacheValue[V](value: V, durationMaybe: Option[Duration] = None)
