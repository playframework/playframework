package play.filters.csrf

import org.specs2.mutable.Specification
import play.api.libs.ws.WS.WSRequestHolder
import play.api.libs.ws.Response
import scala.concurrent.Future
import play.api.mvc.{Handler, Session}
import play.api.libs.Crypto
import play.api.test.{FakeApplication, TestServer, PlaySpecification}
import play.api.http.{ContentTypes, ContentTypeOf, Writeable}

/**
 * Specs for functionality that each CSRF filter/action shares in common
 */
trait CSRFCommonSpecs extends Specification with PlaySpecification {

  import CSRFConf._

  "a CSRF filter" should {
    // accept/reject tokens
    "accept requests with token in query string" in {
      lazy val token = generate
      csrfCheckRequest(_.withQueryString(TokenName -> token)
        .withSession(TokenName -> token)
        .post(Map("foo" -> "bar"))
      )(_.status must_== OK)
    }
    "accept requests with token in form body" in {
      lazy val token = generate
      csrfCheckRequest(_.withSession(TokenName -> token)
        .post(Map("foo" -> "bar", TokenName -> token))
      )(_.status must_== OK)
    }
    /* TODO: write multipart/form-data Writable
    "accept requests with a session token and token in multipart body" in {
      lazy val token = generate
      makeRequest(_.withSession(TokenName -> token)
        .post(Map("foo" -> "bar", TokenName -> token))
      ).status must_== OK
    }
    */
    "accept requests with token in header" in {
      lazy val token = generate
      csrfCheckRequest(_.withSession(TokenName -> token)
        .withHeaders(HeaderName -> token)
        .post(Map("foo" -> "bar"))
      )(_.status must_== OK)
    }
    "accept requests with nocheck header" in {
      csrfCheckRequest(_.withHeaders(HeaderName -> HeaderNoCheck)
        .post(Map("foo" -> "bar"))
      )(_.status must_== OK)
    }
    "accept requests with ajax header" in {
      csrfCheckRequest(_.withHeaders("X-Requested-With" -> "a spoon")
        .post(Map("foo" -> "bar"))
      )(_.status must_== OK)
    }
    "reject requests with different token in body" in {
      csrfCheckRequest(_.withSession(TokenName -> generate)
        .post(Map("foo" -> "bar", TokenName -> generate))
      )(_.status must_== FORBIDDEN)
    }
    "reject requests with unsigned token in body" in {
      csrfCheckRequest(_.withSession(TokenName -> generate)
        .post(Map("foo" -> "bar", TokenName -> "foo"))
      )(_.status must_== FORBIDDEN)
    }
    "reject requests with unsigned token in session" in {
      csrfCheckRequest(_.withSession(TokenName -> "foo")
        .post(Map("foo" -> "bar", TokenName -> generate))
      )(_.status must_== FORBIDDEN)
    }
    "reject requests with token in session but none elsewhere" in {
      csrfCheckRequest(_.withSession(TokenName -> generate)
        .post(Map("foo" -> "bar"))
      )(_.status must_== FORBIDDEN)
    }
    "reject requests with token in body but not in session" in {
      csrfCheckRequest(
        _.post(Map("foo" -> "bar", TokenName -> generate))
      )(_.status must_== FORBIDDEN)
    }

    // add to response
    "add a token if none is found" in {
      csrfAddToken(_.get()) { response =>
        val token = response.body
        token must not be empty
        val session = response.cookies.find(_.name.exists(_ == Session.COOKIE_NAME)).flatMap(_.value).map(Session.decode)
        session must beSome
        val sessionToken = session.flatMap(_.get(TokenName))
        sessionToken must beSome.like {
          case s => Crypto.compareSignedTokens(s, token) must beTrue
        }
      }
    }
    "not set the token if already set" in {
      lazy val token = generate
      Thread.sleep(2)
      csrfAddToken(_.withSession(TokenName -> token).get()) { response =>
        // it shouldn't be equal, to protect against BREACH vulnerability
        response.body must_!= token
        Crypto.compareSignedTokens(token, response.body) must beTrue
        // Ensure that the session wasn't updated
        response.cookies must beEmpty
      }
    }
  }

  def generate = Crypto.generateSignedToken

  /**
   * Set up a request that will go through the CSRF action. The action must return 200 OK if successful.
   */
  def csrfCheckRequest[T](makeRequest: WSRequestHolder => Future[Response])(handleResponse: Response => T): T

  /**
   * Make a request that will have a token generated and added to the request and response if not present.  The request
   * must return the generated token in the body, accessed as if a template had accessed it.
   */
  def csrfAddToken[T](makeRequest: WSRequestHolder => Future[Response])(handleResponse: Response => T): T

  implicit class EnrichedRequestHolder(request: WSRequestHolder) {
    def withSession(session: (String, String)*): WSRequestHolder = {
      withCookies(Session.COOKIE_NAME -> Session.encode(session.toMap))
    }
    def withCookies(cookies: (String, String)*): WSRequestHolder = {
      request.withHeaders(COOKIE -> cookies.map(c => c._1 + "=" + c._2).mkString(", "))
    }
  }

  implicit def simpleFormWriteable: Writeable[Map[String, String]] = Writeable.writeableOf_urlEncodedForm.map[Map[String, String]](_.mapValues(v => Seq(v)))
  implicit def simpleFormContentType: ContentTypeOf[Map[String, String]] = ContentTypeOf[Map[String, String]](Some(ContentTypes.FORM))

  def withServer[T](router: PartialFunction[(String, String), Handler])(block: => T) = running(TestServer(testServerPort, FakeApplication(
    additionalConfiguration = Map("application.secret" -> "foobar"),
    withRoutes = router
  )))(block)
}
