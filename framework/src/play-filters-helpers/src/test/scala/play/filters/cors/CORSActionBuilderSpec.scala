/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.filters.cors

import play.api.mvc.Results
import play.api.{ Application, Configuration }

class CORSActionBuilderSpec extends CORSCommonSpec {

  def withApplication[T](conf: Map[String, _ <: Any] = Map.empty)(block: Application => T): T = {
    running(_.routes {
      case (_, "/error") => CORSActionBuilder(Configuration.reference ++ Configuration.from(conf)) { req =>
        throw sys.error("error")
      }
      case _ => CORSActionBuilder(Configuration.reference ++ Configuration.from(conf))(Results.Ok)
    })(block)
  }

  def withApplicationWithPathConfiguredAction[T](configPath: String, conf: Map[String, _ <: Any] = Map.empty)(block: Application => T): T = {
    running(_.configure(conf).routes {
      case (_, "/error") => CORSActionBuilder(Configuration.reference ++ Configuration.from(conf), configPath = configPath) { req =>
        throw sys.error("error")
      }
      case _ => CORSActionBuilder(Configuration.reference ++ Configuration.from(conf), configPath = configPath)(Results.Ok)
    })(block)
  }

  "The CORSActionBuilder with" should {

    val restrictOriginsPathConf = Map("myaction.allowedOrigins" -> Seq("http://example.org", "http://localhost:9000"))

    "handle a cors request with a subpath of app configuration" in withApplicationWithPathConfiguredAction(configPath = "myaction", conf = restrictOriginsPathConf) {
      app =>
        val result = route(app, fakeRequest().withHeaders(ORIGIN -> "http://localhost:9000")).get

        status(result) must_== OK
        header(ACCESS_CONTROL_ALLOW_CREDENTIALS, result) must beSome("true")
        header(ACCESS_CONTROL_ALLOW_HEADERS, result) must beNone
        header(ACCESS_CONTROL_ALLOW_METHODS, result) must beNone
        header(ACCESS_CONTROL_ALLOW_ORIGIN, result) must beSome("http://localhost:9000")
        header(ACCESS_CONTROL_EXPOSE_HEADERS, result) must beNone
        header(ACCESS_CONTROL_MAX_AGE, result) must beNone
        header(VARY, result) must beSome(ORIGIN)
    }

    commonTests
  }
}

