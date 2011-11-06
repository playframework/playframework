package test
import play.api.test.IntegrationTestRunner
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.specs2.matcher.MustThrownMatchers._

/**
* run it via play it:run
*/
object IntegrationTest extends IntegrationTestRunner {

    def test = {
      val driver = new HtmlUnitDriver()
      driver.get("http://localhost:9000")
      driver.getPageSource.toString must contain ("Hello world")
      //should not work
      //driver.getPageSource.toString must contain ("1Hello")
    }
}

