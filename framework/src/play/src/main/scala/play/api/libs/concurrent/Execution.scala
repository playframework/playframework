/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.concurrent

import play.core.{ Execution => CoreExecution }
import scala.concurrent.ExecutionContext

@deprecated("Please see https://www.playframework.com/documentation/2.6.x/Migration26#play.api.libs.concurrent.Execution-is-deprecated", "2.6.0")
object Execution {

  object Implicits {
    implicit def defaultContext: ExecutionContext = CoreExecution.internalContext
  }

  def defaultContext: ExecutionContext = CoreExecution.internalContext

}

