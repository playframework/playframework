package test

import play.api.test._
import play.api.test.Helpers._

import play.api.libs._

import org.specs2.mutable._

import models._
import models.Protocol._

object FunctionalSpec extends Specification {

  "an Application" should {
    
    "pass functional test" in {
      running(TestServer(9001), HTMLUNIT) { browser =>
        
        browser.goTo("http://localhost:9001")
        browser.pageSource must contain("Hello world")
        
        await(WS.url("http://localhost:9001").get()).body must contain ("Hello world")
        
        await(WS.url("http://localhost:9001/post").post()).body must contain ("POST!")
        
        await(WS.url("http://localhost:9001/json").get()).json.as[User] must equalTo(User(1, "Sadek", List("tea")))
        
        browser.goTo("http://localhost:9001/conf")
        browser.pageSource must contain("This value comes from complex-app's complex1.conf")
        browser.pageSource must contain("override akka:15")
        browser.pageSource must contain("None")
        
        browser.goTo("http://localhost:9001/json_java")
        browser.pageSource must contain ("{\"peter\":\"foo\",\"yay\":\"value\"}")

        browser.goTo("http://localhost:9001/json_from_jsobject")
        browser.pageSource must contain ("{\"blah\":\"foo\"}")

        browser.goTo("http://localhost:9001/headers")
        browser.pageSource must contain("localhost:9001")
        
        // --- Cookies
        
        browser.goTo("http://localhost:9001/json_java")
        browser.getCookies.size must equalTo(0)

        browser.goTo("http://localhost:9001/cookie")
        browser.getCookieNamed("foo").getValue must equalTo("bar")

        browser.goTo("http://localhost:9001/read/foo")
        browser.pageSource must contain("Cookie foo has value: bar")

        browser.goTo("http://localhost:9001/read/bar")
        browser.pageSource must equalTo("")

        browser.goTo("http://localhost:9001/clear/foo")
        browser.getCookies.size must equalTo(0)
        
      }
    }

  }
  
}
