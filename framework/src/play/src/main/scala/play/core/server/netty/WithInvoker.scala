package play.core.server.netty

import play.core.Invoker

trait WithInvoker {
  def invoker: Invoker
}
