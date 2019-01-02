/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc

import org.specs2.mutable.Specification
import play.api.http.HttpConfiguration
import play.api.libs.typedmap.TypedMap
import play.api.mvc.{ Headers, RequestHeader }
import play.api.mvc.request.{ DefaultRequestFactory, RemoteConnection, RequestTarget }
import play.mvc.Http.HeaderNames

import scala.compat.java8.OptionConverters._
import scala.collection.JavaConverters._

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
        toScala(headers().get("a")) must beSome("b1")
        toScala(headers().get("c")) must beSome("d1")
      }

      "get all header values" in {
        headers().getAll("a").asScala must containTheSameElementsAs(Seq("b1", "b2"))
        headers().getAll("c").asScala must containTheSameElementsAs(Seq("d1", "d2"))
      }

      "handle header names case insensitively" in {

        "when getting the header" in {
          toScala(headers().get("a")) must beSome("b1")
          toScala(headers().get("c")) must beSome("d1")

          toScala(headers().get("A")) must beSome("b1")
          toScala(headers().get("C")) must beSome("d1")
        }

        "when checking if the header exists" in {
          headers().contains("a") must beTrue
          headers().contains("A") must beTrue
        }
      }

      "can add new headers" in {
        val h = headers().addHeader("new", "value")
        h.contains("new") must beTrue
        toScala(h.get("new")) must beSome("value")
      }

      "can add new headers with a list of values" in {
        val h = headers().addHeader("new", List("v1", "v2", "v3").asJava)
        h.getAll("new").asScala must containTheSameElementsAs(Seq("v1", "v2", "v3"))
      }

      "remove a header" in {
        val h = headers().addHeader("to-be-removed", "value")
        h.contains("to-be-removed") must beTrue
        h.remove("to-be-removed").contains("to-be-removed") must beFalse
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
