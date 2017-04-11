/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.http.assets

import controllers.{ Assets, AssetsComponents }
import play.api._
import play.api.libs.ws.WSClient
import play.api.test._
import org.apache.commons.io.IOUtils
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

import play.api.routing.Router
import play.core.server.{ Server, ServerConfig }
import play.filters.HttpFiltersComponents
import play.it._

class NettyAssetsSpec extends AssetsSpec with NettyIntegrationSpecification
class AkkaHttpAssetsSpec extends AssetsSpec with AkkaHttpIntegrationSpecification

trait AssetsSpec extends PlaySpecification
    with WsTestClient with ServerIntegrationSpecification {

  sequential

  "Assets controller" should {

    def defaultCacheControl = Play.current.configuration.getDeprecated[Option[String]]("play.assets.defaultCache")
    def aggressiveCacheControl = Play.current.configuration.getDeprecated[Option[String]]("play.assets.aggressiveCache")

    def withServer[T](block: WSClient => T): T = {
      Server.withApplicationFromContext(ServerConfig(mode = Mode.Prod, port = Some(0))) { context =>
        new BuiltInComponentsFromContext(context) with AssetsComponents with HttpFiltersComponents {
          override def router: Router = Router.from {
            case req => assets.versioned("/testassets", req.path)
          }
        }.application
      } { withClient(block)(_) }
    }

    val etagPattern = """([wW]/)?"([^"]|\\")*""""

    "serve an asset" in withServer { client =>
      val result = await(client.url("/bar.txt").get())

      result.status must_== OK
      result.body must_== "This is a test asset."
      result.header(CONTENT_TYPE) must beSome.which(_.startsWith("text/plain"))
      result.header(ETAG) must beSome(matching(etagPattern))
      result.header(LAST_MODIFIED) must beSome
      result.header(VARY) must beNone
      result.header(CONTENT_ENCODING) must beNone
      result.header(CACHE_CONTROL) must_== defaultCacheControl
    }

    "serve an asset as JSON with UTF-8 charset" in withServer { client =>
      val result = await(client.url("/test.json").get())

      result.status must_== OK
      result.body.trim must_== "{}"
      result.header(CONTENT_TYPE) must beSome.which(_ == "application/json; charset=utf-8")
      result.header(ETAG) must beSome(matching(etagPattern))
      result.header(LAST_MODIFIED) must beSome
      result.header(VARY) must beNone
      result.header(CONTENT_ENCODING) must beNone
      result.header(CACHE_CONTROL) must_== defaultCacheControl
    }

    "serve an asset in a subdirectory" in withServer { client =>
      val result = await(client.url("/subdir/baz.txt").get())

      result.status must_== OK
      result.body must_== "Content of baz.txt."
      result.header(CONTENT_TYPE) must beSome.which(_.startsWith("text/plain"))
      result.header(ETAG) must beSome(matching(etagPattern))
      result.header(LAST_MODIFIED) must beSome
      result.header(VARY) must beNone
      result.header(CONTENT_ENCODING) must beNone
      result.header(CACHE_CONTROL) must_== defaultCacheControl
    }

    "serve an asset with spaces in the name" in withServer { client =>
      val result = await(client.url("/foo%20bar.txt").get())

      result.status must_== OK
      result.body must_== "This is a test asset with spaces."
      result.header(CONTENT_TYPE) must beSome.which(_.startsWith("text/plain"))
      result.header(ETAG) must beSome(matching(etagPattern))
      result.header(LAST_MODIFIED) must beSome
      result.header(VARY) must beNone
      result.header(CONTENT_ENCODING) must beNone
      result.header(CACHE_CONTROL) must_== defaultCacheControl
    }

    "serve a non gzipped asset when gzip is available but not requested" in withServer { client =>
      val result = await(client.url("/foo.txt").get())

      result.body must_== "This is a test asset."
      result.header(VARY) must beSome(ACCEPT_ENCODING)
      result.header(CONTENT_ENCODING) must beNone
    }

    "serve a gzipped asset" in withServer { client =>
      val result = await(client.url("/foo.txt")
        .withHeaders(ACCEPT_ENCODING -> "gzip")
        .get())

      result.header(VARY) must beSome(ACCEPT_ENCODING)
      //result.header(CONTENT_ENCODING) must beSome("gzip")
      val ahcResult: play.shaded.ahc.org.asynchttpclient.Response = result.underlying.asInstanceOf[play.shaded.ahc.org.asynchttpclient.Response]
      val is = new ByteArrayInputStream(ahcResult.getResponseBodyAsBytes)
      IOUtils.toString(is, StandardCharsets.UTF_8) must_== "This is a test gzipped asset.\n"
      // release deflate resources
      is.close()
      success
    }

    "return not modified when etag matches" in withServer { client =>
      val Some(etag) = await(client.url("/foo.txt").get()).header(ETAG)
      val result = await(client.url("/foo.txt")
        .withHeaders(IF_NONE_MATCH -> etag)
        get ())

      result.status must_== NOT_MODIFIED
      result.body must beEmpty
      result.header(CACHE_CONTROL) must_== defaultCacheControl
      result.header(ETAG) must beSome(matching(etagPattern))
      result.header(LAST_MODIFIED) must beSome
    }

    "return not modified when multiple etags supply and one matches" in withServer { client =>
      val Some(etag) = await(client.url("/foo.txt").get()).header(ETAG)
      val result = await(client.url("/foo.txt")
        .withHeaders(IF_NONE_MATCH -> ("\"foo\", " + etag + ", \"bar\""))
        .get())

      result.status must_== NOT_MODIFIED
      result.body must beEmpty
    }

    "return asset when etag doesn't match" in withServer { client =>
      val result = await(client.url("/foo.txt")
        .withHeaders(IF_NONE_MATCH -> "\"foobar\"")
        .get())

      result.status must_== OK
      result.body must_== "This is a test asset."
    }

    "return not modified when not modified since" in withServer { client =>
      val Some(timestamp) = await(client.url("/foo.txt").get()).header(LAST_MODIFIED)
      val result = await(client.url("/foo.txt")
        .withHeaders(IF_MODIFIED_SINCE -> timestamp)
        .get())

      result.status must_== NOT_MODIFIED
      result.body must beEmpty

      // Per https://tools.ietf.org/html/rfc7231#section-7.1.1.2
      // An origin server MUST send a Date header field if not 1xx or 5xx.
      result.header(DATE) must beSome
      result.header(ETAG) must beNone
      result.header(CACHE_CONTROL) must beNone
    }

    "return asset when modified since" in withServer { client =>
      val result = await(client.url("/foo.txt")
        .withHeaders(IF_MODIFIED_SINCE -> "Tue, 13 Mar 2012 13:08:36 GMT")
        .get())

      result.status must_== OK
      result.body must_== "This is a test asset."
    }

    "ignore if modified since header if if none match header is set" in withServer { client =>
      val result = await(client.url("/foo.txt")
        .withHeaders(
          IF_NONE_MATCH -> "\"foobar\"",
          IF_MODIFIED_SINCE -> "Wed, 01 Jan 2113 00:00:00 GMT" // might break in 100 years, but I won't be alive, so :P
        ).get())

      result.status must_== OK
      result.body must_== "This is a test asset."
    }

    "return the asset if the if modified since header can't be parsed" in withServer { client =>
      val result = await(client.url("/foo.txt")
        .withHeaders(IF_MODIFIED_SINCE -> "Not a date")
        .get())

      result.status must_== OK
      result.body must_== "This is a test asset."
    }

    "return 200 if the asset is empty" in withServer { client =>
      val result = await(client.url("/empty.txt").get())

      result.status must_== OK
      result.body must beEmpty
    }

    "return 404 for files that don't exist" in withServer { client =>
      val result = await(client.url("/nosuchfile.txt").get())

      result.status must_== NOT_FOUND
      result.header(CONTENT_TYPE) must beSome.which(_.startsWith("text/html"))
    }

    "serve a versioned asset" in withServer { client =>
      val result = await(client.url("/versioned/sub/12345678901234567890123456789012-foo.txt").get())

      result.status must_== OK
      result.body must_== "This is a test asset."
      result.header(CONTENT_TYPE) must beSome.which(_.startsWith("text/plain"))
      result.header(ETAG) must_== Some("\"12345678901234567890123456789012\"")
      result.header(LAST_MODIFIED) must beSome
      result.header(VARY) must beNone
      result.header(CONTENT_ENCODING) must beNone
      result.header(CACHE_CONTROL) must_== aggressiveCacheControl
    }

    "return not found when the path is a directory" in {
      "if the directory is on the file system" in withServer { client =>
        await(client.url("/subdir").get()).status must_== NOT_FOUND
      }
      "if the directory is a jar entry" in {
        Server.withApplicationFromContext() { context =>
          new BuiltInComponentsFromContext(context) with AssetsComponents with HttpFiltersComponents {
            override def router: Router = Router.from {
              case req => assets.versioned("/scala", req.path)
            }
          }.application
        } {
          withClient { client =>
            await(client.url("/collection").get()).status must_== NOT_FOUND
          }(_)
        }
      }
    }

    "serve a partial content if requested" in {
      "return a 206 Partial Content status" in withServer { client =>
        val result = await(
          client.url("/range.txt")
            .withHeaders(RANGE -> "bytes=0-10")
            .get()
        )

        result.status must_== PARTIAL_CONTENT
      }

      "The first 500 bytes: 0-499 inclusive" in withServer { client =>
        val result = await(
          client.url("/range.txt")
            .withHeaders(RANGE -> "bytes=0-499")
            .get()
        )

        result.status must_== PARTIAL_CONTENT
        result.header(CONTENT_RANGE) must beSome.which(_.startsWith("bytes 0-499/"))
        result.bodyAsBytes.length must beEqualTo(500)
        result.header(CONTENT_LENGTH) must beSome("500")
      }

      "The second 500 bytes: 500-999 inclusive" in withServer { client =>
        val result = await(
          client.url("/range.txt")
            .withHeaders(RANGE -> "bytes=500-999")
            .get()
        )

        result.bodyAsBytes.length must beEqualTo(500)
        result.status must_== PARTIAL_CONTENT
        result.header(CONTENT_RANGE) must beSome.which(_.startsWith("bytes 500-999/"))
        result.bodyAsBytes.length must beEqualTo(500)
        result.header(CONTENT_LENGTH) must beSome("500")
      }

      "The final 500 bytes: 9500-9999, inclusive" in withServer { client =>
        val result = await(
          client.url("/range.txt")
            .withHeaders(RANGE -> "bytes=9500-9999")
            .get()
        )

        result.status must_== PARTIAL_CONTENT
        result.header(CONTENT_RANGE) must beSome.which(_.startsWith("bytes 9500-9999/"))
        result.bodyAsBytes.length must beEqualTo(500)
        result.header(CONTENT_LENGTH) must beSome("500")
      }

      "The final 500 bytes using a open range: 9500-" in withServer { client =>
        val result = await(
          client.url("/range.txt")
            .withHeaders(RANGE -> "bytes=9500-")
            .get()
        )

        result.status must_== PARTIAL_CONTENT
        result.header(CONTENT_RANGE) must beSome.which(_.startsWith("bytes 9500-9999/10000"))
        result.bodyAsBytes.length must beEqualTo(500)
        result.header(CONTENT_LENGTH) must beSome("500")
      }

      "The first and last bytes only: 0 and 9999: bytes=0-0,-1" in withServer { client =>
        val result = await(
          client.url("/range.txt")
            .withHeaders(RANGE -> "bytes=0-0,-1")
            .get()
        )

        result.status must_== PARTIAL_CONTENT
        result.header(CONTENT_RANGE) must beSome.which(_.startsWith("bytes 0-0,-1/"))
      }.pendingUntilFixed

      "Multiple intervals to get the second 500 bytes" in withServer { client =>
        val result = await(
          client.url("/range.txt")
            .withHeaders(RANGE -> "bytes=500-600,601-999")
            .get()
        )

        result.status must_== PARTIAL_CONTENT
        result.header(CONTENT_TYPE) must beSome.which(_.startsWith("multipart/byteranges"))
      }.pendingUntilFixed

      "Return status 416 when first byte is gt the length of the complete entity" in withServer { client =>
        val result = await(
          client.url("/range.txt")
            .withHeaders(RANGE -> "bytes=10500-10600")
            .get()
        )

        result.status must_== REQUESTED_RANGE_NOT_SATISFIABLE
      }

      "Return a Content-Range header for 416 responses" in withServer { client =>
        val result = await(
          client.url("/range.txt")
            .withHeaders(RANGE -> "bytes=10500-10600")
            .get()
        )

        result.header(CONTENT_RANGE) must beSome.which(_ == "bytes */10000")
      }

      "No Content-Disposition header when serving assets" in withServer { client =>
        val result = await(
          client.url("/range.txt")
            .withHeaders(RANGE -> "bytes=10500-10600")
            .get()
        )

        result.header(CONTENT_DISPOSITION) must beNone
      }
    }
  }
}
