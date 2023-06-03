/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.csrf

import java.util.concurrent.CompletableFuture
import javax.inject.Inject

import scala.concurrent.Future
import scala.jdk.OptionConverters._
import scala.language.postfixOps
import scala.util.Random

import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.specs2.specification.core.Fragment
import play.api.http.HttpEntity
import play.api.http.HttpErrorHandler
import play.api.http.HttpFilters
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.guice.GuiceApplicationLoader
import play.api.libs.json.Json
import play.api.libs.ws._
import play.api.mvc._
import play.api.mvc.Handler.Stage
import play.api.routing.HandlerDef
import play.api.routing.Router
import play.api.test._
import play.api.ApplicationLoader.Context
import play.api.Environment
import play.api.Mode
import play.mvc.Http

/**
 * Specs for the global CSRF filter
 */
class CSRFFilterSpec extends CSRFCommonSpecs {

  "a CSRF filter also" should {
    // conditions for adding a token
    "not add a token to non GET requests" in {
      buildCsrfAddToken()(_.put(""))(_.status must_== NOT_FOUND)
    }
    "not add a token to GET requests that don't accept HTML" in {
      buildCsrfAddToken()(_.addHttpHeaders(ACCEPT -> "application/json").get())(_.status must_== NOT_FOUND)
    }
    "not add a token to GET request when response might be cached by shared cache" in {
      buildCsrfAddResponseHeaders(CACHE_CONTROL -> "public, max-age=3600")(_.get())(_.cookies must be empty)
    }
    "add a token to GET request when response is not cached by shared cache" in {
      Fragment.foreach(
        Seq(
          "no-cache",
          "no-store",
          "NO-CACHE",
          "NO-STORE ",
          "no-cache, must-revalidate",
          "private",
          "PRIVATE ",
          "must-revalidate, private"
        )
      ) { directive =>
        directive >> {
          buildCsrfAddResponseHeaders(CACHE_CONTROL -> directive)(_.get())(_.cookies must not be empty)
        }
      }
    }
    "add a token to GET request when response does not have a Cache-Control header" in {
      buildCsrfAddResponseHeaders()(_.get())(_.cookies must not be empty)
    }
    "not add a token to non GET request when response might be cached by shared cache" in {
      Fragment.foreach(Seq("POST", "PUT", "DELETE")) { method =>
        method >> {
          buildCsrfAddResponseHeaders(CACHE_CONTROL -> "public, max-age=3600")(_.execute(method))(
            _.cookies must be empty
          )
        }
      }
    }
    "not add a token to non GET request when response is not cached by shared cache" in {
      Fragment.foreach(Seq("POST", "PUT", "DELETE")) { method =>
        method >> {
          buildCsrfAddResponseHeaders(CACHE_CONTROL -> "no-cache")(_.execute(method))(
            _.cookies must be empty
          )
        }
      }
    }
    "not add a token to non GET request when response does not have a Cache-Control header" in {
      Fragment.foreach(Seq("POST", "PUT", "DELETE")) { method =>
        method >> {
          buildCsrfAddResponseHeaders()(_.execute(method))(
            _.cookies must be empty
          )
        }
      }
    }
    "add a token to GET requests that accept HTML" in {
      buildCsrfAddToken()(_.addHttpHeaders(ACCEPT -> "text/html").get())(_.status must_== OK)
    }
    "add a token to GET requests that accept XHTML" in {
      buildCsrfAddToken()(_.addHttpHeaders(ACCEPT -> "application/xhtml+xml").get())(_.status must_== OK)
    }
    "not add a token to HEAD requests that don't accept HTML" in {
      buildCsrfAddToken()(_.addHttpHeaders(ACCEPT -> "application/json").head())(_.status must_== NOT_FOUND)
    }
    "add a token to HEAD requests that accept HTML" in {
      buildCsrfAddToken()(_.addHttpHeaders(ACCEPT -> "text/html").head())(_.status must_== OK)
    }

    // extra conditions for doing a check
    "check non form bodies" in {
      buildCsrfCheckRequest(sendUnauthorizedResult = false)(_.addCookie("foo" -> "bar").post(Json.obj("foo" -> "bar")))(
        _.status must_== FORBIDDEN
      )
    }
    "check all methods" in {
      buildCsrfCheckRequest(sendUnauthorizedResult = false)(_.addCookie("foo" -> "bar").delete())(
        _.status must_== FORBIDDEN
      )
    }
    "not check safe methods" in {
      buildCsrfCheckRequest(sendUnauthorizedResult = false)(_.addCookie("foo" -> "bar").options())(_.status must_== OK)
    }
    "not check requests with no cookies" in {
      buildCsrfCheckRequest(sendUnauthorizedResult = false)(_.post(Map("foo" -> "bar")))(_.status must_== OK)
    }

    "not add a token when responding to GET requests that accept HTML and don't get the token" in {
      buildCsrfAddTokenNoRender(false)(_.addHttpHeaders(ACCEPT -> "text/html").get())(_.cookies must be empty)
    }
    "not add a token when responding to GET requests that accept XHTML and don't get the token" in {
      buildCsrfAddTokenNoRender(false)(_.addHttpHeaders(ACCEPT -> "application/xhtml+xml").get())(
        _.cookies must be empty
      )
    }
    "add a token when responding to GET requests that don't get the token, if using non-HTTPOnly session cookie" in {
      buildCsrfAddTokenNoRender(
        false,
        "play.filters.csrf.cookie.name" -> null,
        "play.http.session.httpOnly"    -> "false"
      )(_.addHttpHeaders(ACCEPT -> "text/html").get())(_.cookies must not be empty)
    }
    "add a token when responding to GET requests that don't get the token, if using non-HTTPOnly cookie" in {
      buildCsrfAddTokenNoRender(
        false,
        "play.filters.csrf.cookie.name"     -> "csrf",
        "play.filters.csrf.cookie.httpOnly" -> "false"
      )(_.addHttpHeaders(ACCEPT -> "text/html").get())(_.cookies must not be empty)
    }
    "add a token when responding to GET requests that don't get the token, if response is streamed" in {
      buildCsrfAddTokenNoRender(true)(_.addHttpHeaders(ACCEPT -> "text/html").get())(_.cookies must not be empty)
    }

    // other
    "feed the body once a check has been done and passes" in {
      withActionServer(
        Seq(
          "play.http.filters" -> classOf[CsrfFilters].getName
        )
      )(implicit app => {
        case _ =>
          val Action = inject[DefaultActionBuilder]
          Action(
            _.body.asFormUrlEncoded
              .flatMap(_.get("foo"))
              .flatMap(_.headOption)
              .map(Results.Ok(_))
              .getOrElse(Results.NotFound)
          )
      }) { (ws, port) =>
        val token = signedTokenProvider.generateToken
        await(
          ws.url("http://localhost:" + port)
            .withSession(TokenName -> token)
            .post(Map("foo" -> "bar", TokenName -> token))
        ).body[String] must_== "bar"
      }
    }

    "allow bypassing the CSRF filter using a route modifier tag" in {
      withActionServer(
        Seq(
          "play.http.filters" -> classOf[CsrfFilters].getName
        )
      )(implicit app => {
        case _ =>
          val env    = inject[Environment]
          val Action = inject[DefaultActionBuilder]
          new Stage {
            override def apply(requestHeader: RequestHeader): (RequestHeader, Handler) = {
              (
                requestHeader.addAttr(
                  Router.Attrs.HandlerDef,
                  HandlerDef(
                    env.classLoader,
                    "routes",
                    "FooController",
                    "foo",
                    Seq.empty,
                    "POST",
                    "/foo",
                    "comments",
                    Seq("NOCSRF", "api")
                  )
                ),
                Action { request =>
                  request.body.asFormUrlEncoded
                    .flatMap(_.get("foo"))
                    .flatMap(_.headOption)
                    .map(Results.Ok(_))
                    .getOrElse(Results.NotFound)
                }
              )
            }
          }
      }) { (ws, port) =>
        val token = signedTokenProvider.generateToken
        await(
          ws.url("http://localhost:" + port)
            .withSession(TokenName -> token)
            .post(Map("foo" -> "bar"))
        ).body[String] must_== "bar"
      }
    }

    "reject a request if CSRF filter route modifier tags white and blacklist are empty" in {
      withActionServer(
        Seq(
          "play.http.filters"                          -> classOf[CsrfFilters].getName,
          "play.filters.csrf.routeModifiers.whiteList" -> Seq(),
          "play.filters.csrf.routeModifiers.blackList" -> Seq(),
        )
      )(implicit app => {
        case _ =>
          val env    = inject[Environment]
          val Action = inject[DefaultActionBuilder]
          new Stage {
            override def apply(requestHeader: RequestHeader): (RequestHeader, Handler) = {
              (
                requestHeader.addAttr(
                  Router.Attrs.HandlerDef,
                  HandlerDef(
                    env.classLoader,
                    "routes",
                    "FooController",
                    "foo",
                    Seq.empty,
                    "POST",
                    "/foo",
                    "comments",
                    Seq()
                  )
                ),
                Action { request =>
                  request.body.asFormUrlEncoded
                    .flatMap(_.get("foo"))
                    .flatMap(_.headOption)
                    .map(Results.Ok(_))
                    .getOrElse(Results.NotFound)
                }
              )
            }
          }
      }) { (ws, port) =>
        val token = signedTokenProvider.generateToken
        await(
          ws.url("http://localhost:" + port)
            .withSession(TokenName -> token)
            .post(Map("foo" -> "bar"))
        ).status must_== FORBIDDEN
      }
    }

    "reject a request if CSRF filter only has route modifier tags blacklist which matches" in {
      withActionServer(
        Seq(
          "play.http.filters"                          -> classOf[CsrfFilters].getName,
          "play.filters.csrf.routeModifiers.whiteList" -> Seq(),
          "play.filters.csrf.routeModifiers.blackList" -> Seq("CHECKCSRF"),
        )
      )(implicit app => {
        case _ =>
          val env    = inject[Environment]
          val Action = inject[DefaultActionBuilder]
          new Stage {
            override def apply(requestHeader: RequestHeader): (RequestHeader, Handler) = {
              (
                requestHeader.addAttr(
                  Router.Attrs.HandlerDef,
                  HandlerDef(
                    env.classLoader,
                    "routes",
                    "FooController",
                    "foo",
                    Seq.empty,
                    "POST",
                    "/foo",
                    "comments",
                    Seq("CHECKCSRF", "api")
                  )
                ),
                Action { request =>
                  request.body.asFormUrlEncoded
                    .flatMap(_.get("foo"))
                    .flatMap(_.headOption)
                    .map(Results.Ok(_))
                    .getOrElse(Results.NotFound)
                }
              )
            }
          }
      }) { (ws, port) =>
        val token = signedTokenProvider.generateToken
        await(
          ws.url("http://localhost:" + port)
            .withSession(TokenName -> token)
            .post(Map("foo" -> "bar"))
        ).status must_== FORBIDDEN
      }
    }

    "bypass CSRF filter if only route modifier tags blacklist exists but does NOT match" in {
      withActionServer(
        Seq(
          "play.http.filters"                          -> classOf[CsrfFilters].getName,
          "play.filters.csrf.routeModifiers.whiteList" -> Seq(),
          "play.filters.csrf.routeModifiers.blackList" -> Seq("CHECKCSRF"),
        )
      )(implicit app => {
        case _ =>
          val env    = inject[Environment]
          val Action = inject[DefaultActionBuilder]
          new Stage {
            override def apply(requestHeader: RequestHeader): (RequestHeader, Handler) = {
              (
                requestHeader.addAttr(
                  Router.Attrs.HandlerDef,
                  HandlerDef(
                    env.classLoader,
                    "routes",
                    "FooController",
                    "foo",
                    Seq.empty,
                    "POST",
                    "/foo",
                    "comments",
                    Seq("SOMETHINGELSE", "api")
                  )
                ),
                Action { request =>
                  request.body.asFormUrlEncoded
                    .flatMap(_.get("foo"))
                    .flatMap(_.headOption)
                    .map(Results.Ok(_))
                    .getOrElse(Results.NotFound)
                }
              )
            }
          }
      }) { (ws, port) =>
        val token = signedTokenProvider.generateToken
        await(
          ws.url("http://localhost:" + port)
            .withSession(TokenName -> token)
            .post(Map("foo" -> "bar"))
        ).status must_== OK
      }
    }

    val notBufferedFakeApp = GuiceApplicationBuilder()
      .configure(
        "play.http.secret.key"              -> "ad31779d4ee49d5ad5162bf1429c32e2e9933f3b",
        "play.filters.csrf.body.bufferSize" -> "200",
        "play.http.filters"                 -> classOf[CsrfFilters].getName
      )
      .appRoutes(implicit app => {
        case _ => {
          val Action = inject[DefaultActionBuilder]
          Action { req =>
            (for {
              body      <- req.body.asFormUrlEncoded
              foos      <- body.get("foo")
              foo       <- foos.headOption
              buffereds <- body.get("buffered")
              buffered  <- buffereds.headOption
            } yield {
              Results.Ok(foo + " " + buffered)
            }).getOrElse(Results.NotFound)
          }
        }
      })
      .build()

    "feed a not fully buffered body once a check has been done and passes" in new WithServer(
      notBufferedFakeApp,
      testServerPort
    ) {
      override def running() = {
        val token = signedTokenProvider.generateToken
        val ws    = inject[WSClient]
        val response = await(
          ws.url("http://localhost:" + port)
            .withSession(TokenName -> token)
            .addHttpHeaders(CONTENT_TYPE -> "application/x-www-form-urlencoded")
            .post(
              Seq(
                // Ensure token is first so that it makes it into the buffered part
                TokenName  -> token,
                "buffered" -> "buffer",
                // This value must go over the edge of csrf.body.bufferSize
                "longvalue" -> Random.alphanumeric.take(1024).mkString(""),
                "foo"       -> "bar"
              ).map(f => f._1 + "=" + f._2).mkString("&")
            )
        )
        response.status must_== OK
        response.body[String] must_== "bar buffer"
      }
    }

    "work with a Java error handler" in {
      def csrfCheckRequest                        = buildCsrfCheckRequestWithJavaHandler()
      def csrfAddToken                            = buildCsrfAddToken("csrf.cookie.name" -> "csrf")
      def generate                                = signedTokenProvider.generateToken
      def addToken(req: WSRequest, token: String) = req.withCookies("csrf" -> token)
      def getToken(response: WSResponse)          = response.cookie("csrf").map(_.value)
      def compareTokens(a: String, b: String)     = signedTokenProvider.compareTokens(a, b) must beTrue

      sharedTests(csrfCheckRequest, csrfAddToken, generate, addToken, getToken, compareTokens, UNAUTHORIZED)
    }
  }

