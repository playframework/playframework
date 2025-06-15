/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.test

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.reflectiveCalls

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.stream.Materializer
import org.apache.pekko.util.ByteString
import org.specs2.mutable._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.test.Helpers._
import play.twirl.api.Content

class HelpersSpec extends Specification {

  sequential // needed because we read/write sys.props in differents tests below

  val ctrl = new HelpersTest
  class HelpersTest extends ControllerHelpers {
    lazy val Action: ActionBuilder[Request, AnyContent] = ActionBuilder.ignoringBody
    def abcAction: EssentialAction                      = Action {
      Ok("abc").as("text/plain")
    }
    def jsonAction: EssentialAction = Action {
      Ok("""{"content": "abc"}""").as("application/json")
    }
  }

  "inMemoryDatabase" should {
    "change database with a name argument" in {
      val inMemoryDatabaseConfiguration = inMemoryDatabase("test")
      inMemoryDatabaseConfiguration.get("db.test.driver") must beSome("org.h2.Driver")
      inMemoryDatabaseConfiguration.get("db.test.url") must beSome[String].which { url =>
        url.startsWith("jdbc:h2:mem:play-test-")
      }
    }

    "add options" in {
      val inMemoryDatabaseConfiguration =
        inMemoryDatabase("test", Map("MODE" -> "PostgreSQL", "DB_CLOSE_DELAY" -> "-1"))
      inMemoryDatabaseConfiguration.get("db.test.driver") must beSome("org.h2.Driver")
      inMemoryDatabaseConfiguration.get("db.test.url") must beSome[String].which { url =>
        """^jdbc:h2:mem:play-test([0-9-]+);MODE=PostgreSQL;DB_CLOSE_DELAY=-1$""".r.findFirstIn(url).isDefined
      }
    }
  }

  "status" should {
    "extract the status from Accumulator[ByteString, Result] as Int" in {
      implicit val system: ActorSystem = ActorSystem()
      try {
        implicit val mat: Materializer = Materializer.matFromSystem
        status(ctrl.abcAction.apply(FakeRequest())) must_== 200
      } finally {
        system.terminate()
      }
    }
  }

  "contentAsString" should {
    "extract the content from Result as String" in {
      contentAsString(Future.successful(Ok("abc"))) must_== "abc"
    }

    "extract the content from Content as String" in {
      val content = new Content {
        val body: String        = "abc"
        val contentType: String = "text/plain"
      }
      contentAsString(content) must_== "abc"
    }

    "extract the content from Accumulator[ByteString, Result] as String" in {
      implicit val system: ActorSystem = ActorSystem()
      try {
        implicit val mat: Materializer = Materializer.matFromSystem
        contentAsString(ctrl.abcAction.apply(FakeRequest())) must_== "abc"
      } finally {
        system.terminate()
      }
    }
  }

  "contentAsBytes" should {
    "extract the content from Result as Bytes" in {
      contentAsBytes(Future.successful(Ok("abc"))) must_== ByteString(97, 98, 99)
    }

    "extract the content from chunked Result as Bytes" in {
      implicit val system: ActorSystem = ActorSystem()
      try {
        implicit val mat: Materializer = Materializer.matFromSystem
        contentAsBytes(Future.successful(Ok.chunked(Source(List("a", "b", "c"))))) must_== ByteString(97, 98, 99)
      } finally {
        system.terminate()
      }
    }

    "extract the content from Content as Bytes" in {
      val content = new Content {
        val body: String        = "abc"
        val contentType: String = "text/plain"
      }
      contentAsBytes(content) must_== Array(97, 98, 99)
    }
  }

  "contentAsJson" should {
    "extract the content from Result as Json" in {
      val jsonResult = Ok("""{"play":["java","scala"]}""").as("application/json")
      (contentAsJson(Future.successful(jsonResult)) \ "play").as[List[String]] must_== List("java", "scala")
    }

    "extract the content from Content as Json" in {
      val jsonContent = new Content {
        val body: String        = """{"play":["java","scala"]}"""
        val contentType: String = "application/json"
      }
      (contentAsJson(jsonContent) \ "play").as[List[String]] must_== List("java", "scala")
    }

    "extract the content from Accumulator[ByteString, Result] as Json" in {
      implicit val system: ActorSystem = ActorSystem()
      try {
        implicit val mat: Materializer = Materializer.matFromSystem
        (contentAsJson(ctrl.jsonAction.apply(FakeRequest())) \ "content").as[String] must_== "abc"
      } finally {
        system.terminate()
      }
    }
  }

