/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.tests.scalatest.oneserverpertest

import play.api.test._
import org.scalatest._
import org.scalatestplus.play._
import play.api.test.Helpers._
import play.api.libs.ws._
import play.api.mvc._
import Results._

// #scalafunctionaltest-oneserverpertest
class ExampleSpec extends PlaySpec with OneServerPerTest {

  // Override newAppForTest if you need a FakeApplication with other than
  // default parameters.
  override def newAppForTest(testData: TestData): FakeApplication =
    new FakeApplication(
      additionalConfiguration = Map("ehcacheplugin" -> "disabled"),
      withRoutes = {
        case ("GET", "/") => Action { Ok("ok") }
      }
    )

  "The OneServerPerTest trait" must {
    "test server logic" in {
      val myPublicAddress =  s"localhost:$port"
      val testPaymentGatewayURL = s"http://$myPublicAddress"
      // The test payment gateway requires a callback to this server before it returns a result...
      val callbackURL = s"http://$myPublicAddress/callback"
      // await is from play.api.test.FutureAwaits
      val response = await(WS.url(testPaymentGatewayURL).withQueryString("callbackURL" -> callbackURL).get())

      response.status mustBe (OK)
    }
  }
}
// #scalafunctionaltest-oneserverpertest
