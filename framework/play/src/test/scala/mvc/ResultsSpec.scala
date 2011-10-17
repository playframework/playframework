package play.test

import org.specs2.mutable._

class ResultsSpec extends Specification {

  import play.api.mvc._
  import play.api.mvc.Results._

  "SimpleResult" should {

    "have status" in {
      val SimpleResult(SimpleHttpResponse(status, _), _) = Ok("hello")
      status must be_==(200)
    }

    "support Content-Type overriding" in {
      val SimpleResult(SimpleHttpResponse(_, headers), _) = Ok("hello").as("text/html")
      headers must havePair("Content-Type" -> "text/html")
    }

    "support headers manipulaton" in {
      val SimpleResult(SimpleHttpResponse(_, headers), _) =
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

      val SimpleResult(SimpleHttpResponse(_, headers), _) =
        Ok("hello").as("text/html")
          .withCookies(Cookie("session", "items"), Cookie("preferences", "blue"))
          .withCookies(Cookie("lang", "fr"), Cookie("session", "items2"))
          .discardingCookies("logged")

      val setCookies = Cookies.decode(headers("Set-Cookie")).map(c => c.name -> c).toMap
      setCookies.size must be_==(4)
      setCookies("session").value must be_==("items2")
      setCookies("preferences").value must be_==("blue")
      setCookies("lang").value must be_==("fr")
      setCookies("logged").maxAge must be_==(0)
    }

    "support session helper" in {

      Session.decode("  ").isEmpty must be_==(true)

      val data = Map("user" -> "kiki", "bad:key" -> "yop", "langs" -> "fr:en:de")
      val encodedSession = Session.encode(Session(data))
      val decodedSession = Session.decode(encodedSession)

      decodedSession.data.size must be_==(2)
      decodedSession.data must havePair("user" -> "kiki")
      decodedSession.data must havePair("langs" -> "fr:en:de")

      val SimpleResult(SimpleHttpResponse(_, headers), _) =
        Ok("hello").as("text/html")
          .withCookies(Cookie("session", "items"), Cookie("preferences", "blue"))
          .discardingCookies("logged")
          .withSession("user" -> "kiki", "langs" -> "fr:en:de")
          .withCookies(Cookie("lang", "fr"), Cookie("session", "items2"))

      val setCookies = Cookies.decode(headers("Set-Cookie")).map(c => c.name -> c).toMap
      setCookies.size must be_==(5)
      setCookies("session").value must be_==("items2")
      setCookies("preferences").value must be_==("blue")
      setCookies("lang").value must be_==("fr")
      setCookies("logged").maxAge must be_==(0)

      val playSession = Session.decodeFromCookie(setCookies.get(Session.SESSION_COOKIE_NAME))
      playSession.data.size must be_==(2)
      playSession.data must havePair("user" -> "kiki")
      playSession.data must havePair("langs" -> "fr:en:de")
    }

  }

}
