package test

import org.specs2.mutable.Specification
import play.api.test._
import play.api.test.Helpers._

class FunctionalSpec extends Specification {

  "an Application" should {
  
    "pass functional test" in {
   
      running(TestServer(9001), HTMLUNIT) { browser =>
        browser.goTo("http://localhost:9001")
        browser.waitUntil[Boolean]{
          browser.pageSource.contains("Pi")
        } must equalTo(true)
      }
   
    }
    
  }

}
