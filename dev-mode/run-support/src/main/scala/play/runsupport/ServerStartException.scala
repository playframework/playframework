/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.runsupport

class ServerStartException(underlying: Throwable) extends IllegalStateException(underlying) {
  override def getMessage = underlying.getMessage
}
