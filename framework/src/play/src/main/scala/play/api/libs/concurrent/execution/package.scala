package play.api.libs.concurrent.execution 

object Implicits {
  implicit lazy val defaultContext: scala.concurrent.ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global: scala.concurrent.ExecutionContext //FIXME use a proper ThreadPool for Play from Conf
}

