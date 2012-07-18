package play.api.libs.concurrent.execution

object `package` {

  implicit val defaultContext: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global //FIXME use a proper ThreadPool for Play from Conf

}
