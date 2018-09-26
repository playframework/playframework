/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server

import java.io.File
import java.util.Properties
import org.specs2.mutable.Specification
import play.core.ApplicationProvider

class ServerConfigSpec extends Specification {

  "ServerConfig construction" should {
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
