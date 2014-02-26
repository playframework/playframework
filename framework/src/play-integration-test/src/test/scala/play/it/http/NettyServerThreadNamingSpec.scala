package play.it.http

import org.specs2.mutable.Specification

import play.api.test.{DefaultAwaitTimeout, Helpers, FakeApplication, TestServer}
import play.api.mvc.Action
import play.api.mvc.Results.Ok
import play.api.libs.ws.WS

class NettyServerThreadNamingSpec extends Specification with DefaultAwaitTimeout {

  "NettyServer" should {
    "make sure the netty uses the thread names specified by Play" in {
      implicit val app  = FakeApplication(withRoutes = {
        case _ =>
          val threadName = Thread.currentThread.getName
          Action(Ok(threadName))
      })
      Helpers.running(TestServer(9191, app)) {
        val response = Helpers.await(WS.url("http://localhost:9191").get())
        response.body must beMatching("play-netty-worker-\\d$")
      }
    }
  }
}
