package play.runsupport

import sbt.FeedbackProvidedException

class ServerStartException(underlying: Throwable) extends FeedbackProvidedException {
  override def getMessage = underlying.getMessage
}
