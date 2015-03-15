/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.filters.csrf

import org.specs2.mutable.Specification
import play.api.libs.ws._
import scala.concurrent.Future
import play.api.mvc.{ Handler, Session }
import play.api.libs.Crypto
import play.api.test.{ FakeApplication, TestServer, PlaySpecification }
import play.api.http.{ ContentTypes, ContentTypeOf, Writeable }
import org.specs2.matcher.MatchResult

/**
 * Specs for functionality that each CSRF filter/action shares in common
 */
trait CSRFCommonSpecs extends Specification with PlaySpecification {

  val TokenName = "csrfToken"
  val HeaderName = "Csrf-Token"

  // This extracts the tests out into different configurations
  def sharedTests(csrfCheckRequest: CsrfTester, csrfAddToken: CsrfTester, generate: => String,
    addToken: (WSRequest, String) => WSRequest,
    getToken: WSResponse => Option[String], compareTokens: (String, String) => MatchResult[Any],
    errorStatusCode: Int) = {
    // accept/reject tokens
    "accept requests with token in query string" in {
      lazy val token = generate
      csrfCheckRequest(req => addToken(req.withQueryString(TokenName -> token), token)
        .post(Map("foo" -> "bar"))
      )(_.status must_== OK)
    }
    "accept requests with token in form body" in {
      lazy val token = generate
      csrfCheckRequest(req => addToken(req, token)
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
      csrfCheckRequest(req => addToken(req, token)
        .withHeaders(HeaderName -> token)
        .post(Map("foo" -> "bar"))
      )(_.status must_== OK)
    }
    "accept requests with nocheck header" in {
      csrfCheckRequest(_.withHeaders(HeaderName -> "nocheck")
        .post(Map("foo" -> "bar"))
      )(_.status must_== OK)
    }
    "accept requests with ajax header" in {
      csrfCheckRequest(_.withHeaders("X-Requested-With" -> "a spoon")
        .post(Map("foo" -> "bar"))
      )(_.status must_== OK)
    }
    "reject requests with different token in body" in {
      csrfCheckRequest(req => addToken(req, generate)
        .post(Map("foo" -> "bar", TokenName -> generate))
      )(_.status must_== errorStatusCode)
    }
    "reject requests with token in session but none elsewhere" in {
      csrfCheckRequest(req => addToken(req, generate)
        .post(Map("foo" -> "bar"))
      )(_.status must_== errorStatusCode)
    }
    "reject requests with token in body but not in session" in {
      csrfCheckRequest(
        _.post(Map("foo" -> "bar", TokenName -> generate))
      )(_.status must_== errorStatusCode)
    }

    // add to response
    "add a token if none is found" in {
      csrfAddToken(_.get()) { response =>
        val token = response.body
        token must not be empty
        val rspToken = getToken(response)
        rspToken must beSome.like {
          case s => compareTokens(token, s)
        }
      }
    }
    "not set the token if already set" in {
      lazy val token = generate
      Thread.sleep(2)
      csrfAddToken(req => addToken(req, token).get()) { response =>
        getToken(response) must beNone
        compareTokens(token, response.body)
        // Ensure that nothing was updated
        response.cookies must beEmpty
      }
    }
  }

  "a CSRF filter" should {

    "work with signed session tokens" in {
      def csrfCheckRequest = buildCsrfCheckRequest(false)
      def csrfAddToken = buildCsrfAddToken()
      def generate = Crypto.generateSignedToken
      def addToken(req: WSRequest, token: String) = req.withSession(TokenName -> token)
      def getToken(response: WSResponse) = {
        val session = response.cookies.find(_.name.exists(_ == Session.COOKIE_NAME)).flatMap(_.value).map(Session.decode)
        session.flatMap(_.get(TokenName))
      }
      def compareTokens(a: String, b: String) = Crypto.compareSignedTokens(a, b) must beTrue

      sharedTests(csrfCheckRequest, csrfAddToken, generate, addToken, getToken, compareTokens, FORBIDDEN)

      "reject requests with unsigned token in body" in {
        csrfCheckRequest(req => addToken(req, generate)
          .post(Map("foo" -> "bar", TokenName -> "foo"))
        )(_.status must_== FORBIDDEN)
      }
      "reject requests with unsigned token in session" in {
        csrfCheckRequest(req => addToken(req, "foo")
          .post(Map("foo" -> "bar", TokenName -> generate))
        ) { response =>
          response.status must_== FORBIDDEN
          response.cookies.find(_.name.exists(_ == Session.COOKIE_NAME)) must beSome.like {
            case cookie => cookie.value must beNone
          }
        }
      }
      "return a different token on each request" in {
        lazy val token = generate
        Thread.sleep(2)
        csrfAddToken(req => addToken(req, token).get()) { response =>
          // it shouldn't be equal, to protect against BREACH vulnerability
          response.body must_!= token
          Crypto.compareSignedTokens(token, response.body) must beTrue
        }
      }
    }

    "work with unsigned session tokens" in {
      def csrfCheckRequest = buildCsrfCheckRequest(false, "play.filters.csrf.token.sign" -> "false")
      def csrfAddToken = buildCsrfAddToken("play.filters.csrf.token.sign" -> "false")
      def generate = Crypto.generateToken
      def addToken(req: WSRequest, token: String) = req.withSession(TokenName -> token)
      def getToken(response: WSResponse) = {
        val session = response.cookies.find(_.name.exists(_ == Session.COOKIE_NAME)).flatMap(_.value).map(Session.decode)
        session.flatMap(_.get(TokenName))
      }
      def compareTokens(a: String, b: String) = a must_== b

      sharedTests(csrfCheckRequest, csrfAddToken, generate, addToken, getToken, compareTokens, FORBIDDEN)
    }

    "work with signed cookie tokens" in {
      def csrfCheckRequest = buildCsrfCheckRequest(false, "play.filters.csrf.cookie.name" -> "csrf")
      def csrfAddToken = buildCsrfAddToken("play.filters.csrf.cookie.name" -> "csrf")
      def generate = Crypto.generateSignedToken
      def addToken(req: WSRequest, token: String) = req.withCookies("csrf" -> token)
      def getToken(response: WSResponse) = response.cookies.find(_.name.exists(_ == "csrf")).flatMap(_.value)
      def compareTokens(a: String, b: String) = Crypto.compareSignedTokens(a, b) must beTrue

      sharedTests(csrfCheckRequest, csrfAddToken, generate, addToken, getToken, compareTokens, FORBIDDEN)
    }

    "work with unsigned cookie tokens" in {
      def csrfCheckRequest = buildCsrfCheckRequest(false, "play.filters.csrf.cookie.name" -> "csrf", "play.filters.csrf.token.sign" -> "false")
      def csrfAddToken = buildCsrfAddToken("play.filters.csrf.cookie.name" -> "csrf", "play.filters.csrf.token.sign" -> "false")
      def generate = Crypto.generateToken
      def addToken(req: WSRequest, token: String) = req.withCookies("csrf" -> token)
      def getToken(response: WSResponse) = response.cookies.find(_.name.exists(_ == "csrf")).flatMap(_.value)
      def compareTokens(a: String, b: String) = a must_== b

      sharedTests(csrfCheckRequest, csrfAddToken, generate, addToken, getToken, compareTokens, FORBIDDEN)
    }

    "work with secure cookie tokens" in {
      def csrfCheckRequest = buildCsrfCheckRequest(false, "play.filters.csrf.cookie.name" -> "csrf", "play.filters.csrf.cookie.secure" -> "true")
      def csrfAddToken = buildCsrfAddToken("play.filters.csrf.cookie.name" -> "csrf", "play.filters.csrf.cookie.secure" -> "true")
      def generate = Crypto.generateSignedToken
      def addToken(req: WSRequest, token: String) = req.withCookies("csrf" -> token)
      def getToken(response: WSResponse) = {
        response.cookies.find(_.name.exists(_ == "csrf")).flatMap { cookie =>
          cookie.secure must beTrue
          cookie.value
        }
      }
      def compareTokens(a: String, b: String) = Crypto.compareSignedTokens(a, b) must beTrue

      sharedTests(csrfCheckRequest, csrfAddToken, generate, addToken, getToken, compareTokens, FORBIDDEN)
    }

    "work with checking failed result" in {
      def csrfCheckRequest = buildCsrfCheckRequest(true, "play.filters.csrf.cookie.name" -> "csrf")
      def csrfAddToken = buildCsrfAddToken("play.filters.csrf.cookie.name" -> "csrf")
      def generate = Crypto.generateSignedToken
      def addToken(req: WSRequest, token: String) = req.withCookies("csrf" -> token)
      def getToken(response: WSResponse) = response.cookies.find(_.name.exists(_ == "csrf")).flatMap(_.value)
      def compareTokens(a: String, b: String) = Crypto.compareSignedTokens(a, b) must beTrue

      sharedTests(csrfCheckRequest, csrfAddToken, generate, addToken, getToken, compareTokens, UNAUTHORIZED)
    }
  }

  trait CsrfTester {
    def apply[T](makeRequest: WSRequest => Future[WSResponse])(handleResponse: WSResponse => T): T
  }

  /**
   * Set up a request that will go through the CSRF action. The action must return 200 OK if successful.
   */
  def buildCsrfCheckRequest(sendUnauthorizedResult: Boolean, configuration: (String, String)*): CsrfTester

  /**
   * Make a request that will have a token generated and added to the request and response if not present.  The request
   * must return the generated token in the body, accessed as if a template had accessed it.
   */
  def buildCsrfAddToken(configuration: (String, String)*): CsrfTester

  implicit class EnrichedRequestHolder(request: WSRequest) {
    def withSession(session: (String, String)*): WSRequest = {
      withCookies(Session.COOKIE_NAME -> Session.encode(session.toMap))
    }
    def withCookies(cookies: (String, String)*): WSRequest = {
      request.withHeaders(COOKIE -> cookies.map(c => c._1 + "=" + c._2).mkString(", "))
    }
  }

  implicit def simpleFormWriteable: Writeable[Map[String, String]] = Writeable.writeableOf_urlEncodedForm.map[Map[String, String]](_.mapValues(v => Seq(v)))
  implicit def simpleFormContentType: ContentTypeOf[Map[String, String]] = ContentTypeOf[Map[String, String]](Some(ContentTypes.FORM))

  def withServer[T](config: Seq[(String, String)])(router: PartialFunction[(String, String), Handler])(block: => T) = running(TestServer(testServerPort, FakeApplication(
    additionalConfiguration = Map(config: _*) ++ Map("play.crypto.secret" -> "foobar"),
    withRoutes = router
  )))(block)
}
