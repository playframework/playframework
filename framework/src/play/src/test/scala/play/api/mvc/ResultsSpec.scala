/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }
import java.time.{ LocalDateTime, ZoneOffset }
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import org.specs2.mutable._
import play.api.http.HeaderNames._
import play.api.http._
import play.api.http.Status._
import play.api.i18n._
import play.api.{ Application, Play }
import play.core.test._

import scala.concurrent.Await
import scala.concurrent.duration._

class ResultsSpec extends Specification {
  import scala.concurrent.ExecutionContext.Implicits.global

  import play.api.mvc.Results._

  implicit val fileMimeTypes: FileMimeTypes = new DefaultFileMimeTypesProvider(FileMimeTypesConfiguration()).get

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

  lazy val cookieHeaderEncoding = new DefaultCookieHeaderEncoding()
  lazy val sessionCookieBaker = new DefaultSessionCookieBaker()
  lazy val flashCookieBaker = new DefaultFlashCookieBaker()

  // bake the results cookies into the headers
  def bake(result: Result): Result = {
    result.bakeCookies(cookieHeaderEncoding, sessionCookieBaker, flashCookieBaker)
  }

  "Result" should {

    "have status" in {
      val Result(ResponseHeader(status, _, _), _, _, _, _) = Ok("hello")
      status must be_==(200)
    }

    "support Content-Type overriding" in {
      val Result(ResponseHeader(_, _, _), body, _, _, _) = Ok("hello").as("text/html")

      body.contentType must beSome("text/html")
    }

    "support headers manipulation" in {
      val Result(ResponseHeader(_, headers, _), _, _, _, _) =
        Ok("hello").as("text/html").withHeaders("Set-Cookie" -> "yes", "X-YOP" -> "1", "X-Yop" -> "2")

      headers.size must_== 2
      headers must havePair("Set-Cookie" -> "yes")
      headers must not havePair ("X-YOP" -> "1")
      headers must havePair("X-Yop" -> "2")
    }

    "support date headers manipulation" in {
      val Result(ResponseHeader(_, headers, _), _, _, _, _) =
        Ok("hello").as("text/html").withDateHeaders(DATE ->
          LocalDateTime.of(2015, 4, 1, 0, 0).atZone(ZoneOffset.UTC))
      headers must havePair(DATE -> "Wed, 01 Apr 2015 00:00:00 GMT")
    }

    "support cookies helper" in withApplication {
      val setCookieHeader = cookieHeaderEncoding.encodeSetCookieHeader(Seq(Cookie("session", "items"), Cookie("preferences", "blue")))

      val decodedCookies = cookieHeaderEncoding.decodeSetCookieHeader(setCookieHeader).map(c => c.name -> c).toMap
      decodedCookies.size must be_==(2)
      decodedCookies("session").value must be_==("items")
      decodedCookies("preferences").value must be_==("blue")

      val newCookieHeader = cookieHeaderEncoding.mergeSetCookieHeader(setCookieHeader, Seq(Cookie("lang", "fr"), Cookie("session", "items2")))

      val newDecodedCookies = cookieHeaderEncoding.decodeSetCookieHeader(newCookieHeader).map(c => c.name -> c).toMap
      newDecodedCookies.size must be_==(3)
      newDecodedCookies("session").value must be_==("items2")
      newDecodedCookies("preferences").value must be_==("blue")
      newDecodedCookies("lang").value must be_==("fr")

      val Result(ResponseHeader(_, headers, _), _, _, _, _) = bake {
        Ok("hello").as("text/html")
          .withCookies(Cookie("session", "items"), Cookie("preferences", "blue"))
          .withCookies(Cookie("lang", "fr"), Cookie("session", "items2"))
          .discardingCookies(DiscardingCookie("logged"))
      }

      val setCookies = cookieHeaderEncoding.decodeSetCookieHeader(headers("Set-Cookie")).map(c => c.name -> c).toMap
      setCookies must haveSize(4)
      setCookies("session").value must be_==("items2")
      setCookies("session").maxAge must beNone
      setCookies("preferences").value must be_==("blue")
      setCookies("lang").value must be_==("fr")
      setCookies("logged").maxAge must beSome(Cookie.DiscardedMaxAge)
    }

    "properly add and discard cookies" in {
      val result = Ok("hello").as("text/html")
        .withCookies(Cookie("session", "items"), Cookie("preferences", "blue"))
        .withCookies(Cookie("lang", "fr"), Cookie("session", "items2"))
        .discardingCookies(DiscardingCookie("logged"))

      result.newCookies.length must_== 4
      result.newCookies.find(_.name == "logged").map(_.value) must beSome("")

      val resultDiscarded = result.discardingCookies(DiscardingCookie("preferences"), DiscardingCookie("lang"))
      resultDiscarded.newCookies.length must_== 4
      resultDiscarded.newCookies.find(_.name == "preferences").map(_.value) must beSome("")
      resultDiscarded.newCookies.find(_.name == "lang").map(_.value) must beSome("")
    }

    "provide convenience method for setting cookie header" in withApplication {
      def testWithCookies(
        cookies1: List[Cookie],
        cookies2: List[Cookie],
        expected: Option[Set[Cookie]]) = {
        val result = bake { Ok("hello").withCookies(cookies1: _*).withCookies(cookies2: _*) }
        result.header.headers.get("Set-Cookie").map(cookieHeaderEncoding.decodeSetCookieHeader(_).to[Set]) must_== expected
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

    "support clearing a language cookie using clearingLang" in withApplication { app: Application =>
      implicit val messagesApi = app.injector.instanceOf[MessagesApi]
      val cookie = cookieHeaderEncoding.decodeSetCookieHeader(bake(Ok.clearingLang).header.headers("Set-Cookie")).head
      cookie.name must_== Play.langCookieName
      cookie.value must_== ""
    }

    "allow discarding a cookie by deprecated names method" in withApplication {
      cookieHeaderEncoding.decodeSetCookieHeader(bake(Ok.discardingCookies(DiscardingCookie("blah"))).header.headers("Set-Cookie")).head.name must_== "blah"
    }

    "allow discarding multiple cookies by deprecated names method" in withApplication {
      val baked = bake { Ok.discardingCookies(DiscardingCookie("foo"), DiscardingCookie("bar")) }
      val cookies = cookieHeaderEncoding.decodeSetCookieHeader(baked.header.headers("Set-Cookie")).map(_.name)
      cookies must containTheSameElementsAs(Seq("foo", "bar"))
    }

    "support sending a file with Ok status" in withFile { (file, fileName) =>
      val rh = Ok.sendFile(file).header

      (rh.status aka "status" must_== OK) and
        (rh.headers.get(CONTENT_DISPOSITION) aka "disposition" must beSome(s"""inline; filename="$fileName""""))
    }

    "support sending a file with Unauthorized status" in withFile { (file, fileName) =>
      val rh = Unauthorized.sendFile(file).header

      (rh.status aka "status" must_== UNAUTHORIZED) and
        (rh.headers.get(CONTENT_DISPOSITION) aka "disposition" must beSome(s"""inline; filename="$fileName""""))
    }

    "support sending a file attached with Unauthorized status" in withFile { (file, fileName) =>
      val rh = Unauthorized.sendFile(file, inline = false).header

      (rh.status aka "status" must_== UNAUTHORIZED) and
        (rh.headers.get(CONTENT_DISPOSITION) aka "disposition" must beSome(s"""attachment; filename="$fileName""""))
    }

    "support sending a file with PaymentRequired status" in withFile { (file, fileName) =>
      val rh = PaymentRequired.sendFile(file).header

      (rh.status aka "status" must_== PAYMENT_REQUIRED) and
        (rh.headers.get(CONTENT_DISPOSITION) aka "disposition" must beSome(s"""inline; filename="$fileName""""))
    }

    "support sending a file attached with PaymentRequired status" in withFile { (file, fileName) =>
      val rh = PaymentRequired.sendFile(file, inline = false).header

      (rh.status aka "status" must_== PAYMENT_REQUIRED) and
        (rh.headers.get(CONTENT_DISPOSITION) aka "disposition" must beSome(s"""attachment; filename="$fileName""""))
    }

    "support sending a file with filename" in withFile { (file, fileName) =>
      val rh = Ok.sendFile(file, fileName = _ => "测 试.tmp").header

      (rh.status aka "status" must_== OK) and
        (rh.headers.get(CONTENT_DISPOSITION) aka "disposition" must beSome(s"""inline; filename="? ?.tmp"; filename*=utf-8''%e6%b5%8b%20%e8%af%95.tmp"""))
    }

    "support sending a path with Ok status" in withPath { (file, fileName) =>
      val rh = Ok.sendPath(file).header

      (rh.status aka "status" must_== OK) and
        (rh.headers.get(CONTENT_DISPOSITION) aka "disposition" must beSome(s"""inline; filename="$fileName""""))
    }

    "support sending a path with Unauthorized status" in withPath { (file, fileName) =>
      val rh = Unauthorized.sendPath(file).header

      (rh.status aka "status" must_== UNAUTHORIZED) and
        (rh.headers.get(CONTENT_DISPOSITION) aka "disposition" must beSome(s"""inline; filename="$fileName""""))
    }

    "support sending a path attached with Unauthorized status" in withPath { (file, fileName) =>
      val rh = Unauthorized.sendPath(file, inline = false).header

      (rh.status aka "status" must_== UNAUTHORIZED) and
        (rh.headers.get(CONTENT_DISPOSITION) aka "disposition" must beSome(s"""attachment; filename="$fileName""""))
    }

    "support sending a path with filename" in withPath { (file, fileName) =>
      val rh = Ok.sendPath(file, fileName = _ => "测 试.tmp").header

      (rh.status aka "status" must_== OK) and
        (rh.headers.get(CONTENT_DISPOSITION) aka "disposition" must beSome(s"""inline; filename="? ?.tmp"; filename*=utf-8''%e6%b5%8b%20%e8%af%95.tmp"""))
    }

    "allow checking content length" in withPath { (file, fileName) =>
      val content = "test"
      Files.write(file, content.getBytes(StandardCharsets.ISO_8859_1))
      val rh = Ok.sendPath(file)

      rh.body.contentLength must beSome(content.length)
    }

    "sendFile should honor onClose" in withFile { (file, fileName) =>
      implicit val system = ActorSystem()
      implicit val mat = ActorMaterializer()
      try {
        var fileSent = false
        val res = Results.Ok.sendFile(file, onClose = () => {
          fileSent = true
        })

        // Actually we need to wait until the Stream completes
        Await.ready(res.body.dataStream.runWith(Sink.ignore), 60.seconds)
        // and then we need to wait until the onClose completes
        Thread.sleep(500)

        fileSent must be_==(true)
      } finally {
        Await.ready(system.terminate(), 60.seconds)
      }
    }

    "support redirects for reverse routed calls" in {
      Results.Redirect(Call("GET", "/path")).header must_== Status(303).withHeaders(LOCATION -> "/path").header
    }

    "support redirects for reverse routed calls with custom statuses" in {
      Results.Redirect(Call("GET", "/path"), TEMPORARY_REDIRECT).header must_== Status(TEMPORARY_REDIRECT).withHeaders(LOCATION -> "/path").header
    }

    "redirect with a fragment" in {
      val url = "http://host:port/path?k1=v1&k2=v2"
      val fragment = "my-fragment"
      val expectedLocation = url + "#" + fragment
      Results.Redirect(Call("GET", url, fragment)).header.headers.get(LOCATION) must_== Option(expectedLocation)
    }

    "redirect with a fragment and status" in {
      val url = "http://host:port/path?k1=v1&k2=v2"
      val fragment = "my-fragment"
      val expectedLocation = url + "#" + fragment
      Results.Redirect(Call("GET", url, fragment), 301).header.headers.get(LOCATION) must_== Option(expectedLocation)
    }

    "brew coffee with a teapot, short and stout" in {
      val Result(ResponseHeader(status, _, _), body, _, _, _) = ImATeapot("no coffee here").as("short/stout")
      status must be_==(418)
      body.contentType must beSome("short/stout")
    }

    "brew coffee with a teapot, long and sweet" in {
      val Result(ResponseHeader(status, _, _), body, _, _, _) = ImATeapot("still no coffee here").as("long/sweet")
      status must be_==(418)
      body.contentType must beSome("long/sweet")
    }
  }
}