  "The CSRF module" should {
    val environment = Environment(new java.io.File("."), getClass.getClassLoader, Mode.Test)
    def fakeContext = Context.create(environment)
    def loader      = new GuiceApplicationLoader
    "allow injecting CSRF filters" in {
      implicit val app = loader.load(fakeContext)
      inject[CSRFFilter] must beAnInstanceOf[CSRFFilter]
    }
  }

  def buildCsrfCheckRequest(sendUnauthorizedResult: Boolean, configuration: (String, String)*) = new CsrfTester {
    def apply[T](makeRequest: (WSRequest) => Future[WSResponse])(handleResponse: (WSResponse) => T) = {
      val config = configuration ++ Seq("play.http.filters" -> classOf[CsrfFilters].getName) ++ {
        if (sendUnauthorizedResult) Seq("play.filters.csrf.errorHandler" -> classOf[CustomErrorHandler].getName)
        else Nil
      }
      withActionServer(config) { implicit app =>
        {
          case _ =>
            val Action = inject[DefaultActionBuilder]
            Action(Results.Ok)
        }
      } { (ws, port) => handleResponse(await(makeRequest(ws.url("http://localhost:" + port)))) }
    }
  }

  def buildCsrfCheckRequestWithJavaHandler() = new CsrfTester {
    def apply[T](makeRequest: (WSRequest) => Future[WSResponse])(handleResponse: (WSResponse) => T) = {
      withActionServer(
        Seq(
          "play.http.filters"              -> classOf[CsrfFilters].getName,
          "play.filters.csrf.cookie.name"  -> "csrf",
          "play.filters.csrf.errorHandler" -> "play.filters.csrf.JavaErrorHandler"
        )
      ) { implicit app =>
        {
          case _ =>
            val Action = inject[DefaultActionBuilder]
            Action(Results.Ok)
        }
      } { (ws, port) => handleResponse(await(makeRequest(ws.url("http://localhost:" + port)))) }
    }
  }

