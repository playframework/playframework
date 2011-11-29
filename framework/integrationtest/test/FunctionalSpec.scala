package test
import org.specs2.mutable._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.concurrent._
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import play.api.test.IntegrationTest._
import com.ning.http.client.providers.netty.NettyResponse
import play.api.WS
import play.api.ws.Response

object FunctionalSpec extends Specification {

"an Application" should {
  "pass functional test" in {
   withNettyServer{
    val driver = new HtmlUnitDriver()
      driver.get("http://localhost:9000")
      driver.getPageSource must contain ("Hello world")
      val resultGet: String = WS.url("http://localhost:9000").get().value match { case r: Redeemed[Response] => r.a.body }
      resultGet must contain ("Hello world")
      val resultPost: String = WS.url("http://localhost:9000/post").post().value match { case r: Redeemed[Response] => r.a.body }
      resultPost must contain ("POST!")

   }
  }
 }

}
