package test

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._

/**
 * add your integration spec here.
 * An integration test will fire up a whole play application in a real (or headless) browser
 */
class IntegrationSpec extends Specification {

  "Application" should {

    "work from within a browser" in new WithBrowser{

      browser.goTo("http://localhost:" + port)

      browser.pageSource must contain("Your new application is ready.")

    }

  }

}
