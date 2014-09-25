/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.global

import play.api.test._
import play.api.mvc._
import play.GlobalSettings
import play.core.j.JavaGlobalSettingsAdapter
import play.api.libs.ws.WS

object JavaGlobalSpec extends PlaySpecification {

  "java Global support" should {
    "allow defining a custom global object" in testGlobal(new simple.Global())
    "allow defining on start and on stop methods" in testGlobal(new startstop.Global())
    "allow intercepting requests" in {
      val content = contentOf("/normal", new intercept.Global())
      content must_== "hi"
    }
  }

  def testGlobal(global: GlobalSettings) = running(FakeApplication(
    withGlobal = Some(new JavaGlobalSettingsAdapter(global))
  ))(success)

  def contentOf(url: String, global: GlobalSettings) = running(new TestServer(testServerPort, new FakeApplication(
    withGlobal = Some(new JavaGlobalSettingsAdapter(global))) {
    override lazy val routes = new javaguide.global.Routes(errorHandler, new controllers.Application)
  })) {
    import play.api.Play.current
    await(WS.url("http://localhost:" + testServerPort + url).get()).body
  }
}

package object controllers {
  class Application {
    def normal = Action(Results.Ok("hi"))
  }
}
