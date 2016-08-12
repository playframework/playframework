/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package detailedtopics.configuration.gzipencoding

import akka.stream.ActorMaterializer
import play.api.test._
import detailedtopics.configuration.gzipencoding.CustomFilters

class GzipEncoding extends PlaySpecification {

  //#filters
  import javax.inject.Inject

  import play.api.http.DefaultHttpFilters
  import play.filters.gzip.GzipFilter

  class Filters @Inject() (gzipFilter: GzipFilter)
      extends DefaultHttpFilters(gzipFilter)
  //#filters

  "gzip filter" should {

    "allow custom strategies for when to gzip (Scala)" in {

      import play.api.mvc._
      running() { app =>
        implicit val mat = ActorMaterializer()(app.actorSystem)

        val filter =
        //#should-gzip
          new GzipFilter(shouldGzip = (request, response) =>
            response.body.contentType.exists(_.startsWith("text/html")))
        //#should-gzip

        header(CONTENT_ENCODING,
          filter(Action(Results.Ok("foo")))(gzipRequest).run()
        ) must beNone
      }
    }

    "allow custom strategies for when to gzip (Java)" in {

      import play.api.mvc._
      val app = play.api.inject.guice.GuiceApplicationBuilder().build()
      running(app) {
        implicit val mat = ActorMaterializer()(app.actorSystem)

        val filter = (new CustomFilters(mat)).filters()(0)

        header(CONTENT_ENCODING,
          filter(Action(Results.Ok("foo")))(gzipRequest).run()
        ) must beNone
      }
    }

  }

  def gzipRequest = FakeRequest().withHeaders(ACCEPT_ENCODING -> "gzip")


}
