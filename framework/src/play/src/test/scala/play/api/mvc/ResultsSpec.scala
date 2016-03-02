/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.mvc

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }
import java.util.concurrent.atomic.AtomicInteger

import org.joda.time.{ DateTime, DateTimeZone }
import org.specs2.mutable._
import play.api.http.HeaderNames._
import play.api.http.Status._
import play.api.i18n.{ DefaultLangs, DefaultMessagesApi }
import play.api.{ Configuration, Environment, Play }
import play.core.test._

object ResultsSpec extends Specification {

  import play.api.mvc.Results._

  val fileCounter = new AtomicInteger(1)
  def freshFileName: String = s"test${fileCounter.getAndIncrement}.tmp"

  def withFile[T](block: (File, String) => T): T = {
    val fileName = freshFileName
    val file = new File(fileName)
    try {
      file.createNewFile()
      block(file, fileName)
    } finally file.delete()
  }

  def withPath[T](block: (Path, String) => T): T = {
    val fileName = freshFileName
    val file = Paths.get(fileName)
    try {
      Files.createFile(file)
      block(file, fileName)
    } finally Files.delete(file)
  }

  "Result" should {

    "have status" in {
      val Result(ResponseHeader(status, _, _), _) = Ok("hello")
      status must be_==(200)
    }

    "support Content-Type overriding" in {
      val Result(ResponseHeader(_, _, _), body) = Ok("hello").as("text/html")

      body.contentType must beSome("text/html")
    }

    "support headers manipulation" in {
      val Result(ResponseHeader(_, headers, _), _) =
        Ok("hello").as("text/html").withHeaders("Set-Cookie" -> "yes", "X-YOP" -> "1", "X-Yop" -> "2")

      headers.size must_== 2
      headers must havePair("Set-Cookie" -> "yes")
      headers must not havePair ("X-YOP" -> "1")
      headers must havePair("X-Yop" -> "2")
    }

    "support date headers manipulation" in {
      val Result(ResponseHeader(_, headers, _), _) =
        Ok("hello").as("text/html").withDateHeaders(DATE ->
          new DateTime(2015, 4, 1, 0, 0).withZoneRetainFields(DateTimeZone.UTC))
      headers must havePair(DATE -> "Wed, 01 Apr 2015 00:00:00 GMT")
    }

    "support cookies helper" in withApplication {
      val setCookieHeader = Cookies.encodeSetCookieHeader(Seq(Cookie("session", "items"), Cookie("preferences", "blue")))

      val decodedCookies = Cookies.decodeSetCookieHeader(setCookieHeader).map(c => c.name -> c).toMap
      decodedCookies.size must be_==(2)
      decodedCookies("session").value must be_==("items")
      decodedCookies("preferences").value must be_==("blue")

      val newCookieHeader = Cookies.mergeSetCookieHeader(setCookieHeader, Seq(Cookie("lang", "fr"), Cookie("session", "items2")))

      val newDecodedCookies = Cookies.decodeSetCookieHeader(newCookieHeader).map(c => c.name -> c).toMap
      newDecodedCookies.size must be_==(3)
      newDecodedCookies("session").value must be_==("items2")
      newDecodedCookies("preferences").value must be_==("blue")
      newDecodedCookies("lang").value must be_==("fr")

      val Result(ResponseHeader(_, headers, _), _) =
        Ok("hello").as("text/html")
          .withCookies(Cookie("session", "items"), Cookie("preferences", "blue"))
          .withCookies(Cookie("lang", "fr"), Cookie("session", "items2"))
          .discardingCookies(DiscardingCookie("logged"))

      val setCookies = Cookies.decodeSetCookieHeader(headers("Set-Cookie")).map(c => c.name -> c).toMap
      setCookies must haveSize(4)
      setCookies("session").value must be_==("items2")
      setCookies("session").maxAge must beNone
      setCookies("preferences").value must be_==("blue")
      setCookies("lang").value must be_==("fr")
      setCookies("logged").maxAge must beSome(0)
    }

    "provide convenience method for setting cookie header" in withApplication {
      def testWithCookies(
        cookies1: List[Cookie],
        cookies2: List[Cookie],
        expected: Option[Set[Cookie]]) = {
        val result = Ok("hello").withCookies(cookies1: _*).withCookies(cookies2: _*)
        result.header.headers.get("Set-Cookie").map(Cookies.decodeSetCookieHeader(_).to[Set]) must_== expected
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

    "support clearing a language cookie using clearingLang" in withApplication {
      implicit val messagesApi = new DefaultMessagesApi(Environment.simple(), Configuration.reference, new DefaultLangs(Configuration.reference))
      val cookie = Cookies.decodeSetCookieHeader(Ok.clearingLang.header.headers("Set-Cookie")).head
      cookie.name must_== Play.langCookieName
      cookie.value must_== ""
    }

    "allow discarding a cookie by deprecated names method" in withApplication {
      Cookies.decodeSetCookieHeader(Ok.discardingCookies(DiscardingCookie("blah")).header.headers("Set-Cookie")).head.name must_== "blah"
    }

    "allow discarding multiple cookies by deprecated names method" in withApplication {
      val cookies = Cookies.decodeSetCookieHeader(Ok.discardingCookies(DiscardingCookie("foo"), DiscardingCookie("bar")).header.headers("Set-Cookie")).map(_.name)
      cookies must containTheSameElementsAs(Seq("foo", "bar"))
    }

    "support sending a file with Ok status" in withFile { (file, fileName) =>
      val rh = Ok.sendFile(file).header

      (rh.status aka "status" must_== OK) and
        (rh.headers.get(CONTENT_DISPOSITION) aka "disposition" must beSome(s"""attachment; filename="${fileName}""""))
    }

    "support sending a file with Unauthorized status" in withFile { (file, fileName) =>
      val rh = Unauthorized.sendFile(file).header

      (rh.status aka "status" must_== UNAUTHORIZED) and
        (rh.headers.get(CONTENT_DISPOSITION) aka "disposition" must beSome(s"""attachment; filename="${fileName}""""))
    }

    "support sending a file inline with Unauthorized status" in withFile { (file, fileName) =>
      val rh = Unauthorized.sendFile(file, inline = true).header

      (rh.status aka "status" must_== UNAUTHORIZED) and
        (rh.headers.get(CONTENT_DISPOSITION) aka "disposition" must beSome(s"""inline; filename="${fileName}""""))
    }

    "support sending a file with PaymentRequired status" in withFile { (file, fileName) =>
      val rh = PaymentRequired.sendFile(file).header

      (rh.status aka "status" must_== PAYMENT_REQUIRED) and
        (rh.headers.get(CONTENT_DISPOSITION) aka "disposition" must beSome(s"""attachment; filename="${fileName}""""))
    }

    "support sending a file inline with PaymentRequired status" in withFile { (file, fileName) =>
      val rh = PaymentRequired.sendFile(file, inline = true).header

      (rh.status aka "status" must_== PAYMENT_REQUIRED) and
        (rh.headers.get(CONTENT_DISPOSITION) aka "disposition" must beSome(s"""inline; filename="${fileName}""""))
    }

    "support sending a path with Ok status" in withPath { (file, fileName) =>
      val rh = Ok.sendPath(file).header

      (rh.status aka "status" must_== OK) and
        (rh.headers.get(CONTENT_DISPOSITION) aka "disposition" must beSome(s"""attachment; filename="${fileName}""""))
    }

    "support sending a path with Unauthorized status" in withPath { (file, fileName) =>
      val rh = Unauthorized.sendPath(file).header

      (rh.status aka "status" must_== UNAUTHORIZED) and
        (rh.headers.get(CONTENT_DISPOSITION) aka "disposition" must beSome(s"""attachment; filename="${fileName}""""))
    }

    "support sending a path inline with Unauthorized status" in withPath { (file, fileName) =>
      val rh = Unauthorized.sendPath(file, inline = true).header

      (rh.status aka "status" must_== UNAUTHORIZED) and
        (rh.headers.get(CONTENT_DISPOSITION) aka "disposition" must beSome(s"""inline; filename="${fileName}""""))
    }

    "allow checking content length" in withPath { (file, fileName) =>
      val content = "test"
      Files.write(file, content.getBytes(StandardCharsets.ISO_8859_1))
      val rh = Ok.sendPath(file)

      rh.body.contentLength must beSome(content.length)
    }

    "support redirects for reverse routed calls" in {
      Results.Redirect(Call("GET", "/path")).header must_== Status(303).withHeaders(LOCATION -> "/path").header
    }

    "support redirects for reverse routed calls with custom statuses" in {
      Results.Redirect(Call("GET", "/path"), TEMPORARY_REDIRECT).header must_== Status(TEMPORARY_REDIRECT).withHeaders(LOCATION -> "/path").header
    }
  }
}
