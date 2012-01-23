package play.api.test

import java.io._

import play.api._

import org.openqa.selenium._
import org.openqa.selenium.firefox._
import org.openqa.selenium.htmlunit._

import fr.javafreelance.fluentlenium.core._

import collection.JavaConverters._

case class TestBrowser(webDriver: WebDriver) extends Fluent(webDriver) {
    
    def goTo(url: String) {
        getDriver.get(url)
    }
    
    /**
     * @return url
     */
    override def url = super.url
    
    /**
     * @return current page's source
     */
    override def pageSource = super.pageSource
    
    /**
     * @return cookies 
     */
    def getCookies() =  getDriver().manage().getCookies().asScala
    
    /**
     * @param name
     */
    def getCookieNamed(name: String) = getDriver().manage().getCookieNamed(name)
    
    /**
     * quits the browser
     */
    def quit() {
        getDriver.quit()
    }
    
}

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
    server = new play.core.server.NettyServer(new play.core.TestApplication(application), port, mode = Mode.Test)
  }

  def stop() {
    if (server == null) {
      sys.error("Server is not started!");
    }
    server.stop()
    server = null
  }

}