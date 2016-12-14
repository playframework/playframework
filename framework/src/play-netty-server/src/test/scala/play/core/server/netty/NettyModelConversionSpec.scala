/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.server.netty

import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http._
import org.specs2.mutable.Specification
import play.api.http.HttpConfiguration
import play.core.server.common.ForwardedHeaderHandler.Rfc7239
import play.core.server.common.{ ForwardedHeaderHandler, ServerResultUtils }

class NettyModelConversionSpec extends Specification {

  private val localChannel = new EmbeddedChannel()
  private val serverResultUtils = new ServerResultUtils(HttpConfiguration())
  private val headerHandler = new ForwardedHeaderHandler(new ForwardedHeaderHandler.ForwardedHeaderHandlerConfig(Rfc7239, List()))
  private val nettyModelConversion = new NettyModelConversion(serverResultUtils, headerHandler)

  "convertRequest" should {
    "handle '/pat/resources/BodhiApplication?where={%22name%22:%22hsdashboard%22}' as a URI" in {
      val request = nettyModelConversion.convertRequest(localChannel, new DefaultHttpRequest(
        HttpVersion.HTTP_1_1,
        HttpMethod.GET, "/pat/resources/BodhiApplication?where={%22name%22:%22hsdashboard%22}")).get

      request.path must_=== "/pat/resources/BodhiApplication"
      request.queryString must_=== Map("where" -> Seq("{\"name\":\"hsdashboard\"}"))
    }
    "handle '/dynatable/?queries%5Bsearch%5D=%7B%22condition%22%3A%22AND%22%2C%22rules%22%3A%5B%5D%7D&page=1&perPage=10&offset=0' as a URI" in {
      val request = nettyModelConversion.convertRequest(localChannel, new DefaultHttpRequest(
        HttpVersion.HTTP_1_1,
        HttpMethod.GET, "/dynatable/?queries%5Bsearch%5D=%7B%22condition%22%3A%22AND%22%2C%22rules%22%3A%5B%5D%7D&page=1&perPage=10&offset=0")).get

      request.path must_=== "/dynatable/"
      request.queryString must_=== Map("queries[search]" -> Seq("{\"condition\":\"AND\",\"rules\":[]}"), "page" -> Seq("1"), "perPage" -> Seq("10"), "offset" -> Seq("0"))
    }
    "handle '/foo%20bar.txt' as a URI" in {
      val request = nettyModelConversion.convertRequest(localChannel, new DefaultHttpRequest(
        HttpVersion.HTTP_1_1,
        HttpMethod.GET, "/foo%20bar.txt")).get

      request.path must_=== "/foo%20bar.txt"
    }
    "handle '/?filter=a&filter=b' as a URI" in {
      val request = nettyModelConversion.convertRequest(localChannel, new DefaultHttpRequest(
        HttpVersion.HTTP_1_1,
        HttpMethod.GET, "/?filter=a&filter=b")).get

      request.path must_=== "/"
      request.queryString must_=== Map("filter" -> Seq("a", "b"))
    }
  }

}
