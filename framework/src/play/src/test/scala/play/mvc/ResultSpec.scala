/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc

import java.nio.charset.StandardCharsets
import java.util.Optional

import akka.util.ByteString
import com.fasterxml.jackson.core.JsonEncoding
import org.specs2.mutable._
import play.api.http.HttpEntity.Strict
import play.api.mvc.{ Cookie, Results => ScalaResults }
import play.mvc.Http.HeaderNames

import scala.compat.java8.OptionConverters._

class ResultSpec extends Specification {

  "Result" should {

    "allow sending JSON as UTF-16LE" in {
      val charset = JsonEncoding.UTF16_LE
      val node = play.libs.Json.newObject()
      node.put("foo", 1)
      val javaResult = play.mvc.Results.ok(node, charset)
      javaResult.charset must beEqualTo(Optional.empty)
    }

    // This is in Scala because building wrapped scala results is easier.
    "test for cookies" in {

      val javaResult = ScalaResults.Ok("Hello world").withCookies(Cookie("name1", "value1")).asJava

      val cookies = javaResult.cookies()
      val cookie = cookies.iterator().next()

      cookie.name() must be_==("name1")
      cookie.value() must be_==("value1")
    }

    "get charset correctly" in {
      val charset = StandardCharsets.ISO_8859_1.name()
      val contentType = s"text/plain;charset=$charset"
      val javaResult = ScalaResults.Ok.sendEntity(Strict(ByteString.fromString("foo", charset), Some(contentType))).asJava
      javaResult.charset() must_== Optional.of(charset)
    }

    "get the location header" in {
      val javaResult = ScalaResults.Ok("Hello world").withHeaders(HeaderNames.LOCATION -> "/new-location").asJava
      javaResult.redirectLocation().asScala must beSome("/new-location")
    }
  }
}
