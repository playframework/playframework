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
    "allow defining a custom error handler" in {
      val content = contentOf("/error", new onerror.Global())
      content must contain("Custom error page")
      content must contain("foobar")
    }
    "allow defining a custom not found handler" in {
      val content = contentOf("/notfound", new notfound.Global())
      content must contain("Custom not found page")
      content must contain("/notfound")
    }
    "allow defining a custom bad request handler" in {
      val content = contentOf("/withInt/notint", new badrequest.Global())
      content must_== "Don't try to hack the URI!"
    }
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
    override protected def loadRoutes = Some(Routes)
  })) {
    await(WS.url("http://localhost:" + testServerPort + url).get()).body
  }
}

package object controllers {
  object Application {
    def withInt(i: Int) = Action(Results.Ok)
    def normal = Action(Results.Ok("hi"))
    def error = Action(req => throw new RuntimeException("foobar"))
  }
}
