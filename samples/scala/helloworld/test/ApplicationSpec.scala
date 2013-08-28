import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification {
  
  "Application" should {
    
    "send 404 on a bad request" in {
      running(FakeApplication()) {
        route(FakeRequest(GET, "/boum")) must beNone        
      }
    }
    
    "render an empty form on index" in {
      running(FakeApplication()) {
        val home = route(FakeRequest(GET, "/")).get
        
        status(home) must equalTo(OK)
        contentType(home) must beSome.which(_ == "text/html")
      }
    }
      "send BadRequest on form error" in {
      running(FakeApplication()) {
        val home = route(FakeRequest(GET, "/hello?name=Bob&repeat=xx")).get
        status(home) must equalTo(BAD_REQUEST)
        contentType(home) must beSome.which(_ == "text/html")
      }
    }
    "say hello" in {
      running(FakeApplication()) {
        val home = route(FakeRequest(GET, "/hello?name=Bob&repeat=10")).get
        status(home) must equalTo(OK)
        contentType(home) must beSome.which(_ == "text/html")
      }
    }
  } 
}
