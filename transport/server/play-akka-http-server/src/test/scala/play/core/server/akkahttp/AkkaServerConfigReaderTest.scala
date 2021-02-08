/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.akkahttp

import akka.http.scaladsl.model._
import org.specs2.mutable.Specification
import akka.http.scaladsl.model.headers.Host
import play.api.Configuration

class AkkaServerConfigReaderTest extends Specification {
  "AkkaServerConfigReader.getHostHeader" should {
    "parse Host header without port number" in {
      val reader = new AkkaServerConfigReader(Configuration("default-host-header" -> "localhost"))
      val actual = reader.getHostHeader

      actual must beRight(Host("localhost"))
    }

    "parse Host header with port number" in {
      val reader = new AkkaServerConfigReader(Configuration("default-host-header" -> "localhost:4000"))
      val actual = reader.getHostHeader

      actual must beRight(Host("localhost", 4000))
    }

    "fail to parse an invalid host address" in {
      val reader = new AkkaServerConfigReader(Configuration("default-host-header" -> "localhost://"))
      val actual = reader.getHostHeader

      actual must beLeft
    }
  }
}
