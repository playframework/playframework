/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.test

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.specs2.mutable._
import play.api.mvc.Results._
import play.api.mvc._
import play.api.test.Helpers._
import play.twirl.api.Content

import scala.concurrent.Future
import scala.language.reflectiveCalls

class HelpersSpec extends Specification {

  val ctrl = new ControllerHelpers {
    lazy val Action: ActionBuilder[Request, AnyContent] = ActionBuilder.ignoringBody
    def abcAction: EssentialAction = Action {
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
      inMemoryDatabaseConfiguration.get("db.test.url") must beSome.which { url =>
        url.startsWith("jdbc:h2:mem:play-test-")
      }
    }

    "add options" in {
      val inMemoryDatabaseConfiguration = inMemoryDatabase("test", Map("MODE" -> "PostgreSQL", "DB_CLOSE_DELAY" -> "-1"))
      inMemoryDatabaseConfiguration.get("db.test.driver") must beSome("org.h2.Driver")
      inMemoryDatabaseConfiguration.get("db.test.url") must beSome.which { url =>
        """^jdbc:h2:mem:play-test([0-9-]+);MODE=PostgreSQL;DB_CLOSE_DELAY=-1$""".r.findFirstIn(url).isDefined
      }
    }
  }

  "status" should {

    "extract the status from Accumulator[ByteString, Result] as Int" in {
      implicit val system = ActorSystem()
      try {
        implicit val mat = ActorMaterializer()
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
        val body: String = "abc"
        val contentType: String = "text/plain"
      }
      contentAsString(content) must_== "abc"
    }

    "extract the content from Accumulator[ByteString, Result] as String" in {
      implicit val system = ActorSystem()
      try {
        implicit val mat = ActorMaterializer()
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
      implicit val system = ActorSystem()
      try {
        implicit val mat = ActorMaterializer()
        contentAsBytes(Future.successful(Ok.chunked(Source(List("a", "b", "c"))))) must_== ByteString(97, 98, 99)
      } finally {
        system.terminate()
      }
    }

    "extract the content from Content as Bytes" in {
      val content = new Content {
        val body: String = "abc"
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
        val body: String = """{"play":["java","scala"]}"""
        val contentType: String = "application/json"
      }
      (contentAsJson(jsonContent) \ "play").as[List[String]] must_== List("java", "scala")
    }

    "extract the content from Accumulator[ByteString, Result] as Json" in {
      implicit val system = ActorSystem()
      try {
        implicit val mat = ActorMaterializer()
        (contentAsJson(ctrl.jsonAction.apply(FakeRequest())) \ "content").as[String] must_== "abc"
      } finally {
        system.terminate()
      }
    }
  }

  "Fakes" in {
    "FakeRequest" should {
      "parse query strings" in {
        val request = FakeRequest("GET", "/uri?q1=1&q2=2", FakeHeaders(), AnyContentAsEmpty)
        request.queryString.get("q1") must beSome.which(_.contains("1"))
        request.queryString.get("q2") must beSome.which(_.contains("2"))
      }
      "return an empty map when there is no query string parameters" in {
        val request = FakeRequest("GET", "/uri", FakeHeaders(), AnyContentAsEmpty)
        request.queryString must beEmpty
      }
    }
  }

}
