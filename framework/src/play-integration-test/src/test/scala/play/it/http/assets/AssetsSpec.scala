package play.it.http.assets

import controllers.Assets
import play.api.test._
import org.apache.commons.io.IOUtils
import java.util.zip.GZIPInputStream
import java.io.ByteArrayInputStream
import play.api.{Configuration, Mode}
import play.api.mvc.Handler
import play.utils.{UriEncoding, Threads}

object AssetsSpec extends PlaySpecification {
  "Assets controller" should {

    val defaultCacheControl = Some("max-age=3600")

    implicit val port: Port = testServerPort

    def withServer[T](block: => T): T = {
      val routes: PartialFunction[(String, String), Handler] = {
        case (_, path) => Assets.at("/testassets", path.substring(1))
      }
      running(TestServer(port, new FakeApplication(withRoutes = routes) {
        // setting prod mode ensures caching headers get set, gzip is turned on, etc
        override val mode = Mode.Prod
        // but we don't want to load config in prod mode
        override lazy val initialConfiguration = Threads.withContextClassLoader(classloader) {
          Configuration(Configuration.loadDev(path, Map.empty))
        }
      }))(block)
    }

    "serve an asset" in withServer {
      val result = await(wsUrl("/bar.txt").get())

      result.status must_== OK
      result.body must_== "This is a test asset."
      result.header(CONTENT_TYPE) must beSome.which(_.startsWith("text/plain"))
      result.header(ETAG) must beSome
      result.header(LAST_MODIFIED) must beSome
      result.header(VARY) must beNone
      result.header(CONTENT_ENCODING) must beNone
      result.header(CACHE_CONTROL) must_== defaultCacheControl
    }

    "serve an asset in a subdirectory" in withServer {
      val result = await(wsUrl("/subdir/baz.txt").get())

      result.status must_== OK
      result.body must_== "Content of baz.txt."
      result.header(CONTENT_TYPE) must beSome.which(_.startsWith("text/plain"))
      result.header(ETAG) must beSome
      result.header(LAST_MODIFIED) must beSome
      result.header(VARY) must beNone
      result.header(CONTENT_ENCODING) must beNone
      result.header(CACHE_CONTROL) must_== defaultCacheControl
    }

    "serve an asset with spaces in the name" in withServer {
      val result = await(wsUrl("/foo%20bar.txt").get())

      result.status must_== OK
      result.body must_== "This is a test asset with spaces."
      result.header(CONTENT_TYPE) must beSome.which(_.startsWith("text/plain"))
      result.header(ETAG) must beSome
      result.header(LAST_MODIFIED) must beSome
      result.header(VARY) must beNone
      result.header(CONTENT_ENCODING) must beNone
      result.header(CACHE_CONTROL) must_== defaultCacheControl
    }

    "serve a non gzipped asset when gzip is available but not requested" in withServer {
      val result = await(wsUrl("/foo.txt").get())

      result.body must_== "This is a test asset."
      result.header(VARY) must beSome(ACCEPT_ENCODING)
      result.header(CONTENT_ENCODING) must beNone
    }

    "serve a gzipped asset" in withServer {
      val result = await(wsUrl("/foo.txt")
        .withHeaders(ACCEPT_ENCODING -> "gzip")
        .get())

      result.header(VARY) must beSome(ACCEPT_ENCODING)
      result.header(CONTENT_ENCODING) must beSome("gzip")
      val is = new GZIPInputStream(new ByteArrayInputStream(result.getAHCResponse.getResponseBodyAsBytes))
      IOUtils.toString(is) must_== "This is a test gzipped asset.\n"
      // release deflate resources
      is.close()
      success
    }

    "return not modified when etag matches" in withServer {
      val Some(etag) = await(wsUrl("/foo.txt").get()).header(ETAG)
      val result = await(wsUrl("/foo.txt")
        .withHeaders(IF_NONE_MATCH -> etag)
        get())

      result.status must_== NOT_MODIFIED
      result.body must beEmpty
      result.header(CACHE_CONTROL) must_== defaultCacheControl
      result.header(ETAG) must beSome
      result.header(LAST_MODIFIED) must beSome
    }

    "return not modified when multiple etags supply and one matches" in withServer {
      val Some(etag) = await(wsUrl("/foo.txt").get()).header(ETAG)
      val result = await(wsUrl("/foo.txt")
        .withHeaders(IF_NONE_MATCH -> ("\"foo\", " + etag + ", \"bar\""))
        .get())

      result.status must_== NOT_MODIFIED
      result.body must beEmpty
    }

    "return asset when etag doesn't match" in withServer {
      val result = await(wsUrl("/foo.txt")
        .withHeaders(IF_NONE_MATCH -> "foobar")
        .get())

      result.status must_== OK
      result.body must_== "This is a test asset."
    }

    "return not modified when not modified since" in withServer {
      val Some(timestamp) = await(wsUrl("/foo.txt").get()).header(LAST_MODIFIED)
      val result = await(wsUrl("/foo.txt")
        .withHeaders(IF_MODIFIED_SINCE -> timestamp)
        .get())

      result.status must_== NOT_MODIFIED
      result.body must beEmpty

      // I don't know why we implement this behaviour, I can't see it in the HTTP spec, but there were tests for it
      result.header(DATE) must beSome
      result.header(ETAG) must beNone
      result.header(CACHE_CONTROL) must beNone
    }

    "return asset when modified since" in withServer {
      val result = await(wsUrl("/foo.txt")
        .withHeaders(IF_MODIFIED_SINCE -> "Tue, 13 Mar 2012 13:08:36 GMT")
        .get())

      result.status must_== OK
      result.body must_== "This is a test asset."
    }

    "ignore if modified since header if if none match header is set" in withServer {
      val result = await(wsUrl("/foo.txt")
        .withHeaders(
        IF_NONE_MATCH -> "foobar",
        IF_MODIFIED_SINCE -> "Wed, 01 Jan 2113 00:00:00 GMT" // might break in 100 years, but I won't be alive, so :P
      ).get())

      result.status must_== OK
      result.body must_== "This is a test asset."
    }

    "return the asset if the if modified since header can't be parsed" in withServer {
      val result = await(wsUrl("/foo.txt")
        .withHeaders(IF_MODIFIED_SINCE -> "Not a date")
        .get())

      result.status must_== OK
      result.body must_== "This is a test asset."
    }

    "return 200 if the asset is empty" in withServer {
      val result = await(wsUrl("/empty.txt").get())

      result.status must_== OK
      result.body must beEmpty
    }

    "return 404 for files that don't exist" in withServer {
      val result = await(wsUrl("/nosuchfile.txt").get())

      result.status must_== NOT_FOUND
      result.body must beEmpty
    }

  }
}
