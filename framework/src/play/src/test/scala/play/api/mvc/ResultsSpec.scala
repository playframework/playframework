package play.api.mvc

import org.specs2.mutable._
import org.specs2.specification.{AroundOutside, Scope}
import org.specs2.execute.{Result => SpecsResult}
import play.api.Application

object ResultsSpec extends Specification {

  import play.api.mvc.Results._

  sequential

  "SimpleResult" should {

    "have status" in {
      val SimpleResult(ResponseHeader(status, _), _) = Ok("hello")
      status must be_==(200)
    }

    "support Content-Type overriding" in {
      val SimpleResult(ResponseHeader(_, headers), _) = Ok("hello").as("text/html")
      headers must havePair("Content-Type" -> "text/html")
    }

    "support headers manipulaton" in {
      val SimpleResult(ResponseHeader(_, headers), _) =
        Ok("hello").as("text/html").withHeaders("Set-Cookie" -> "yes", "X-YOP" -> "1", "X-YOP" -> "2")

      headers.size must be_==(3)
      headers must havePair("Content-Type" -> "text/html")
      headers must havePair("Set-Cookie" -> "yes")
      headers must havePair("X-YOP" -> "2")
    }

    "support cookies helper" in {
      val setCookieHeader = Cookies.encode(Seq(Cookie("session", "items"), Cookie("preferences", "blue")))

      val decodedCookies = Cookies.decode(setCookieHeader).map(c => c.name -> c).toMap
      decodedCookies.size must be_==(2)
      decodedCookies("session").value must be_==("items")
      decodedCookies("preferences").value must be_==("blue")

      val newCookieHeader = Cookies.merge(setCookieHeader, Seq(Cookie("lang", "fr"), Cookie("session", "items2")))

      val newDecodedCookies = Cookies.decode(newCookieHeader).map(c => c.name -> c).toMap
      newDecodedCookies.size must be_==(3)
      newDecodedCookies("session").value must be_==("items2")
      newDecodedCookies("preferences").value must be_==("blue")
      newDecodedCookies("lang").value must be_==("fr")

      val SimpleResult(ResponseHeader(_, headers), _) =
        Ok("hello").as("text/html")
          .withCookies(Cookie("session", "items"), Cookie("preferences", "blue"))
          .withCookies(Cookie("lang", "fr"), Cookie("session", "items2"))
          .discardingCookies(DiscardingCookie("logged"))

      val setCookies = Cookies.decode(headers("Set-Cookie")).map(c => c.name -> c).toMap
      setCookies.size must be_==(4)
      setCookies("session").value must be_==("items2")
      setCookies("session").maxAge must beNone
      setCookies("preferences").value must be_==("blue")
      setCookies("lang").value must be_==("fr")
      // Should be beSome(-1) once https://github.com/netty/netty/issues/712 is fixed
      setCookies("logged").maxAge must beSome(0)
    }


    "allow discarding a cookie by deprecated names method" in {
      Cookies.decode(Ok.discardingCookies("blah").header.headers("Set-Cookie")).head.name must_== "blah"
    }

    "allow discarding multiple cookies by deprecated names method" in {
      val cookies = Cookies.decode(Ok.discardingCookies("foo", "bar").header.headers("Set-Cookie")).map(_.name)
      cookies must contain("foo", "bar").only
    }

    "support session helper" in new WithApplication {

      Session.decode("  ").isEmpty must be_==(true)

      val data = Map("user" -> "kiki", "bad:key" -> "yop", "langs" -> "fr:en:de")
      val encodedSession = Session.encode(data)
      val decodedSession = Session.decode(encodedSession)

      decodedSession.size must be_==(2)
      decodedSession must havePair("user" -> "kiki")
      decodedSession must havePair("langs" -> "fr:en:de")
      val SimpleResult(ResponseHeader(_, headers), _) =
        Ok("hello").as("text/html")
          .withSession("user" -> "kiki", "langs" -> "fr:en:de")
          .withCookies(Cookie("session", "items"), Cookie("preferences", "blue"))
          .discardingCookies(DiscardingCookie("logged"))
          .withSession("user" -> "kiki", "langs" -> "fr:en:de")
          .withCookies(Cookie("lang", "fr"), Cookie("session", "items2"))

      val setCookies = Cookies.decode(headers("Set-Cookie")).map(c => c.name -> c).toMap
      setCookies.size must be_==(5)
      setCookies("session").value must be_==("items2")
      setCookies("preferences").value must be_==("blue")
      setCookies("lang").value must be_==("fr")
      // Should be beSome(-1) once https://github.com/netty/netty/issues/712 is fixed
      setCookies("logged").maxAge must beSome(0)
      val playSession = Session.decodeFromCookie(setCookies.get(Session.COOKIE_NAME))
      playSession.data.size must be_==(2)
      playSession.data must havePair("user" -> "kiki")
      playSession.data must havePair("langs" -> "fr:en:de")
    }

    "ignore session cookies that have been tampered with" in new WithApplication {
      val data = Map("user" -> "alice")
      val encodedSession = Session.encode(data)
      // Change a value in the session
      val maliciousSession = encodedSession.replaceFirst("user%3Aalice", "user%3Amallory")
      val decodedSession = Session.decode(maliciousSession)
      decodedSession must beEmpty
    }

    "support a custom application context" in {
      "set session on right path" in new WithFooPath {
        Cookies.decode(Ok.withSession("user" -> "alice").header.headers("Set-Cookie")).head.path must_== "/foo"
      }

      "discard session on right path" in new WithFooPath {
        Cookies.decode(Ok.withNewSession.header.headers("Set-Cookie")).head.path must_== "/foo"
      }

      "set flash on right path" in new WithFooPath {
        Cookies.decode(Ok.flashing("user" -> "alice").header.headers("Set-Cookie")).head.path must_== "/foo"
      }

      // flash cookie is discarded in PlayDefaultUpstreamHandler
    }

    "support a custom session domain" in {
      "set session on right domain" in new WithFooDomain {
        Cookies.decode(Ok.withSession("user" -> "alice").header.headers("Set-Cookie")).head.domain must beSome(".foo.com")
      }

      "discard session on right domain" in new WithFooDomain {
        Cookies.decode(Ok.withNewSession.header.headers("Set-Cookie")).head.domain must beSome(".foo.com")
      }
    }

    "support a secure session" in {
      "set session as secure" in new WithSecureSession {
        Cookies.decode(Ok.withSession("user" -> "alice").header.headers("Set-Cookie")).head.secure must_== true
      }

      "discard session as secure" in new WithSecureSession {
        Cookies.decode(Ok.withNewSession.header.headers("Set-Cookie")).head.secure must_== true
      }
    }

  }

  abstract class WithFooPath extends WithApplication("application.context" -> "/foo")

  abstract class WithFooDomain extends WithApplication("session.domain" -> ".foo.com")

  abstract class WithSecureSession extends WithApplication("session.secure" -> true)

  abstract class WithApplication(config: (String, Any)*) extends Around with Scope {
    import play.api._
    import java.io.File

    implicit lazy val app: Application =
      new DefaultApplication(new File("./src/play/src/test"), Thread.currentThread.getContextClassLoader, None, play.api.Mode.Test){
        override lazy val configuration = Configuration.from(Map("application.secret" -> "pass",
          "ehcacheplugin" -> "disabled") ++ config.toMap)
      }

    def around[T <% SpecsResult](t: => T) = {
      Play.start(app)
      try {
        t
      } finally {
        Play.stop()
      }
    }
  }
}
