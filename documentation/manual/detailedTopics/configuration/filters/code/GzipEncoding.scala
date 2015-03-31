/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package detailedtopics.configuration.gzipencoding

import play.api.test._

object GzipEncoding extends PlaySpecification {

  //#filters
  import javax.inject.Inject

  import play.api.http.HttpFilters
  import play.filters.gzip.GzipFilter

  class Filters @Inject() (gzipFilter: GzipFilter) extends HttpFilters {
    def filters = Seq(gzipFilter)
  }
  //#filters

  "gzip filter" should {

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

  }

  def gzipRequest = FakeRequest().withHeaders(ACCEPT_ENCODING -> "gzip")


}
