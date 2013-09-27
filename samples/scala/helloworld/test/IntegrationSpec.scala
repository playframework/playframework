import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class IntegrationSpec extends Specification {
  
  "Application" should {
    
    "work from within a browser" in {
      running(TestServer(3333), HTMLUNIT) { browser =>
        browser.goTo("http://localhost:3333/")
        
        browser.waitUntil[Boolean]{
          browser.pageSource contains ("Hello world")
        }

        browser.$("h1").first.getText.contains("Configure your 'Hello world':")

        browser.$("#name").text("Bob")
        browser.$("#submit").click()
        
        browser.$("dl.error").size must equalTo(1)
        browser.$("dl#repeat_field dd.error").first.getText must equalTo("Numeric value expected")
        browser.$("#name").first.getValue must equalTo("Bob")
        
        browser.$("#repeat").text("xxx")
        browser.$("#submit").click()
        
        browser.$("dl.error").size must equalTo(1)
        browser.$("dl#repeat_field dd.error").first.getText must equalTo("Numeric value expected")
        browser.$("#name").first.getValue must equalTo("Bob")
        browser.$("#repeat").first.getValue must equalTo("xxx")
        
        browser.$("#name").text("")
        browser.$("#submit").click()
        
        browser.$("dl.error").size must equalTo(2)
        browser.$("dl#name_field dd.error").first.getText must equalTo("This field is required")
        browser.$("dl#repeat_field dd.error").first.getText must equalTo("Numeric value expected")
        browser.$("#name").first.getValue must equalTo("")
        browser.$("#repeat").first.getValue must equalTo("xxx")
        
        browser.$("#name").text("Bob")
        browser.$("#repeat").text("10")
        browser.$("#submit").click()
        
        browser.$("header a").first.getText must equalTo("Here is the result:")
        
        val items = browser.$("section ul li")
        
        items.size must equalTo(10)
        items.get(0).getText must equalTo("Hello Bob!")
        
        browser.$("p.buttons a").click()
        
        browser.$("h1").first.getText must equalTo("Configure your 'Hello world':")
      }
    }
    
  }
  
}
