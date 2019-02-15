/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc

import org.specs2.mutable.Specification
import scala.collection.JavaConverters._
import scala.compat.java8.OptionConverters._

class ResponseHeaderSpec extends Specification {

  "ResponseHeader" should {

    "create with status and headers" in {
      val headers = Map("a" -> "b").asJava
      val responseHeader = new ResponseHeader(Http.Status.OK, headers)
      responseHeader.status() must beEqualTo(Http.Status.OK)
      responseHeader.getHeader("a").asScala must beSome("b")
    }

    "create with status, headers and a reason phrase" in {
      val headers = Map("a" -> "b").asJava
      val responseHeader = new ResponseHeader(Http.Status.OK, headers, "Custom")
      responseHeader.status() must beEqualTo(Http.Status.OK)
      responseHeader.getHeader("a").asScala must beSome("b")
      responseHeader.reasonPhrase().asScala must beSome("Custom")
    }

    "get all headers" in {
      val headers = Map("a" -> "b", "c" -> "d").asJava
      val responseHeader = new ResponseHeader(Http.Status.OK, headers)
      responseHeader.headers().get("a") must beEqualTo("b")
      responseHeader.headers().get("c") must beEqualTo("d")
    }

    "get a single header" in {
      val headers = Map("a" -> "b").asJava
      val responseHeader = new ResponseHeader(Http.Status.OK, headers)
      responseHeader.getHeader("a").asScala must beSome("b")
      responseHeader.getHeader("c").asScala must beNone
    }

    "add a single new header" in {
      val headers = Map("a" -> "b").asJava
      val responseHeader = new ResponseHeader(Http.Status.OK, headers)
      val newResponseHeader = responseHeader.withHeader("c", "d")
      newResponseHeader.headers().get("c") must beEqualTo("d")
    }

    "preserve existing headers when adding a single new header" in {
      val headers = Map("a" -> "b").asJava
      val responseHeader = new ResponseHeader(Http.Status.OK, headers)
      val newResponseHeader = responseHeader.withHeader("c", "d")
      newResponseHeader.headers().get("a") must beEqualTo("b")
      newResponseHeader.headers().get("c") must beEqualTo("d")
    }

    "add multiple new headers" in {
      val headers = Map("a" -> "b").asJava
      val responseHeader = new ResponseHeader(Http.Status.OK, headers)
      val newResponseHeader = responseHeader.withHeaders(Map("c" -> "d", "e" -> "f").asJava)
      newResponseHeader.headers().get("c") must beEqualTo("d")
      newResponseHeader.headers().get("e") must beEqualTo("f")
    }

    "be convertible to a Scala ResponseHeader" in {
      val headers = Map("a" -> "b").asJava
      val responseHeader = new ResponseHeader(Http.Status.OK, headers)
      val scalaResponseHeader = responseHeader.asScala()
      scalaResponseHeader.status must beEqualTo(Http.Status.OK)
      scalaResponseHeader.headers.contains("a") must beTrue
    }

    "handle header names case insensitively" in {
      "when adding a single header" in {
        val headers = Map("Name" -> "Value").asJava
        val responseHeader = new ResponseHeader(Http.Status.OK, headers).withHeader("NAME", "New Value")
        responseHeader.headers().get("name") must beEqualTo("New Value")
      }
      "when adding multiple headers" in {
        val headers = Map("Name" -> "Value").asJava
        val responseHeader = new ResponseHeader(Http.Status.OK, headers)
          .withHeaders(Map("NAME" -> "New Value", "Another" -> "Another Value").asJava)

        responseHeader.headers().get("name") must beEqualTo("New Value")
      }
      "when getting the header" in {
        val headers = Map("Name" -> "Value").asJava
        val responseHeader = new ResponseHeader(Http.Status.OK, headers)
        responseHeader.getHeader("NAME").asScala must beSome("Value")
      }
    }

  }
}
