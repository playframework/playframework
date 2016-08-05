/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.test

import java.nio.charset.StandardCharsets
import java.util.Optional

import akka.util.ByteString
import org.specs2.mutable._

import play.api.http.HttpEntity.Strict
import play.api.mvc.{ Cookie, Results }

/**
 *
 */
class ResultSpec extends Specification {

  "Result" should {

    "allow sending JSON as UTF-16LE" in {
      val charset = "utf-16le"
      val node = play.libs.Json.newObject()
      node.put("foo", 1)
      val javaResult = play.mvc.Results.ok(node, charset)
      javaResult.charset().get().toLowerCase must_== charset
    }

    "not allow sending JSON as ISO-8859-1" in {
      val charset = "iso-8859-1"
      val node = play.libs.Json.newObject()
      node.put("foo", 1)
      play.mvc.Results.ok(node, charset) should throwAn[IllegalArgumentException]
    }

    // This is in Scala because building wrapped scala results is easier.
    "test for cookies" in {

      val javaResult = Results.Ok("Hello world").withCookies(Cookie("name1", "value1")).asJava

      val cookies = javaResult.cookies()
      val cookie = cookies.iterator().next()

      cookie.name() must be_==("name1")
      cookie.value() must be_==("value1")
    }

    "get charset correctly" in {
      val charset = StandardCharsets.ISO_8859_1.name()
      val contentType = s"text/plain;charset=$charset"
      val javaResult = Results.Ok.sendEntity(new Strict(ByteString.fromString("foo", charset), Some(contentType))).asJava
      javaResult.charset() must_== Optional.of(charset)
    }
  }
}
