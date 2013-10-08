package detailedtopics.configuration.gzipencoding

import play.api.test._
import play.filters.gzip.GzipFilter
import play.api.templates.Html
import play.api.mvc.Results
import play.core.j.JavaGlobalSettingsAdapter

object GzipEncoding extends PlaySpecification {

  "gzip filter" should {
    "be possible to configure in play" in {
      //#global
      import play.api._
      import play.api.mvc._
      import play.filters.gzip.GzipFilter

      object Global extends WithFilters(new GzipFilter()) with GlobalSettings {
        // onStart, onStop etc...
      }
      //#global

      running(FakeApplication()) {
        header(CONTENT_ENCODING,
          Global.doFilter(Action(Results.Ok))(gzipRequest).run
        ) must beSome("gzip")
      }
    }

    "allow custom strategies for when to gzip" in {
      val filter =
        //#should-gzip
        new GzipFilter(shouldGzip = (request, response) =>
          response.headers.get("Content-Type").exists(_.startsWith("text/html")))
        //#should-gzip

      import play.api.mvc._
      running(FakeApplication()) {
        header(CONTENT_ENCODING,
          filter(Action(Results.Ok("foo")))(gzipRequest).run
        ) must beNone
      }
    }

    "be possible to configure in a play java project" in {
      import play.api.mvc._
      running(FakeApplication()) {
        val global = new JavaGlobalSettingsAdapter(new Global())
        header(CONTENT_ENCODING,
          global.doFilter(Action(Results.Ok))(gzipRequest).run
        ) must beSome("gzip")
      }
    }
  }

  def gzipRequest = FakeRequest().withHeaders(ACCEPT_ENCODING -> "gzip")
}
