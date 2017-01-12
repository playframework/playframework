/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.concurrent

import akka.stream.Supervision
import akka.stream.Supervision.Decider
import play.api.Logger

trait PlaySupervisionProvider {
  def decider: Supervision.Decider
}

class DefaultPlaySupervisionProvider extends PlaySupervisionProvider {

  private val logger = Logger("play.akka")

  override def decider: Decider = { e =>
    logger.error("Unhandled exception in stream", e)
    Supervision.Stop
  }

}