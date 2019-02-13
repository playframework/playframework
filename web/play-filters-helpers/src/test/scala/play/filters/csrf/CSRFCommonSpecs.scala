/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.csrf

import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification
import play.api.Application
import play.api.http.{ ContentTypeOf, ContentTypes, SecretConfiguration, SessionConfiguration }
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.crypto._
import play.api.libs.ws._
import play.api.mvc.{ DefaultSessionCookieBaker, Handler, SessionCookieBaker }
import play.api.test.{ PlaySpecification, TestServer }
import play.filters.csrf.CSRF.{ SignedTokenProvider, UnsignedTokenProvider }

import scala.concurrent.Future
import scala.reflect.ClassTag

/**
 * Specs for functionality that each CSRF filter/action shares in common
 */
trait CSRFCommonSpecs extends Specification with PlaySpecification {

  val TokenName = "csrfToken"
  val HeaderName = "Csrf-Token"
  val CRYPTO_SECRET = "ad31779d4ee49d5ad5162bf1429c32e2e9933f3b"

  def inject[T: ClassTag](implicit app: Application) = app.injector.instanceOf[T]

  val cookieSigner = new DefaultCookieSigner(SecretConfiguration(CRYPTO_SECRET))
  val tokenSigner = new DefaultCSRFTokenSigner(cookieSigner, java.time.Clock.systemUTC())
  val signedTokenProvider = new SignedTokenProvider(tokenSigner)
  val unsignedTokenProvider = new UnsignedTokenProvider(tokenSigner)
  val sessionCookieBaker: SessionCookieBaker = new DefaultSessionCookieBaker(
    SessionConfiguration(),
    SecretConfiguration(secret = CRYPTO_SECRET),
    cookieSigner
  )

  val Boundary = "83ff53821b7c"
  def multiPartFormDataBody(tokenName: String, tokenValue: String) = {
    s"""--$Boundary
      |Content-Disposition: form-data; name="foo"; filename="foo.txt"
      |Content-Type: application/octet-stream
      |
      |hello foo
      |--$Boundary
      |Content-Disposition: form-data; name="$tokenName"
      |
      |$tokenValue
      |--$Boundary--""".stripMargin.replaceAll("\r?\n", "\r\n")
  }

