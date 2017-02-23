/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.filters.csrf

import java.util.concurrent.CompletableFuture
import javax.inject.Inject

import play.api.http.HttpFilters
import play.filters.csrf.CSRFConfig
import play.mvc.Http

import scala.concurrent.Future
import play.api.libs.ws._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.test._
import scala.util.Random
import play.api.inject.guice.{ GuiceApplicationBuilder, GuiceApplicationLoader }
import play.api.{ Mode, Configuration, Environment }
import play.api.ApplicationLoader.Context
import play.core.DefaultWebCommands

/**
 * Specs for the global CSRF filter
 */
object CSRFFilterSpec extends CSRFCommonSpecs {

  sequential

  "a CSRF filter also" should {

    // conditions for adding a token
    "not add a token to non GET requests" in {
      buildCsrfAddToken()(_.put(""))(_.status must_== NOT_FOUND)
    }
    "not add a token to GET requests that don't accept HTML" in {
      buildCsrfAddToken()(_.withHeaders(ACCEPT -> "application/json").get())(_.status must_== NOT_FOUND)
    }
    "not add a token to responses that set cache headers" in {
      buildCsrfAddResponseHeaders(CACHE_CONTROL -> "public, max-age=3600")(_.get())(_.cookies must be empty)
    }
    "add a token to responses that set 'no-cache' headers" in {
      buildCsrfAddResponseHeaders(CACHE_CONTROL -> "no-cache")(_.get())(_.cookies must not be empty)
    }
    "add a token to GET requests that accept HTML" in {
      buildCsrfAddToken()(_.withHeaders(ACCEPT -> "text/html").get())(_.status must_== OK)
    }
    "not add a token to HEAD requests that don't accept HTML" in {
      buildCsrfAddToken()(_.withHeaders(ACCEPT -> "application/json").head())(_.status must_== NOT_FOUND)
    }
    "add a token to HEAD requests that accept HTML" in {
      buildCsrfAddToken()(_.withHeaders(ACCEPT -> "text/html").head())(_.status must_== OK)
    }

    // extra conditions for doing a check
    "check non form bodies" in {
      buildCsrfCheckRequest(false)(_.withCookies("foo" -> "bar").post(Json.obj("foo" -> "bar")))(_.status must_== FORBIDDEN)
    }
    "check all methods" in {
      buildCsrfCheckRequest(false)(_.withCookies("foo" -> "bar").delete())(_.status must_== FORBIDDEN)
    }
    "not check safe methods" in {
      buildCsrfCheckRequest(false)(_.withCookies("foo" -> "bar").options())(_.status must_== OK)
    }
    "not check requests with no cookies" in {
      buildCsrfCheckRequest(false)(_.post(Map("foo" -> "bar")))(_.status must_== OK)
    }

    // other
    "feed the body once a check has been done and passes" in {
      withServer(Seq(
        "play.http.filters" -> classOf[CsrfFilters].getName
      )) {
        case _ => Action(
          _.body.asFormUrlEncoded
            .flatMap(_.get("foo"))
            .flatMap(_.headOption)
            .map(Results.Ok(_))
            .getOrElse(Results.NotFound))
      } {
        val token = crypto.generateSignedToken
        import play.api.Play.current
        await(WS.url("http://localhost:" + testServerPort).withSession(TokenName -> token)
          .post(Map("foo" -> "bar", TokenName -> token))).body must_== "bar"
      }
    }

    val notBufferedFakeApp = GuiceApplicationBuilder()
      .configure(
        "play.crypto.secret" -> "foobar",
        "play.filters.csrf.body.bufferSize" -> "200",
        "play.http.filters" -> classOf[CsrfFilters].getName
      )
      .routes {
        case _ => Action { req =>
          (for {
            body <- req.body.asFormUrlEncoded
            foos <- body.get("foo")
            foo <- foos.headOption
            buffereds <- body.get("buffered")
            buffered <- buffereds.headOption
          } yield {
            Results.Ok(foo + " " + buffered)
          }).getOrElse(Results.NotFound)
        }
      }
      .build()

    "feed a not fully buffered body once a check has been done and passes" in new WithServer(notBufferedFakeApp, testServerPort) {
      val token = crypto.generateSignedToken
      val response = await(WS.url("http://localhost:" + port).withSession(TokenName -> token)
        .withHeaders(CONTENT_TYPE -> "application/x-www-form-urlencoded")
        .post(
          Seq(
            // Ensure token is first so that it makes it into the buffered part
            TokenName -> token,
            "buffered" -> "buffer",
            // This value must go over the edge of csrf.body.bufferSize
            "longvalue" -> Random.alphanumeric.take(1024).mkString(""),
            "foo" -> "bar"
          ).map(f => f._1 + "=" + f._2).mkString("&")
        )
      )
      response.status must_== OK
      response.body must_== "bar buffer"
    }

    "work with a Java error handler" in {
      def csrfCheckRequest = buildCsrfCheckRequestWithJavaHandler()
      def csrfAddToken = buildCsrfAddToken("csrf.cookie.name" -> "csrf")
      def generate = crypto.generateSignedToken
      def addToken(req: WSRequest, token: String) = req.withCookies("csrf" -> token)
      def getToken(response: WSResponse) = response.cookies.find(_.name.exists(_ == "csrf")).flatMap(_.value)
      def compareTokens(a: String, b: String) = crypto.compareSignedTokens(a, b) must beTrue

      sharedTests(csrfCheckRequest, csrfAddToken, generate, addToken, getToken, compareTokens, UNAUTHORIZED)
    }

  }

