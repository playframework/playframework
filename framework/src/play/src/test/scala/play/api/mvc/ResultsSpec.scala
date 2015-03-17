/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.mvc

import org.specs2.mutable._
import play.api.libs.iteratee.{ Iteratee, Enumerator }
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits._
import play.api.i18n.{ DefaultLangs, DefaultMessagesApi, Lang }
import play.api.{ Configuration, Environment, FakeApplication, Play }
import play.api.http.HeaderNames._
import play.api.http.Status._

object ResultsSpec extends Specification {

  import play.api.mvc.Results._

  sequential

  "Result" should {

    "have status" in {
      val Result(ResponseHeader(status, _), _, _) = Ok("hello")
      status must be_==(200)
    }

    "support Content-Type overriding" in {
      val Result(ResponseHeader(_, headers), _, _) = Ok("hello").as("text/html")
      headers must havePair("Content-Type" -> "text/html")
    }

    "support headers manipulation" in {
      val Result(ResponseHeader(_, headers), _, _) =
        Ok("hello").as("text/html").withHeaders("Set-Cookie" -> "yes", "X-YOP" -> "1", "X-Yop" -> "2")

      headers.size must be_==(3)
      headers must havePair("Content-Type" -> "text/html")
      headers must havePair("Set-Cookie" -> "yes")
      headers must not havePair ("X-YOP" -> "1")
      headers must havePair("X-Yop" -> "2")
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

      val Result(ResponseHeader(_, headers), _, _) =
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

    "provide convenience method for setting cookie header" in {
      def testWithCookies(
        cookies1: List[Cookie],
        cookies2: List[Cookie],
        expected: Option[Set[Cookie]]) = {
        val result = Ok("hello").withCookies(cookies1: _*).withCookies(cookies2: _*)
        result.header.headers.get("Set-Cookie").map(Cookies.decode(_).to[Set]) must_== expected
      }
      val preferencesCookie = Cookie("preferences", "blue")
      val sessionCookie = Cookie("session", "items")
      testWithCookies(
        List(),
        List(),
        None)
      testWithCookies(
        List(preferencesCookie),
        List(),
        Some(Set(preferencesCookie)))
      testWithCookies(
        List(),
        List(sessionCookie),
        Some(Set(sessionCookie)))
      testWithCookies(
        List(),
        List(sessionCookie, preferencesCookie),
        Some(Set(sessionCookie, preferencesCookie)))
      testWithCookies(
        List(sessionCookie, preferencesCookie),
        List(),
        Some(Set(sessionCookie, preferencesCookie)))
      testWithCookies(
        List(preferencesCookie),
        List(sessionCookie),
        Some(Set(preferencesCookie, sessionCookie)))
    }

    "support clearing a language cookie using clearingLang" in {
      implicit val messagesApi = new DefaultMessagesApi(Environment.simple(), Configuration.reference, new DefaultLangs(Configuration.reference))
      val cookie = Cookies.decode(Ok.clearingLang.header.headers("Set-Cookie")).head
      cookie.name must_== Play.langCookieName
      cookie.value must_== ""
    }

    "allow discarding a cookie by deprecated names method" in {
      Cookies.decode(Ok.discardingCookies(DiscardingCookie("blah")).header.headers("Set-Cookie")).head.name must_== "blah"
    }

    "allow discarding multiple cookies by deprecated names method" in {
      val cookies = Cookies.decode(Ok.discardingCookies(DiscardingCookie("foo"), DiscardingCookie("bar")).header.headers("Set-Cookie")).map(_.name)
      cookies must containTheSameElementsAs(Seq("foo", "bar"))
    }

    "support sending a file with Ok status" in {
      val file = new java.io.File("test.tmp")
      file.createNewFile()
      val rh = Ok.sendFile(file).header
      file.delete()

      (rh.status aka "status" must_== OK) and
        (rh.headers.get(CONTENT_DISPOSITION) aka "disposition" must beSome("""attachment; filename="test.tmp""""))
    }
    "support sending a file with Unauthorized status" in {
      val file = new java.io.File("test.tmp")
      file.createNewFile()
      val rh = Unauthorized.sendFile(file).header
      file.delete()

      (rh.status aka "status" must_== UNAUTHORIZED) and
        (rh.headers.get(CONTENT_DISPOSITION) aka "disposition" must beSome("""attachment; filename="test.tmp""""))
    }

    "support sending a file inline with Unauthorized status" in {
      val file = new java.io.File("test.tmp")
      file.createNewFile()
      val rh = Unauthorized.sendFile(file, inline = true).header
      file.delete()

      (rh.status aka "status" must_== UNAUTHORIZED) and
        (rh.headers.get(CONTENT_DISPOSITION) aka "disposition" must beNone)
    }

    "support redirects for reverse routed calls" in {
      Results.Redirect(Call("GET", "/path")).header must_== Status(303).withHeaders(LOCATION -> "/path").header
    }

    "support redirects for reverse routed calls with custom statuses" in {
      Results.Redirect(Call("GET", "/path"), TEMPORARY_REDIRECT).header must_== Status(TEMPORARY_REDIRECT).withHeaders(LOCATION -> "/path").header
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
    "dechunk a stream with trailers, ignoring the trailers" in {
      consume(enumerator("a", "bc", "def") &> chunk(Some(
        Iteratee.consume[Array[Byte]]().map(data => Seq("Full-Data" -> new String(data)))
      )) &> dechunk) must containTheSameElementsAs(Seq(
        "a", "bc", "def"
      ))
    }
    "dechunk a stream with trailers and get the trailers" in {
      def consumeWithTrailers(enumerator: Enumerator[Either[Array[Byte], Seq[(String, String)]]]) = Await.result(
        enumerator |>>> Iteratee.getChunks[Either[Array[Byte], Seq[(String, String)]]],
        Duration(5, TimeUnit.SECONDS)
      ).map {
          case Left(bytes) => Left(new String(bytes))
          case r => r
        }

      consumeWithTrailers(enumerator("a", "bc", "def") &> chunk(Some(
        Iteratee.consume[Array[Byte]]().map(_ => Seq("Full-Data" -> "333"))
      )) &> dechunkWithTrailers) must containTheSameElementsAs(Seq(
        Left("a"), Left("bc"), Left("def"), Right(Seq("Full-Data" -> "333"))
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
