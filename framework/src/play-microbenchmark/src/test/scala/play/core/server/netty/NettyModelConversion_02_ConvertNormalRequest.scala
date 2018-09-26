/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.server.netty

import io.netty.channel.Channel
import io.netty.handler.codec.http.HttpRequest
import org.openjdk.jmh.annotations.{ TearDown, _ }
import play.api.http.HttpConfiguration
import play.api.mvc.RequestHeader
import play.api.mvc.request.{ DefaultRequestFactory, RequestTarget }

@State(Scope.Benchmark)
class NettyModelConversion_02_ConvertNormalRequest {

  // Cache some values that will be used in the benchmark
  private val nettyConversion = NettyHelpers.conversion
  private val requestFactory = new DefaultRequestFactory(HttpConfiguration())
  private val remoteAddress = NettyHelpers.localhost

  // Benchmark state
  private var channel: Channel = null
  private var request: HttpRequest = null
  private var result: RequestHeader = null

  @Setup(Level.Iteration)
  def setup(): Unit = {
    channel = NettyHelpers.nettyChannel(remoteAddress, ssl = false)
    request = NettyHelpers.nettyRequest(
      method = "GET",
      target = "/x/y/z",
      headers = List(
        "Accept-Encoding" -> "gzip, deflate, sdch, br",
        "Host" -> "www.playframework.com",
        "Accept-Language" -> "en-US,en;q=0.8",
        "Upgrade-Insecure-Requests" -> "1",
        "User-Agent" -> "Mozilla/9.9 (Macintosh; Intel Mac OS X 10_99_9) AppleWebKit/999.99 (KHTML, like Gecko) Chrome/99.9.9999.999 Safari/999.999",
        "Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
        "Cache-Control" -> "max-age=0",
        "Cookie" -> "__utma=99999999999999999999999999999999999999999999999999999; __utmz=999999999999999999999999999999999999999999999999999999999999999999999; _mkto_trk=999999999999999999999999999999999999999999999999999999999999999",
        "Connection" -> "keep-alive"
      )
    )
    result = null
  }

  @TearDown(Level.Iteration)
  def tearDown(): Unit = {
    // Sanity check the benchmark result
    assert(result.path == "/x/y/z")
  }

  @Benchmark
  def convertRequest(): Unit = {
    result = nettyConversion.convertRequest(channel, request).get
    result = requestFactory.copyRequestHeader(result)
  }
}