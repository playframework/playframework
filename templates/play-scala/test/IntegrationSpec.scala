import org.scalatest.TestData
import org.scalatestplus.play._
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test._
import play.api.test.Helpers._

/**
 * add your integration spec here.
 * An integration test will fire up a whole play application in a real (or headless) browser
 */
class IntegrationSpec extends PlaySpec with OneBrowserPerTest with HtmlUnitFactory with ServerProvider {

  private var privateApp: Application = _

  /**
   * Implicit method that returns the `FakeApplication` instance for the current test.
   */
  implicit final def app: Application = synchronized { privateApp }

  /**
   * Creates new instance of `Application` with parameters set to their defaults. Override this method if you
   * need an `Application` created with non-default parameter values.
   */
  def newAppForTest(testData: TestData): Application = GuiceApplicationBuilder().build()

  lazy val port: Int = Helpers.testServerPort

  override def withFixture(test: NoArgTest) = {
    synchronized { privateApp = newAppForTest(test) }
    Helpers.running(TestServer(port, app)) {
      super.withFixture(test)
    }
  }

  "Application" should {

    "work from within a browser" in {

      go to ("http://localhost:" + port)

      pageSource must include ("Your new application is ready.")
    }
  }
}