  def buildCsrfAddToken(configuration: (String, String)*) = new CsrfTester {
    def apply[T](makeRequest: (WSRequest) => Future[WSResponse])(handleResponse: (WSResponse) => T) = {
      withActionServer(
        configuration ++ Seq("play.http.filters" -> classOf[CsrfFilters].getName)
      )(implicit app => {
        case _ =>
          val Action = inject[DefaultActionBuilder]
          Action { implicit req =>
            CSRF
              .getToken(req)
              .map { token => Results.Ok(token.value) }
              .getOrElse(Results.NotFound)
          }
      }) { (ws, port) => handleResponse(await(makeRequest(ws.url("http://localhost:" + port)))) }
    }
  }

  def buildCsrfAddTokenNoRender(streamed: Boolean, configuration: (String, String)*) = new CsrfTester {
    def apply[T](makeRequest: (WSRequest) => Future[WSResponse])(handleResponse: (WSResponse) => T) = {
      withActionServer(
        configuration ++ Seq("play.http.filters" -> classOf[CsrfFilters].getName)
      )(implicit app => {
        case _ =>
          val Action = inject[DefaultActionBuilder]
          if (streamed) {
            Action(
              Result(
                header = ResponseHeader(200, Map.empty),
                body = HttpEntity.Streamed(Source.single(ByteString("Hello world")), None, Some("text/html"))
              )
            )
          } else {
            Action(Results.Ok("Hello world!"))
          }
      }) { (ws, port) => handleResponse(await(makeRequest(ws.url("http://localhost:" + port)))) }
    }
  }

