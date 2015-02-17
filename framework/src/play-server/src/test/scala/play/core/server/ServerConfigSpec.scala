/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server

import java.io.File
import java.util.Properties
import org.specs2.mutable.Specification
import play.core.ApplicationProvider

object ServerConfigSpec extends Specification {

  "ServerConfig" should {
    "fail when both http and https ports are missing" in {
      ServerConfig(
        rootDir = new File("/asdasd"),
        port = None,
        sslPort = None,
        properties = new Properties()
      ) must throwAn[IllegalArgumentException]
    }
  }

}
