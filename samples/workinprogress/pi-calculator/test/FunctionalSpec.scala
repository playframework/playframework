package test
import org.specs2.mutable._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.concurrent._
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import play.api.test.IntegrationTest._
import com.ning.http.client.providers.netty.NettyResponse
import play.api.libs.WS

object FunctionalSpec extends Specification {

"an Application" should {
  "pass functional test" in {
   withNettyServer{
    val driver = new HtmlUnitDriver()
      driver.get("http://localhost:9001")
      driver.getPageSource must contain ("Pi")
   }
  }
 }

}
