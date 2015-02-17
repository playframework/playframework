/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.concurrent

import play.core.Invoker
import scala.concurrent.ExecutionContext

object Execution {

  object Implicits {
    implicit def defaultContext: ExecutionContext = Execution.defaultContext
  }

  def defaultContext: ExecutionContext = Invoker.executionContext

}