  def buildCsrfAddResponseHeaders(responseHeaders: (String, String)*) = new CsrfTester {
    def apply[T](makeRequest: (WSRequest) => Future[WSResponse])(handleResponse: (WSResponse) => T) = {
      withActionServer(
        Seq("play.http.filters" -> classOf[CsrfFilters].getName)
      )(implicit app => {
        case _ =>
          val Action = inject[DefaultActionBuilder]
          Action { implicit request: RequestHeader =>
            Results.Ok(CSRF.getToken.fold("")(_.value)).withHeaders(responseHeaders: _*)
          }
      }) { (ws, port) => handleResponse(await(makeRequest(ws.url("http://localhost:" + port)))) }
    }
  }
}

class CustomErrorHandler extends CSRF.ErrorHandler {
  import play.api.mvc.Results.Unauthorized
  def handle(req: RequestHeader, msg: String) =
    Future.successful(
      Unauthorized(
        "Origin: " + req.attrs
          .get(HttpErrorHandler.Attrs.HttpErrorInfo)
          .map(_.origin)
          .getOrElse("<not set>") + " / " + msg
      )
    )
}

class JavaErrorHandler extends CSRFErrorHandler {
  def handle(req: Http.RequestHeader, msg: String) =
    CompletableFuture.completedFuture(
      play.mvc.Results.unauthorized(
        "Origin: " + req.attrs
          .getOptional(play.http.HttpErrorHandler.Attrs.HTTP_ERROR_INFO)
          .toScala
          .map(_.origin)
          .getOrElse("<not set>") + " / " + msg
      )
    )
}

class CsrfFilters @Inject() (filter: CSRFFilter) extends HttpFilters {
  def filters = Seq(filter)
}
