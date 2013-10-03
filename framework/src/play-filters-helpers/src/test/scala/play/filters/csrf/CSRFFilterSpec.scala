package play.filters.csrf

import play.api.libs.ws.WS.WSRequestHolder
import scala.concurrent.Future
import play.api.libs.ws._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.test.{FakeApplication, TestServer}
import scala.util.Random
import play.api.libs.Crypto

/**
 * Specs for the global CSRF filter
 */
object CSRFFilterSpec extends CSRFCommonSpecs {

  sequential

  import CSRFConf._

  "a CSRF filter also" should {

    // conditions for adding a token
    "not add a token to non GET requests" in {
      buildCsrfAddToken()(_.put(""))(_.status must_== NOT_FOUND)
    }
    "not add a token to GET requests that don't accept HTML" in {
      buildCsrfAddToken()(_.withHeaders(ACCEPT -> "application/json").get())(_.status must_== NOT_FOUND)
    }
    "add a token to GET requests that accept HTML" in {
      buildCsrfAddToken()(_.withHeaders(ACCEPT -> "text/html").get())(_.status must_== OK)
    }

    // extra conditions for not doing a check
    "not check non form bodies" in {
      buildCsrfCheckRequest()(_.post(Json.obj("foo" -> "bar")))(_.status must_== OK)
    }
    "not check safe methods" in {
      buildCsrfCheckRequest()(_.put(Map("foo" -> "bar")))(_.status must_== OK)
    }

    // other
    "feed the body once a check has been done and passes" in {
      withServer(Nil) {
        case _ => CSRFFilter()(Action(
          _.body.asFormUrlEncoded
            .flatMap(_.get("foo"))
            .flatMap(_.headOption)
            .map(Results.Ok(_))
            .getOrElse(Results.NotFound)))
      } {
        val token = Crypto.generateSignedToken
        await(WS.url("http://localhost:" + testServerPort).withSession(TokenName -> token)
          .post(Map("foo" -> "bar", TokenName -> token))).body must_== "bar"
      }
    }
    "feed a not fully buffered body once a check has been done and passes" in running(TestServer(testServerPort, FakeApplication(
      additionalConfiguration = Map("application.secret" -> "foobar", "csrf.body.bufferSize" -> "200"),
      withRoutes = {
        case _ => CSRFFilter()(Action(
          _.body.asFormUrlEncoded
            .flatMap(_.get("foo"))
            .flatMap(_.headOption)
            .map(Results.Ok(_))
            .getOrElse(Results.NotFound)))
      }
    ))) {
      val token = Crypto.generateSignedToken
      val response = await(WS.url("http://localhost:" + testServerPort).withSession(TokenName -> token)
        .withHeaders(CONTENT_TYPE -> "application/x-www-form-urlencoded")
        .post(
          Seq(
            // Ensure token is first so that it makes it into the buffered part
            TokenName -> token,
            // This value must go over the edge of csrf.body.bufferSize
            "longvalue" -> Random.alphanumeric.take(1024).mkString(""),
            "foo" -> "bar"
          ).map(f => f._1 + "=" + f._2).mkString("&")
        )
      )
      response.status must_== OK
      response.body must_== "bar"
    }
    "be possible to instantiate when there is no running application" in {
      CSRFFilter() must beAnInstanceOf[AnyRef]
    }
  }


  def buildCsrfCheckRequest(configuration: (String, String)*) = new CsrfTester {
    def apply[T](makeRequest: (WSRequestHolder) => Future[Response])(handleResponse: (Response) => T) = withServer(configuration) {
      case _ => CSRFFilter()(Action(Results.Ok))
    } {
      handleResponse(await(makeRequest(WS.url("http://localhost:" + testServerPort))))
    }
  }

  def buildCsrfAddToken(configuration: (String, String)*) = new CsrfTester {
    def apply[T](makeRequest: (WSRequestHolder) => Future[Response])(handleResponse: (Response) => T) = withServer(configuration) {
      case _ => CSRFFilter()(Action { implicit req =>
        CSRF.getToken(req).map { token =>
          Results.Ok(token.value)
        } getOrElse Results.NotFound
      })
    } {
      handleResponse(await(makeRequest(WS.url("http://localhost:" + testServerPort))))
    }
  }
}
