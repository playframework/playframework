package test

import org.specs2.mutable.Specification
import play.api.test.TestServer
import play.api.test.Helpers._
import org.openqa.selenium.Cookie

object LangSpec extends Specification {

  "The Application’s lang" can {

    "be changed using a cookie" in {
      running(TestServer(3333), HTMLUNIT) { browser =>

        // Ensure it's en to begin with
        browser.goTo("http://localhost:3333/hello")
        browser.title must equalTo ("Hello")

        browser.getDriver.manage.addCookie(new Cookie("PLAY_LANG", "fr-FR"))

        // Make sure we get back fr
        browser.goTo("http://localhost:3333/hello")
        browser.title must equalTo ("Bonjour")        
      }
    }

  }

}