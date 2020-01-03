/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.runsupport

import org.specs2.mutable._
import org.specs2.execute.Result

class FilterArgsSpec extends Specification {
  val defaultHttpPort    = 9000
  val defaultHttpAddress = "0.0.0.0"

  def check(args: String*)(
      properties: Seq[(String, String)] = Seq.empty,
      httpPort: Option[Int] = Some(defaultHttpPort),
      httpsPort: Option[Int] = None,
      httpAddress: String = defaultHttpAddress,
      devSettings: Seq[(String, String)] = Seq.empty
  ): Result = {
    val result = Reloader.filterArgs(args, defaultHttpPort, defaultHttpAddress, devSettings)
    result must_== ((properties, httpPort, httpsPort, httpAddress))
  }

  "Reloader.filterArgs" should {
    "support port argument" in {
      check("1234")(
        httpPort = Some(1234)
      )
    }

    "support disabled port argument" in {
      check("disabled")(
        httpPort = None
      )
    }

    "support port property with system property" in {
      check("-Dhttp.port=1234")(
        properties = Seq("http.port" -> "1234"),
        httpPort = Some(1234)
      )
    }

    "support port property with dev setting" in {
      check()(
        devSettings = Seq("play.server.http.port" -> "1234"),
        httpPort = Some(1234)
      )
    }

    "support overriding port property from dev setting by the one from command line" in {
      check("-Dhttp.port=9876")(
        devSettings = Seq("play.server.http.port" -> "1234"),
        properties = Seq("http.port"              -> "9876"),
        httpPort = Some(9876)
      )
    }

    "support overriding port from first non-property argument by the one supplied as property" in {
      check("5555", "-Dhttp.port=9876")(
        properties = Seq("http.port" -> "9876"),
        httpPort = Some(9876)
      )
    }

    "support port property long version from command line that overrides everything else" in {
      check("1234", "-Dplay.server.http.port=5555", "-Dhttp.port=9876")(
        devSettings = Seq("play.server.http.port" -> "5678"),
        properties = Seq("play.server.http.port"  -> "5555", "http.port" -> "9876"),
        httpPort = Some(5555)
      )
    }

    "support disabled port property" in {
      check("-Dhttp.port=disabled")(
        properties = Seq("http.port" -> "disabled"),
        httpPort = None
      )
    }

    "support https port property" in {
      check("-Dhttps.port=4321")(
        properties = Seq("https.port" -> "4321"),
        httpsPort = Some(4321)
      )
    }

    "support https only" in {
      check("-Dhttps.port=4321", "disabled")(
        properties = Seq("https.port" -> "4321"),
        httpPort = None,
        httpsPort = Some(4321)
      )
    }

    "support https port property with dev setting" in {
      check()(
        devSettings = Seq("play.server.https.port" -> "1234"),
        httpsPort = Some(1234)
      )
    }

    "support https disabled" in {
      check("-Dhttps.port=disabled", "-Dhttp.port=1234")(
        properties = Seq("https.port" -> "disabled", "http.port" -> "1234"),
        httpPort = Some(1234),
        httpsPort = None
      )
    }

    "support address property" in {
      check("-Dhttp.address=localhost")(
        properties = Seq("http.address" -> "localhost"),
        httpAddress = "localhost"
      )
    }

    "support address property with dev setting" in {
      check()(
        devSettings = Seq("play.server.http.address" -> "not-default-address"),
        httpAddress = "not-default-address"
      )
    }

    "support all options" in {
      check("-Dhttp.address=localhost", "-Dhttps.port=4321", "-Dtest.option=something", "1234")(
        properties = Seq("http.address" -> "localhost", "https.port" -> "4321", "test.option" -> "something"),
        httpPort = Some(1234),
        httpsPort = Some(4321),
        httpAddress = "localhost"
      )
    }
  }
}