  "cookies" should {
    val cookieA1 = Cookie(name = "a", value = "a1")
    val cookieA2 = Cookie(name = "a", value = "a2")
    val cookieB  = Cookie(name = "b", value = "b")

    val cookiesForResult: Seq[Cookie] = Seq(cookieA1, cookieA2, cookieB)
    val result: Future[Result]        = Future.successful(NoContent.withCookies(cookiesForResult*))
    val ckies: Cookies                = cookies(result)

    "extract cookies from Result" in {
      var i: Int = 0
      ckies.foreach { cookie =>
        cookie must_== cookiesForResult(i)
        i += 1
      }

      // Ensure that all cookies are preserved when iterated over.
      ckies.size must_== cookiesForResult.size
      ckies.toSeq must_== cookiesForResult
    }

    "extract cookies from a Result and return the last value for duplicate names when using `get`" in {
      ckies.get("a") must beSome(cookieA2)
      ckies.get("b") must beSome(cookieB)

      // Ensure that this behavior differs from Cookies created via play.api.mvc.Cookies.apply.
      Cookies(cookiesForResult).get("a") must beSome(cookieA1)
    }
  }

  "redirectLocation" should {
    "extract location for 308 Permanent Redirect" in {
      redirectLocation(Future.successful(PermanentRedirect("/test"))) must beSome("/test")
    }
  }

  def restoringSysProp[T](propName: String)(block: => T): T = {
    val original = sys.props.get(propName)
    try {
      block
    } finally {
      original match {
        case None      => sys.props -= propName
        case Some(old) => sys.props += ((propName, old))
      }
    }
  }

  "testServerAddress" should {

    "set to 0.0.0.0 by default" in {
      val addr = restoringSysProp("testserver.address") {
        sys.props -= "testserver.address"
        Helpers.testServerAddress
      }
      addr mustEqual "0.0.0.0"
    }

    "be configurable with sys props" in {
      val addr = restoringSysProp("testserver.address") {
        sys.props += (("testserver.address", "1.2.3.4"))
        Helpers.testServerAddress
      }
      addr mustEqual "1.2.3.4"
    }
  }

  "Fakes" in {
    "FakeRequest" should {
      "parse query strings" in {
        val request = FakeRequest("GET", "/uri?q1=1&q2=2", FakeHeaders(), AnyContentAsEmpty)
        request.queryString.get("q1") must beSome(contain[String]("1"))
        request.queryString.get("q2") must beSome(contain[String]("2"))
      }

      "return an empty map when there is no query string parameters" in {
        val request = FakeRequest("GET", "/uri", FakeHeaders(), AnyContentAsEmpty)
        request.queryString must beEmpty
      }

      "successfully execute a POST request with an empty body" in {
        val request         = FakeRequest(POST, "/uri").withHeaders("<myheader>" -> "headervalue")
        val fakeApplication = Helpers.baseApplicationBuilder.build()
        val result          = Helpers.route(fakeApplication, request)

        result.get.map(result => result.header.status mustEqual 404)
      }

      "successfully execute a GET request in an Application" in {
        val request         = FakeRequest(GET, "/abc")
        val fakeApplication = Helpers.baseApplicationBuilder
          .routes {
            case (GET, "/abc") => ctrl.abcAction
          }
          .build()
        val Some(result) = Helpers.route(fakeApplication, request)

        Helpers.status(result) mustEqual OK
      }

      "successfully execute a GET request in a Router" in {
        val request = FakeRequest(GET, "/abc")
        val router  = {
          import play.api.routing.Router
          import play.api.routing.sird._
          Router.from {
            case GET(p"/abc") => ctrl.abcAction
          }
        }

        val Some(result) = {
          implicit val system = ActorSystem()
          try {
            Helpers.route(router, request)
          } finally {
            system.terminate()
          }
        }

        Helpers.status(result) mustEqual OK
      }
    }
  }

}
