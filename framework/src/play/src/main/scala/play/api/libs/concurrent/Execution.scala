/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.concurrent

import play.core.{ Execution => CoreExecution }
import scala.concurrent.ExecutionContext

object Execution {

  object Implicits {
    implicit def defaultContext: ExecutionContext = CoreExecution.internalContext
  }

  def defaultContext: ExecutionContext = CoreExecution.internalContext

}

