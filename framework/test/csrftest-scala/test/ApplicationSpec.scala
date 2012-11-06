package test

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._

import play.filters.csrf._
import play.filters.csrf.CSRF.Conf._

class CSRFSpec extends Specification {

  val generator = () => CSRF.Token(42.toString())

  object FakeGlobal extends play.api.mvc.WithFilters(CSRFFilter(generator)) with play.api.GlobalSettings

  val fakeApp = FakeApplication()

  val showToken = FakeRequest(GET, "/test/token").withHeaders(CONTENT_TYPE -> "application/x-www-form-urlencoded")
  val postData =  FakeRequest(POST, "/test/post").withHeaders(CONTENT_TYPE -> "application/x-www-form-urlencoded")

  "CSRF module with default configuration" should {

    "put a CSRF Token in session" in running(fakeApp) {
      val result = route(showToken.withSession("user" -> "jto"))
      result must beSome.which { r =>
        status(r) must equalTo(OK)
        session(r).get(TOKEN_NAME) must beSome
      }
    }

    "not change token if it's already in session" in running(fakeApp) {
      val withSession = showToken.withSession(TOKEN_NAME -> "FAKE_TOKEN")
      route(withSession) must beSome.which { r =>
        status(r) must equalTo(OK)
        session(r).get(TOKEN_NAME) must beNone
      }
    }

    "stuff first request session with a token" in running(fakeApp) {
       route(showToken) must beSome.which { r =>
        status(r) must equalTo(OK)
        val contentToken = contentAsString(r)
        val sessionToken = session(r).get(TOKEN_NAME)

        sessionToken must beSome
        contentToken must not(beEmpty)

        sessionToken.get must beEqualTo(contentToken)
      }
    }

    "reject POST without token" in running(fakeApp) {
      route(postData, Map("hello" -> Seq("world"))) must beSome.which { r =>
        status(r) must equalTo(BAD_REQUEST)
        session(r).get(TOKEN_NAME) must beNone
      }
    }

    "reject POST without session Token" in running(fakeApp) {
      route(FakeRequest(POST, "/test/post?%s=%s".format(TOKEN_NAME, "FAKE_TOKEN")).withHeaders(CONTENT_TYPE -> "application/x-www-form-urlencoded"),
        "Hello World!") must beSome.which { r =>
        status(r) must equalTo(BAD_REQUEST)
        session(r).get(TOKEN_NAME) must beNone
      }
    }

    "reject POST without URL Token" in running(fakeApp) {
      route(postData.withSession(TOKEN_NAME -> "FAKE_TOKEN"), "Hello World!") must beSome.which { r =>
        status(r) must equalTo(BAD_REQUEST)
        session(r).get(TOKEN_NAME) must beNone
      }
    }

    "reject POST with invalid token" in running(fakeApp) {
      route(showToken).flatMap {
        session(_).get(TOKEN_NAME)
      }.flatMap { token =>
        // TODO: Add contructor with ActionRef
        // TODO: Add helper in FakeRequest for GET params
        route(FakeRequest(POST, "/test/post?%s=%s".format(TOKEN_NAME, token.reverse)).withHeaders(CONTENT_TYPE -> "application/x-www-form-urlencoded")
          .withSession(TOKEN_NAME -> token), "Hello World!")
      } must beSome.which { r =>
        status(r) must equalTo(BAD_REQUEST)
        session(r).get(TOKEN_NAME) must beNone
      }
    }

    "allow POST with correct token in queryString" in running(fakeApp) {
      route(showToken).flatMap {
        session(_).get(TOKEN_NAME)
      }.flatMap { token =>
        route(FakeRequest(POST, "/test/post?%s=%s".format(TOKEN_NAME, token)).withHeaders(CONTENT_TYPE -> "application/x-www-form-urlencoded")
          .withSession(TOKEN_NAME -> token), "Hello World!")
      } must beSome.which { r =>
        status(r) must equalTo(OK)
        session(r).get(TOKEN_NAME) must beNone
      }
    }

    "allow POST with correct token in body" in running(fakeApp) {
      route(showToken).flatMap {
        session(_).get(TOKEN_NAME)
      }.flatMap { token =>
        route(FakeRequest(POST, "/test/post").withHeaders(CONTENT_TYPE -> "application/x-www-form-urlencoded")
          .withSession(TOKEN_NAME -> token), Map(TOKEN_NAME -> Seq(token)))
      } must beSome.which { r =>
        status(r) must equalTo(OK)
        session(r).get(TOKEN_NAME) must beNone
      }
    }


    "allow POST with correct token in multipart body" in running(fakeApp) {

      // TODO: play should have a writeable for multipart
      // TODO: Fix it (not working ???)
      def encodeParams(data: Map[String, String]) = {
        import org.jboss.netty.handler.codec.http.multipart._
        import org.jboss.netty.handler.codec.http._

        val encoder = new HttpPostRequestEncoder(new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/"), true)
        data.foreach { case (k, v) =>
          encoder.addBodyAttribute(k, v)
        }
        encoder.finalizeRequest().getContent().toByteBuffer().array()
      }

      import play.api.mvc.MultipartFormData

      skipped("TODO: encode data as multipart")
      route(showToken).flatMap {
        session(_).get(TOKEN_NAME)
      }.flatMap { token =>
        val data = encodeParams(Map(TOKEN_NAME -> token))
        route(FakeRequest(POST, "/test/post")
          .withSession(TOKEN_NAME -> token).withHeaders(CONTENT_TYPE -> "multipart/form-data"), data)
      } must beSome.which { r =>
        status(r) must equalTo(OK)
        session(r).get(TOKEN_NAME) must beNone
      }
    }

    val fakeAppWithFakeGlobal = FakeApplication(withGlobal = Some(FakeGlobal))

    "use the provided generator" in running(fakeAppWithFakeGlobal) {
      route(showToken) must beSome.which { r =>
        status(r) must equalTo(OK)
        val contentToken = contentAsString(r)
        val sessionToken = session(r).get(TOKEN_NAME)

        sessionToken must beSome
        contentToken must not(beEmpty)

        sessionToken.get must beEqualTo("42")
      }
    }

  }

  val fakeAppWithCookieName = FakeApplication(path = new java.io.File("sample/ScalaSample"),
      additionalConfiguration = Map("csrf.cookie.name" -> "JSESSIONID"))

  "CSRF module with csrf.cookie.name" should {
    "put a CSRF Token in Cookies(CSRF.COOKIES)" in running(fakeAppWithCookieName) {
      val result = route(showToken)
      result must beSome.which { r =>
        status(r) must equalTo(OK)
        cookies(r).get("JSESSIONID") must beSome
      }
    }

    val fakeAppNoCreate = FakeApplication(path = new java.io.File("sample/ScalaSample"),
      additionalConfiguration = Map("csrf.cookie.createIfNotFound" -> false))

    "NOT create a Token in Session when csrf.cookie.createIfNotFound=false" in running(fakeAppNoCreate) {
      val result = route(showToken)
      result must beSome.which { r =>
        status(r) must equalTo(OK)
        session(r).get(TOKEN_NAME) must beNone
      }
    }

    val fakeAppWithCookieNameNoCreate = FakeApplication(path = new java.io.File("sample/ScalaSample"),
        additionalConfiguration = Map("csrf.cookie.name" -> "JSESSIONID", "csrf.cookie.createIfNotFound" -> false))
    "NOT create a Token in Cookie when csrf.cookie.createIfNotFound=false" in running(fakeAppWithCookieNameNoCreate) {
      val result = route(showToken)
      result must beSome.which { r =>
        status(r) must equalTo(OK)
        cookies(r).get("JSESSIONID") must beNone
      }
    }
  }

}