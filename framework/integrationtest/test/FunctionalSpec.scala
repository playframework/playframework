package test
import org.specs2.mutable._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.concurrent.Promise
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import play.api.test.IntegrationTest._



object FunctionalSpec extends Specification {

"an Application" should {
  "pass functional test" in {
   withNettyServer{
    val driver = new HtmlUnitDriver()
      driver.get("http://localhost:9000")
      driver.getPageSource must contain ("Hello world")
   }
  }
 }

}
