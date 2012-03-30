package test

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._

class ApplicationSpec extends Specification {
  
  "Application" should {
    
    "send 404 on a bad request" in {
      running(FakeApplication()) {
        routeAndCall(FakeRequest(GET, "/boum")) must beNone        
      }
    }
    
    "render an empty form on index" in {
      running(FakeApplication()) {
        val home = routeAndCall(FakeRequest(GET, "/")).get
        
        status(home) must equalTo(OK)
        contentType(home) must beSome.which(_ == "text/html")
      }
    }
      "send BadRequest on form error" in {
      running(FakeApplication()) {
        val home = routeAndCall(FakeRequest(GET, "/hello?name=Bob&repeat=xx")).get
        status(home) must equalTo(BAD_REQUEST)
        contentType(home) must beSome.which(_ == "text/html")
      }
    }
    "say hello" in {
      running(FakeApplication()) {
        val home = routeAndCall(FakeRequest(GET, "/hello?name=Bob&repeat=10")).get
        status(home) must equalTo(OK)
        contentType(home) must beSome.which(_ == "text/html")
      }
    }
  } 
}
