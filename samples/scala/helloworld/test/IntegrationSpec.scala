package test

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._

object IntegrationSpec extends Specification {
  
  "Application" should {
    
    "send 404 on a bad request" in {
      running(TestServer(3333), HTMLUNIT) { browser =>
        browser.goTo("http://localhost:3333/")
        ok 
      }
    }
    
  }
  
}