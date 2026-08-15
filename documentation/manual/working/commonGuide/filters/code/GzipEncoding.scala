/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package detailedtopics.configuration.gzipencoding

import play.api.test._

class GzipEncoding extends PlaySpecification {
  import jakarta.inject.Inject
  import play.api.http.DefaultHttpFilters
  import play.filters.gzip.GzipFilter

  // #custom-content-encoding
  object CustomContentEncoding {
    import org.apache.pekko.stream.scaladsl.Flow
    import org.apache.pekko.stream.Materializer
    import org.apache.pekko.util.ByteString
    import play.filters.encoding.ContentEncodingFilter

    class BrotliFilter(createBrotliFlow: () => Flow[ByteString, ByteString, ?])(implicit mat: Materializer)
        extends ContentEncodingFilter(
          encodingName = "br",
          createFlow = createBrotliFlow
        )
  }
  // #custom-content-encoding

  class Filters @Inject() (gzipFilter: GzipFilter) extends DefaultHttpFilters(gzipFilter)

  "gzip filter" should {
    "allow custom strategies for when to gzip (Scala)" in {
      import play.api.mvc._
      running() { app =>
        implicit val mat = app.materializer
        def Action       = app.injector.instanceOf[DefaultActionBuilder]

        val filter =
          // #should-gzip
          new GzipFilter(
            shouldGzip =
              (request: RequestHeader, response: Result) => response.body.contentType.exists(_.startsWith("text/html"))
          )
        // #should-gzip

        header(CONTENT_ENCODING, filter(Action(Results.Ok("foo")))(gzipRequest).run()) must beNone
      }
    }

    "allow custom strategies for when to gzip (Java)" in {
      import play.api.mvc._
      val app = play.api.inject.guice.GuiceApplicationBuilder().build()
      running(app) {
        implicit val mat = app.materializer
        def Action       = app.injector.instanceOf[DefaultActionBuilder]

        val filter = (new CustomFilters(mat)).getFilters.get(0)

        header(CONTENT_ENCODING, filter(Action(Results.Ok("foo")))(gzipRequest).run()) must beNone
      }
    }
  }

  def gzipRequest = FakeRequest().withHeaders(ACCEPT_ENCODING -> "gzip")
}
