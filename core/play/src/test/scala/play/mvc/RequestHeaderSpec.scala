/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc

import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

import org.specs2.mutable.Specification
import play.api.http.HttpConfiguration
import play.api.libs.typedmap.TypedMap
import play.api.mvc.request.DefaultRequestFactory
import play.api.mvc.request.RemoteConnection
import play.api.mvc.request.RequestTarget
import play.api.mvc.Headers
import play.api.mvc.RequestHeader
import play.mvc.Http.HeaderNames

class RequestHeaderSpec extends Specification {
  private def requestHeader(headers: (String, String)*): RequestHeader = {
    new DefaultRequestFactory(HttpConfiguration()).createRequestHeader(
      connection = RemoteConnection("", secure = false, None),
      method = "GET",
      target = RequestTarget("/", "", Map.empty),
      version = "",
      headers = Headers(headers: _*),
      attrs = TypedMap.empty
    )
  }

  def headers(additionalHeaders: Map[String, java.util.List[String]] = Map.empty) = {
    val headers = (Map("a" -> List("b1", "b2").asJava, "c" -> List("d1", "d2").asJava) ++ additionalHeaders).asJava
    new Http.Headers(headers)
  }

  "RequestHeader" should {
    "headers" in {
      "check if the header exists" in {
        headers().contains("a") must beTrue
        headers().contains("non-existent") must beFalse
      }

      "get a single header value" in {
        headers().get("a").toScala must beSome("b1")
        headers().get("c").toScala must beSome("d1")
      }

      "get all header values" in {
        headers().getAll("a").asScala must containTheSameElementsAs(Seq("b1", "b2"))
        headers().getAll("c").asScala must containTheSameElementsAs(Seq("d1", "d2"))
      }

      "handle header names case insensitively" in {
        "when getting the header" in {
          headers().get("a").toScala must beSome("b1")
          headers().get("c").toScala must beSome("d1")

          headers().get("A").toScala must beSome("b1")
          headers().get("C").toScala must beSome("d1")
        }

        "when checking if the header exists" in {
          headers().contains("a") must beTrue
          headers().contains("A") must beTrue
        }
      }

      "can add new headers" in {
        val hs = headers()
        val h  = hs.adding("new", "value")
        hs mustNotEqual h
        h.contains("new") must beTrue
        hs.contains("new") must beFalse
        h.get("new").toScala must beSome("value")
        hs.get("new").toScala must beNone
      }

      "can add new headers with a list of values" in {
        val hs = headers()
        val h  = hs.adding("new", List("v1", "v2", "v3").asJava)
        hs mustNotEqual h
        h.getAll("new").asScala must containTheSameElementsAs(Seq("v1", "v2", "v3"))
        hs.getAll("new").asScala must not contain (anyOf("v1", "v2", "v3"))
      }

      "remove a header" in {
        val hs = headers()
        val h  = hs.adding("to-be-removed", "value")
        hs mustNotEqual h
        h.contains("to-be-removed") must beTrue
        hs.contains("to-be-removed") must beFalse
        val rh = h.removing("to-be-removed")
        rh mustNotEqual h
        rh.contains("to-be-removed") must beFalse
        h.contains("to-be-removed") must beTrue
      }
    }

    "has body" in {
      "when there is a content-length greater than zero" in {
        requestHeader(HeaderNames.CONTENT_LENGTH -> "10").asJava.hasBody must beTrue
      }

      "when there is a transfer-encoding header" in {
        requestHeader(HeaderNames.TRANSFER_ENCODING -> "gzip").asJava.hasBody must beTrue
      }
    }

    "has no body" in {
      "when there is not a content-length greater than zero" in {
        requestHeader(HeaderNames.CONTENT_LENGTH -> "0").asJava.hasBody must beFalse
      }

      "when there is not a transfer-encoding header" in {
        requestHeader().asJava.hasBody must beFalse
      }
    }
  }
}
