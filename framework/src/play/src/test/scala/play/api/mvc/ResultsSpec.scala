package play.api.mvc

import org.specs2.mutable._
import play.api.libs.iteratee.{Iteratee, Enumerator}
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits._
import play.api.i18n.Lang
import play.api.{FakeApplication, Play}

object ResultsSpec extends Specification {

  import play.api.mvc.Results._

  sequential

  "SimpleResult" should {

    "have status" in {
      val SimpleResult(ResponseHeader(status, _), _, _) = Ok("hello")
      status must be_==(200)
    }

    "support Content-Type overriding" in {
      val SimpleResult(ResponseHeader(_, headers), _, _) = Ok("hello").as("text/html")
      headers must havePair("Content-Type" -> "text/html")
    }

    "support headers manipulaton" in {
      val SimpleResult(ResponseHeader(_, headers), _, _) =
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

      val SimpleResult(ResponseHeader(_, headers), _, _) =
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
      setCookies("logged").maxAge must beSome
      setCookies("logged").maxAge.get must be_<=(-86000)
    }

    "support adding a language cookie using withLang" in {
      implicit val app = new FakeApplication()
      val cookie = Cookies.decode(Ok.withLang(Lang("en-AU")).header.headers("Set-Cookie")).head
      cookie.name must_== Play.langCookieName
      cookie.value must_== "en-AU"
    }

    "support clearing a language cookie using clearingLang" in {
      implicit val app = new FakeApplication()
      val cookie = Cookies.decode(Ok.clearingLang.header.headers("Set-Cookie")).head
      cookie.name must_== Play.langCookieName
      cookie.value must_== ""
    }

    "allow discarding a cookie by deprecated names method" in {
      Cookies.decode(Ok.discardingCookies("blah").header.headers("Set-Cookie")).head.name must_== "blah"
    }

    "allow discarding multiple cookies by deprecated names method" in {
      val cookies = Cookies.decode(Ok.discardingCookies("foo", "bar").header.headers("Set-Cookie")).map(_.name)
      cookies must containTheSameElementsAs(Seq("foo", "bar"))
    }
  }

  "chunking enumeratee" should {
    "chunk a stream" in {
      consume(enumerator("a", "bc", "def") &> chunk) must containTheSameElementsAs(Seq(
        "1\r\na\r\n",
        "2\r\nbc\r\n",
        "3\r\ndef\r\n",
        "0\r\n\r\n"
      ))
    }

    "support trailers" in {
      consume(enumerator("a", "bc", "def") &> chunk(Some(
        Iteratee.consume[Array[Byte]]().map(data => Seq("Full-Data" -> new String(data)))
      ))) must containTheSameElementsAs(Seq(
        "1\r\na\r\n",
        "2\r\nbc\r\n",
        "3\r\ndef\r\n",
        "0\r\nFull-Data: abcdef\r\n\r\n"
      ))
    }

  }

  "dechunking enumeratee" should {
    "dechunk a chunked stream" in {
      consume(enumerator("a", "bc", "def") &> chunk &> dechunk) must containTheSameElementsAs(Seq(
        "a", "bc", "def"
      ))
    }
    "dechunk an empty stream" in {
      consume(enumerator("0\r\n\r\n") &> dechunk) must containTheSameElementsAs(Seq())
    }
    "dechunk a stream with trailers" in {
      consume(enumerator("a", "bc", "def") &> chunk(Some(
        Iteratee.consume[Array[Byte]]().map(data => Seq("Full-Data" -> new String(data)))
      )) &> dechunk) must containTheSameElementsAs(Seq(
        "a", "bc", "def"
      ))
    }
    "dechunk a stream that is not split at chunks" in {
      consume(enumerator("1\r\na\r\n2\r\nbc\r\n3\r\ndef\r\n0\r\n\r\n") &> dechunk) must containTheSameElementsAs(Seq(
        "a", "bc", "def"
      ))
    }
    "dechunk a stream that is split at different places to the chunks" in {
      consume(enumerator(
        "1\r\na",
        "\r\n2\r\nbc\r\n3\r\nd",
        "ef\r\n0\r\n\r",
        "\n"
      ) &> dechunk) must containTheSameElementsAs(Seq(
        "a", "bc", "def"
      ))
    }
  }

  def enumerator(elems: String*) = Enumerator.enumerate(elems.map(_.getBytes))
  def consume(enumerator: Enumerator[Array[Byte]]) = Await.result(
    enumerator |>>> Iteratee.getChunks[Array[Byte]],
    Duration(5, TimeUnit.SECONDS)
  ).map(new String(_))

}
