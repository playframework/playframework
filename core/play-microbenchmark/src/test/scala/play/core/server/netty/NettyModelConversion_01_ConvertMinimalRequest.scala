/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.netty

import io.netty.channel.Channel
import io.netty.handler.codec.http.HttpRequest
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.annotations.TearDown
import play.api.http.HttpConfiguration
import play.api.mvc.request.DefaultRequestFactory
import play.api.mvc.RequestHeader

@State(Scope.Benchmark)
class NettyModelConversion_01_ConvertMinimalRequest {
  // Cache some values that will be used in the benchmark
  private val nettyConversion = NettyHelpers.conversion
  private val requestFactory  = new DefaultRequestFactory(HttpConfiguration())
  private val remoteAddress   = NettyHelpers.localhost

  // Benchmark state
  private var channel: Channel      = null
  private var request: HttpRequest  = null
  private var result: RequestHeader = null

  @Setup(Level.Iteration)
  def setup(): Unit = {
    channel = NettyHelpers.nettyChannel(remoteAddress, ssl = false)
    request = NettyHelpers.nettyRequest(
      method = "GET",
      target = "/",
      headers = Nil
    )
    result = null
  }

  @TearDown(Level.Iteration)
  def tearDown(): Unit = {
    // Sanity check the benchmark result
    assert(result.path == "/")
  }

  @Benchmark
  def convertRequest(): Unit = {
    result = nettyConversion.convertRequest(channel, request).get
    result = requestFactory.copyRequestHeader(result)
  }
}
