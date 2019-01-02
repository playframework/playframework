/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.runsupport

import org.specs2.mutable._
import org.specs2.execute.Result

class FilterArgsSpec extends Specification {

  val defaultHttpPort = 9000
  val defaultHttpAddress = "0.0.0.0"

  def check(args: String*)(
    properties: Seq[(String, String)] = Seq.empty,
    httpPort: Option[Int] = Some(defaultHttpPort),
    httpsPort: Option[Int] = None,
    httpAddress: String = defaultHttpAddress,
    devSettings: Seq[(String, String)] = Seq.empty): Result = {

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
      val devSettings: Seq[(String, String)] = Seq("play.server.http.port" -> "1234")
      val result = Reloader.filterArgs(Seq.empty, defaultHttpPort, defaultHttpAddress, devSettings)
      result must_== ((Seq.empty, Some(1234), None, defaultHttpAddress))
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
      val devSettings: Seq[(String, String)] = Seq("play.server.https.port" -> "1234")
      val result = Reloader.filterArgs(Seq.empty, defaultHttpPort, defaultHttpAddress, devSettings)
      result must_== ((Seq.empty, Some(9000), Some(1234), defaultHttpAddress))
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
      val devSettings: Seq[(String, String)] = Seq("play.server.http.address" -> "not-default-address")
      val result = Reloader.filterArgs(Seq.empty, defaultHttpPort, defaultHttpAddress, devSettings)
      result must_== ((Seq.empty, Some(9000), None, "not-default-address"))
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
