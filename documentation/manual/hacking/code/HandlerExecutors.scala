package hacking.handlerexecutors

import play.api.test._
import play.core.system.HandlerExecutorContext
import play.api.mvc._
import play.core.server.NettyServer
import play.api.libs.iteratee.Done
import scala.util.{Random, Try}
import play.core.server.netty.NettyEssentialActionExecutor
import play.api.Mode
import play.api.libs.ws.WS

object HandlerExecutors extends PlaySpecification {

  "handler executors" should {
    "allow defining a custom one" in {

      //#closeable-action
      def myAction = CloseableHandler { closeable =>
        EssentialAction { rh =>
          if (rh.headers("Content-Length").toInt > 1048576) {
            // Close immediately if the request body is greater than 1MB, don't consume it
            closeable.close()
          }
          Done(Results.Ok)
        }
      }
      //#closeable-action

      // Use a custom netty server so that we can use a custom handler
      val server = new NettyServer(new play.core.TestApplication(FakeApplication(
        withRoutes = {
          case _ =>
            myAction
        }
      )), Some(testServerPort), mode = Mode.Test,
        handlerExecutors = Seq(NettyCloseableHandlerExecutor, NettyEssentialActionExecutor))

      try {
        await(wsUrl("/foo")(testServerPort).post("Hello World")).status must_== 200
        Try(await(wsUrl("/foo")(testServerPort).withHeaders("Content-Length" -> "2000000").get())) must beFailedTry
      } finally {
        server.stop()
      }


    }
  }

}

//#closeable-handler
import java.io.Closeable
import play.api.mvc.Handler

trait CloseableHandler extends Handler with Function[Closeable, Handler]

object CloseableHandler {
  def apply(action: Closeable => Handler): CloseableHandler = new CloseableHandler {
    def apply(closeable: Closeable): Handler = action(closeable)
  }
}
//#closeable-handler

//#closeable-handler-executor
import play.core.server.netty.NettyBackendRequest
import play.core.system.HandlerExecutor

object NettyCloseableHandlerExecutor extends HandlerExecutor[NettyBackendRequest] {

  def apply(context: HandlerExecutorContext[NettyBackendRequest], request: RequestHeader,
            backend: NettyBackendRequest, handler: Handler) = handler match {

    case closeableHandler: CloseableHandler =>

      // Implement a closeable that closes the Netty channel when called
      val closeable = new Closeable() {
        def close() = backend.context.getChannel.close()
      }

      // Invoke the closeable handler which returns a nested handler
      val nestedHandler = closeableHandler(closeable)

      // Get the executor context to handle the nested handler
      context(request, backend, nestedHandler)

    case _ => None
  }
}
//#closeable-handler-executor