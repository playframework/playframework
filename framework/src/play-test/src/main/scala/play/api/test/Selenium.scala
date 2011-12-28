package play.api.test

import java.io._

import play.api._

import org.openqa.selenium._
import org.openqa.selenium.firefox._
import org.openqa.selenium.htmlunit._

case class TestBrowser(webDriver: WebDriver) extends play.test.TestBrowser(webDriver)

object TestBrowser {

  def default() = TestBrowser(new HtmlUnitDriver)
  def firefox() = TestBrowser(new FirefoxDriver)
  def of[WEBDRIVER <: WebDriver](webDriver: Class[WEBDRIVER]) = TestBrowser(webDriver.newInstance)

}

case class TestServer(port: Int, application: FakeApplication = FakeApplication()) {

  var server: play.core.server.NettyServer = _

  def start() {
    if (server != null) {
      sys.error("Server already started!")
    }
    server = new play.core.server.NettyServer(new play.core.TestApplication(application), port, Mode.Test)
  }

  def stop() {
    if (server == null) {
      sys.error("Server is not started!");
    }
    server.stop()
  }

}