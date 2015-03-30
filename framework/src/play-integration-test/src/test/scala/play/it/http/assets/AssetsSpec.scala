/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.http.assets

import controllers.Assets
import play.api.libs.ws.WSClient
import play.api.test._
import org.apache.commons.io.IOUtils
import java.io.ByteArrayInputStream
import play.api.Mode
import play.core.server.{ ServerConfig, Server }
import play.it._

object NettyAssetsSpec extends AssetsSpec with NettyIntegrationSpecification
object AkkaHttpAssetsSpec extends AssetsSpec with AkkaHttpIntegrationSpecification

trait AssetsSpec extends PlaySpecification
    with WsTestClient with ServerIntegrationSpecification {

  sequential

  "Assets controller" should {

    val defaultCacheControl = Some("public, max-age=3600")
    val aggressiveCacheControl = Some("public, max-age=31536000")

    def withServer[T](block: WSClient => T): T = {
      Server.withRouter(ServerConfig(mode = Mode.Prod, port = Some(0))) {
        case req => Assets.versioned("/testassets", req.path)
      } { implicit port =>
        withClient(block)
      }
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
      val ahcResult: com.ning.http.client.Response = result.underlying.asInstanceOf[com.ning.http.client.Response]
      val is = new ByteArrayInputStream(ahcResult.getResponseBodyAsBytes)
      IOUtils.toString(is) must_== "This is a test gzipped asset.\n"
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

      // I don't know why we implement this behaviour, I can't see it in the HTTP spec, but there were tests for it
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
        Server.withRouter() {
          case req => Assets.versioned("/scala", req.path)
        } { implicit port =>
          withClient { client =>
            await(client.url("/collection").get()).status must_== NOT_FOUND
          }
        }
      }
    }
  }
}
