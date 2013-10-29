/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.system

import play.api.test._
import play.api.mvc.Handler

object HandlerExecutorSpec extends PlaySpecification {
  "handler executor usage" should {
    "fail gracefully if no handler executor is found" in withServer(FakeApplication(
      withRoutes = {
        case _ => new Handler() {}
      }
    )) { implicit port =>
      val response = await(wsUrl("/foo").get())
      response.status must_== 500
    }
  }

  def withServer[T](app: FakeApplication)(block: Port => T) = {
    val port = testServerPort
    running(TestServer(port, app)) {
      block(port)
    }
  }
}