  // This extracts the tests out into different configurations
  def sharedTests(csrfCheckRequest: CsrfTester, csrfAddToken: CsrfTester, generate: => String,
    addToken: (WSRequest, String) => WSRequest,
    getToken: WSResponse => Option[String], compareTokens: (String, String) => MatchResult[Any],
    errorStatusCode: Int) = {
    // accept/reject tokens
    "accept requests with token in query string" in {
      lazy val token = generate
      csrfCheckRequest(req => addToken(req.withQueryStringParameters(TokenName -> token), token)
        .post(Map("foo" -> "bar"))
      )(_.status must_== OK)
    }
    "accept requests with token in form body" in {
      lazy val token = generate
      csrfCheckRequest(req => addToken(req, token)
        .post(Map("foo" -> "bar", TokenName -> token))
      )(_.status must_== OK)
    }
    "accept requests with a session token and token in multipart body" in {
      lazy val token = generate
      csrfCheckRequest(req => addToken(req, token)
        .addHttpHeaders("Content-Type" -> s"multipart/form-data; boundary=$Boundary")
        .post(multiPartFormDataBody(TokenName, token))
      )(_.status must_== OK)
    }
    "accept requests with token in header" in {
      lazy val token = generate
      csrfCheckRequest(req => addToken(req, token)
        .addHttpHeaders(HeaderName -> token)
        .post(Map("foo" -> "bar"))
      )(_.status must_== OK)
    }
    "reject requests with nocheck header" in {
      csrfCheckRequest(_.withCookies("foo" -> "bar")
        .addHttpHeaders(HeaderName -> "nocheck")
        .post(Map("foo" -> "bar"))
      )(_.status must_== errorStatusCode)
    }
    "reject requests with ajax header" in {
      csrfCheckRequest(_.withCookies("foo" -> "bar")
        .addHttpHeaders("X-Requested-With" -> "a spoon")
        .post(Map("foo" -> "bar"))
      )(_.status must_== errorStatusCode)
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
        _.withSession("foo" -> "bar")
          .post(Map("foo" -> "bar", TokenName -> generate))
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
    "add a cookie token if configured to use a cookie even if a session token already exists" in {
      buildCsrfAddToken(
        "play.filters.csrf.cookie.name" -> "csrf",
        "play.filters.csrf.token.name" -> "csrf"
      )({ req =>
          req
            .addHttpHeaders(ACCEPT -> "text/html")
            .withSession("csrf" -> signedTokenProvider.generateToken)
            .get()
        })(_.cookies must not be empty)
    }
  }

  "a CSRF filter" should {

    "work with signed session tokens" in {

      def csrfCheckRequest = buildCsrfCheckRequest(sendUnauthorizedResult = false)
      def csrfAddToken = buildCsrfAddToken()
      def generate = signedTokenProvider.generateToken
      def addToken(req: WSRequest, token: String) = req.withSession(TokenName -> token)
      def getToken(response: WSResponse) = {
        val session = response.cookies.find(_.name == sessionCookieBaker.COOKIE_NAME).map(_.value).map(sessionCookieBaker.decode)
        session.flatMap(_.get(TokenName))
      }
      def compareTokens(a: String, b: String) = signedTokenProvider.compareTokens(a, b) must beTrue

      sharedTests(csrfCheckRequest, csrfAddToken, generate, addToken, getToken, compareTokens, FORBIDDEN)

      "reject requests with unsigned token in body" in {
        csrfCheckRequest(req =>
          addToken(req, generate).post(Map("foo" -> "bar", TokenName -> "foo"))
        )(_.status must_== FORBIDDEN)
      }
      "reject requests with unsigned token in session" in {
        csrfCheckRequest(req =>
          addToken(req, "foo").post(Map("foo" -> "bar", TokenName -> generate))
        ) { response =>
          response.status must_== FORBIDDEN
          response.cookie(sessionCookieBaker.COOKIE_NAME) must beSome.like {
            case cookie => cookie.value must ===("")
          }
        }
      }
      "return a different token on each request" in {
        lazy val token = generate
        Thread.sleep(2)
        csrfAddToken(req => addToken(req, token).get()) { response =>
          // it shouldn't be equal, to protect against BREACH vulnerability
          response.body must_!= token
          signedTokenProvider.compareTokens(token, response.body) must beTrue
        }
      }
    }

    "work with unsigned session tokens" in {
      def csrfCheckRequest = buildCsrfCheckRequest(false, "play.filters.csrf.token.sign" -> "false")
      def csrfAddToken = buildCsrfAddToken("play.filters.csrf.token.sign" -> "false")
      def generate = unsignedTokenProvider.generateToken
      def addToken(req: WSRequest, token: String) = req.withSession(TokenName -> token)
      def getToken(response: WSResponse) = {
        val session = response.cookie(sessionCookieBaker.COOKIE_NAME).map(_.value).map(sessionCookieBaker.decode)
        session.flatMap(_.get(TokenName))
      }
      def compareTokens(a: String, b: String) = a must_== b

      sharedTests(csrfCheckRequest, csrfAddToken, generate, addToken, getToken, compareTokens, FORBIDDEN)
    }

    "work with signed cookie tokens" in {
      def csrfCheckRequest = buildCsrfCheckRequest(false, "play.filters.csrf.cookie.name" -> "csrf")
      def csrfAddToken = buildCsrfAddToken("play.filters.csrf.cookie.name" -> "csrf")
      def generate = signedTokenProvider.generateToken
      def addToken(req: WSRequest, token: String) = req.withCookies("csrf" -> token)
      def getToken(response: WSResponse) = response.cookie("csrf").map(_.value)
      def compareTokens(a: String, b: String) = signedTokenProvider.compareTokens(a, b) must beTrue

      sharedTests(csrfCheckRequest, csrfAddToken, generate, addToken, getToken, compareTokens, FORBIDDEN)
    }

    "work with unsigned cookie tokens" in {
      def csrfCheckRequest = buildCsrfCheckRequest(false, "play.filters.csrf.cookie.name" -> "csrf", "play.filters.csrf.token.sign" -> "false")
      def csrfAddToken = buildCsrfAddToken("play.filters.csrf.cookie.name" -> "csrf", "play.filters.csrf.token.sign" -> "false")
      def generate = unsignedTokenProvider.generateToken
      def addToken(req: WSRequest, token: String) = req.withCookies("csrf" -> token)
      def getToken(response: WSResponse) = response.cookie("csrf").map(_.value)
      def compareTokens(a: String, b: String) = a must_== b

      sharedTests(csrfCheckRequest, csrfAddToken, generate, addToken, getToken, compareTokens, FORBIDDEN)
    }

    "work with secure cookie tokens" in {
      def csrfCheckRequest = buildCsrfCheckRequest(false, "play.filters.csrf.cookie.name" -> "csrf", "play.filters.csrf.cookie.secure" -> "true")
      def csrfAddToken = buildCsrfAddToken("play.filters.csrf.cookie.name" -> "csrf", "play.filters.csrf.cookie.secure" -> "true")
      def generate = signedTokenProvider.generateToken
      def addToken(req: WSRequest, token: String) = req.withCookies("csrf" -> token)
      def getToken(response: WSResponse) = {
        response.cookie("csrf").map { cookie =>
          cookie.secure must beTrue
          cookie.value
        }
      }
      def compareTokens(a: String, b: String) = signedTokenProvider.compareTokens(a, b) must beTrue

      sharedTests(csrfCheckRequest, csrfAddToken, generate, addToken, getToken, compareTokens, FORBIDDEN)
    }

    "work with same site cookie tokens" in {
      def csrfCheckRequest = buildCsrfCheckRequest(false, "play.filters.csrf.cookie.name" -> "csrf", "play.filters.csrf.cookie.sameSite" -> "lax")
      def csrfAddToken = buildCsrfAddToken("play.filters.csrf.cookie.name" -> "csrf", "play.filters.csrf.cookie.sameSite" -> "lax")
      def generate = signedTokenProvider.generateToken
      def addToken(req: WSRequest, token: String) = req.withCookies("csrf" -> token)
      def getToken(response: WSResponse) = {
        response.cookie("csrf").map { cookie =>
          // WSCookie does not have a SameSite property, we need to read the response header as a workaround
          response.headers("Set-Cookie")(0) must beMatching("""csrf=.*; SameSite=Lax; Path=\/""")
          cookie.value
        }
      }
      def compareTokens(a: String, b: String) = signedTokenProvider.compareTokens(a, b) must beTrue

      sharedTests(csrfCheckRequest, csrfAddToken, generate, addToken, getToken, compareTokens, FORBIDDEN)
    }

    "work with checking failed result" in {
      def csrfCheckRequest = buildCsrfCheckRequest(true, "play.filters.csrf.cookie.name" -> "csrf")
      def csrfAddToken = buildCsrfAddToken("play.filters.csrf.cookie.name" -> "csrf")
      def generate = signedTokenProvider.generateToken
      def addToken(req: WSRequest, token: String) = req.withCookies("csrf" -> token)
      def getToken(response: WSResponse) = response.cookies.find(_.name == "csrf").map(_.value)
      def compareTokens(a: String, b: String) = signedTokenProvider.compareTokens(a, b) must beTrue

      sharedTests(csrfCheckRequest, csrfAddToken, generate, addToken, getToken, compareTokens, UNAUTHORIZED)
    }

    "allow configuring a header bypass" in {
      def csrfCheckRequest = buildCsrfCheckRequest(
        false,
        "play.filters.csrf.header.bypassHeaders.X-Requested-With" -> "*",
        "play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck"
      )

      "accept requests with nocheck header" in {
        csrfCheckRequest(_.withCookies("foo" -> "bar")
          .addHttpHeaders(HeaderName -> "nocheck")
          .post(Map("foo" -> "bar"))
        )(_.status must_== OK)
      }
      "accept requests with ajax header" in {
        csrfCheckRequest(_.withCookies("foo" -> "bar")
          .addHttpHeaders("X-Requested-With" -> "a spoon")
          .post(Map("foo" -> "bar"))
        )(_.status must_== OK)
      }
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
      request.withCookies(sessionCookieBaker.COOKIE_NAME -> sessionCookieBaker.encode(session.toMap))
    }
    def withCookies(cookies: (String, String)*): WSRequest = {
      request.addCookies(cookies.map(c => DefaultWSCookie(c._1, c._2)): _*)
    }
    def addCookie(cookies: (String, String)*): WSRequest = {
      request.addCookies(cookies.map(c => DefaultWSCookie(c._1, c._2)): _*)
    }
  }

  implicit def simpleFormContentType: ContentTypeOf[Map[String, String]] = ContentTypeOf[Map[String, String]](Some(ContentTypes.FORM))

  def withServer[T](config: Seq[(String, String)])(router: PartialFunction[(String, String), Handler])(block: WSClient => T) = {
    implicit val app = GuiceApplicationBuilder()
      .configure(Map(config: _*) ++ Map("play.http.secret.key" -> "ad31779d4ee49d5ad5162bf1429c32e2e9933f3b"))
      .routes(router)
      .build()
    val ws = inject[WSClient]
    running(TestServer(testServerPort, app))(block(ws))
  }

  def withActionServer[T](config: Seq[(String, String)])(router: Application => PartialFunction[(String, String), Handler])(block: WSClient => T) = {
    implicit val app = GuiceApplicationBuilder()
      .configure(Map(config: _*) ++ Map("play.http.secret.key" -> "ad31779d4ee49d5ad5162bf1429c32e2e9933f3b"))
      .appRoutes(app => router(app))
      .build()
    val ws = inject[WSClient]
    running(TestServer(testServerPort, app))(block(ws))
  }
}
