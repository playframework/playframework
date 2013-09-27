package test

import org.specs2.mutable.Specification
import play.api.test.TestServer
import play.api.test.Helpers._

object LangSpec extends Specification {

  "The Application’s lang" can {

    "be changed durably" in {
      running(TestServer(3333), HTMLUNIT) { browser =>
        // Set the lang to fr
        browser.goTo("http://localhost:3333/lang/fr")
        browser.pageSource must equalTo ("fr")
        browser.goTo("http://localhost:3333/hello")
        browser.pageSource must equalTo ("Bonjour")

        // Change it for en
        browser.goTo("http://localhost:3333/lang/en")
        browser.pageSource must equalTo ("en")
        browser.goTo("http://localhost:3333/hello")
        browser.pageSource must equalTo ("Hello")

        // Try to change it for an unsupported lang
        browser.goTo("http://localhost:3333/lang/ja")
        browser.pageSource must equalTo ("en")
        browser.goTo("http://localhost:3333/hello")
        browser.pageSource must equalTo ("Hello")

        // Change back to default lang
        browser.goTo("http://localhost:3333/unsetLang")
        browser.pageSource must equalTo ("en")
      }
    }

  }

}
