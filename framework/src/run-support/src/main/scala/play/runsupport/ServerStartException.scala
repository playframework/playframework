package play.runsupport

class ServerStartException(underlying: Throwable) extends IllegalStateException(underlying) {
  override def getMessage = underlying.getMessage
}
