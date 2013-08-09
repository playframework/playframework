package play.api.libs.concurrent

object Execution {

  object Implicits {
    implicit lazy val defaultContext: scala.concurrent.ExecutionContext =
      play.core.Invoker.executionContext: scala.concurrent.ExecutionContext
  }

  val defaultContext = Implicits.defaultContext

}