  "The CSRF module" should {
    val environment = Environment(new java.io.File("."), getClass.getClassLoader, Mode.Test)
    def fakeContext = Context(
      environment,
      None,
      new DefaultWebCommands,
      Configuration.load(environment)
    )
    def loader = new GuiceApplicationLoader
    "allow injecting CSRF filters" in {
      val app = loader.load(fakeContext)
      app.injector.instanceOf[CSRFFilter] must beAnInstanceOf[CSRFFilter]
    }
  }

  def buildCsrfCheckRequest(sendUnauthorizedResult: Boolean, configuration: (String, String)*) = new CsrfTester {
    def apply[T](makeRequest: (WSRequest) => Future[WSResponse])(handleResponse: (WSResponse) => T) = {
      val config = configuration ++ Seq("play.http.filters" -> classOf[CsrfFilters].getName) ++ {
        if (sendUnauthorizedResult) Seq("play.filters.csrf.errorHandler" -> classOf[CustomErrorHandler].getName) else Nil
      }
      withServer(config) {
        case _ => Action(Results.Ok)
      } {
        import play.api.Play.current
        handleResponse(await(makeRequest(WS.url("http://localhost:" + testServerPort))))
      }
    }
  }

  def buildCsrfCheckRequestWithJavaHandler() = new CsrfTester {
    def apply[T](makeRequest: (WSRequest) => Future[WSResponse])(handleResponse: (WSResponse) => T) = {
      withServer(Seq(
        "play.http.filters" -> classOf[CsrfFilters].getName,
        "play.filters.csrf.cookie.name" -> "csrf",
        "play.filters.csrf.errorHandler" -> "play.filters.csrf.JavaErrorHandler"
      )) {
        case _ => Action(Results.Ok)
      } {
        import play.api.Play.current
        handleResponse(await(makeRequest(WS.url("http://localhost:" + testServerPort))))
      }
    }
  }

  def buildCsrfAddToken(configuration: (String, String)*) = new CsrfTester {
    def apply[T](makeRequest: (WSRequest) => Future[WSResponse])(handleResponse: (WSResponse) => T) = withServer(
      configuration ++ Seq("play.http.filters" -> classOf[CsrfFilters].getName)
    ) {
        case _ => Action { implicit req =>
          CSRF.getToken(req).map { token =>
            Results.Ok(token.value)
          } getOrElse Results.NotFound
        }
      } {
        import play.api.Play.current
        handleResponse(await(makeRequest(WS.url("http://localhost:" + testServerPort))))
      }
  }

  def buildCsrfAddResponseHeaders(responseHeaders: (String, String)*) = new CsrfTester {
    def apply[T](makeRequest: (WSRequest) => Future[WSResponse])(handleResponse: (WSResponse) => T) = withServer(
      Seq("play.http.filters" -> classOf[CsrfFilters].getName)
    ) {
        case _ => Action(Results.Ok.withHeaders(responseHeaders: _*))
      } {
        import play.api.Play.current
        handleResponse(await(makeRequest(WS.url("http://localhost:" + testServerPort))))
      }
  }

  class CustomErrorHandler extends CSRF.ErrorHandler {
    import play.api.mvc.Results.Unauthorized
    def handle(req: RequestHeader, msg: String) = Future.successful(Unauthorized(msg))
  }
}

class JavaErrorHandler extends CSRFErrorHandler {
  def handle(req: Http.RequestHeader, msg: String) = CompletableFuture.completedFuture(play.mvc.Results.unauthorized())
}

class CsrfFilters @Inject() (filter: CSRFFilter) extends HttpFilters {
  def filters = Seq(filter)
}
