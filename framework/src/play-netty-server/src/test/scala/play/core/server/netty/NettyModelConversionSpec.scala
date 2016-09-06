/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.server.netty

import java.net.URISyntaxException

import org.specs2.mutable._
import play.core.server.common.ForwardedHeaderHandler
import play.core.server.common.ForwardedHeaderHandler.{ ForwardedHeaderHandlerConfig, Xforwarded }

class NettyModelConversionSpec extends Specification {

  val forwardedHeader = new ForwardedHeaderHandler(ForwardedHeaderHandlerConfig(Xforwarded, List()))
  val modelConversion = new NettyModelConversion(forwardedHeader)

  "NettyModelConversion#parseUri" should {

    "successfully parse a uri with special characters" in {
      val (path, query) = modelConversion.parseUri("/projects/1234/users/18620538586%20%7C%20上海/characteristic?hello=海")
      path must_== "/projects/1234/users/18620538586%20%7C%20上海/characteristic"
      query must_== Map("hello" -> List("海"))
    }

    "throw a URISyntaxException if it contains whitespace" in {
      modelConversion.parseUri("/projects/1234/users/186205 86%20%7C%") must throwA[URISyntaxException]
    }

  }

}
