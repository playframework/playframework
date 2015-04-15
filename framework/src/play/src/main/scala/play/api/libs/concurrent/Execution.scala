/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
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

