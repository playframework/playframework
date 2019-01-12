/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import org.openjdk.jmh.annotations._

/**
 * This benchmark reads a header from a RequestHeader object.
 */
@State(Scope.Benchmark)
class RequestHeader_01_ReadHeaderValue {

  var requestHeader: RequestHeader = null
  var result: String = null

  @Setup(Level.Iteration)
  def setup(): Unit = {
    requestHeader = MvcHelpers.requestHeaderFromHeaders(List(
      "Accept-Encoding" -> "gzip, deflate, sdch, br",
      "Host" -> "www.playframework.com",
      "Accept-Language" -> "en-US,en;q=0.8",
      "Upgrade-Insecure-Requests" -> "1",
      "User-Agent" -> "Mozilla/9.9 (Macintosh; Intel Mac OS X 10_99_9) AppleWebKit/999.99 (KHTML, like Gecko) Chrome/99.9.9999.999 Safari/999.999",
      "Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
      "Cache-Control" -> "max-age=0",
      "Cookie" -> "__utma=99999999999999999999999999999999999999999999999999999; __utmz=999999999999999999999999999999999999999999999999999999999999999999999; _mkto_trk=999999999999999999999999999999999999999999999999999999999999999",
      "Connection" -> "keep-alive"
    ))
    result = null
  }

  @TearDown(Level.Iteration)
  def tearDown(): Unit = {
    // Check the benchmark got the correct result
    assert(result == "max-age=0")
  }

  @Benchmark
  def getCacheControlHeader(): Unit = {
    result = requestHeader.headers("Cache-Control")
  }
}
