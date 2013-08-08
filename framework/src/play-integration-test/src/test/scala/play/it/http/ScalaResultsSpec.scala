package play.it.http

import play.api.test._
import play.api.mvc._
import play.api.mvc.Results._

object ScalaResultsSpec extends PlaySpecification {

  "support session helper" in withApplication() {

    Session.decode("  ").isEmpty must be_==(true)

    val data = Map("user" -> "kiki", "langs" -> "fr:en:de")
    val encodedSession = Session.encode(data)
    val decodedSession = Session.decode(encodedSession)

    decodedSession must_== Map("user" -> "kiki", "langs" -> "fr:en:de")
    val SimpleResult(ResponseHeader(_, headers), _, _) =
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
    setCookies("logged").maxAge must beSome
    setCookies("logged").maxAge.get must be_<=(-86000)
    val playSession = Session.decodeFromCookie(setCookies.get(Session.COOKIE_NAME))
    playSession.data must_== Map("user" -> "kiki", "langs" -> "fr:en:de")
  }

  "ignore session cookies that have been tampered with" in withApplication() {
    val data = Map("user" -> "alice")
    val encodedSession = Session.encode(data)
    // Change a value in the session
    val maliciousSession = encodedSession.replaceFirst("user=alice", "user=mallory")
    val decodedSession = Session.decode(maliciousSession)
    decodedSession must beEmpty
  }

  "support a custom application context" in {
    "set session on right path" in withFooPath {
      Cookies.decode(Ok.withSession("user" -> "alice").header.headers("Set-Cookie")).head.path must_== "/foo"
    }

    "discard session on right path" in withFooPath {
      Cookies.decode(Ok.withNewSession.header.headers("Set-Cookie")).head.path must_== "/foo"
    }

    "set flash on right path" in withFooPath {
      Cookies.decode(Ok.flashing("user" -> "alice").header.headers("Set-Cookie")).head.path must_== "/foo"
    }

    // flash cookie is discarded in PlayDefaultUpstreamHandler
  }

  "support a custom session domain" in {
    "set session on right domain" in withFooDomain {
      Cookies.decode(Ok.withSession("user" -> "alice").header.headers("Set-Cookie")).head.domain must beSome(".foo.com")
    }

    "discard session on right domain" in withFooDomain {
      Cookies.decode(Ok.withNewSession.header.headers("Set-Cookie")).head.domain must beSome(".foo.com")
    }
  }

  "support a secure session" in {
    "set session as secure" in withSecureSession {
      Cookies.decode(Ok.withSession("user" -> "alice").header.headers("Set-Cookie")).head.secure must_== true
    }

    "discard session as secure" in withSecureSession {
      Cookies.decode(Ok.withNewSession.header.headers("Set-Cookie")).head.secure must_== true
    }
  }


  def withApplication[T](config: (String, Any)*)(block: => T): T = running(
    FakeApplication(additionalConfiguration = Map(config:_ *) + ("application.secret" -> "foo"))
  )(block)

  def withFooPath[T](block: => T) = withApplication("application.context" -> "/foo")(block)

  def withFooDomain[T](block: => T) = withApplication("session.domain" -> ".foo.com")(block)

  def withSecureSession[T](block: => T) = withApplication("session.secure" -> true)(block)

}