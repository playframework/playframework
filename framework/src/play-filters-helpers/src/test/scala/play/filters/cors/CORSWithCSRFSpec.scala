/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.filters.cors

import javax.inject.Inject

import play.api.Application
import play.api.http.{ ContentTypes, HttpFilters }
import play.api.inject.bind
import play.api.mvc.{ Action, Results }
import play.api.routing.Router
import play.api.routing.sird._
import play.filters.csrf._

object CORSWithCSRFSpec {

  class Filters @Inject()(corsFilter: CORSFilter, csrfFilter: CSRFFilter) extends HttpFilters {
    def filters = Seq(corsFilter, csrfFilter)
  }

  class FiltersWithoutCors @Inject()(csrfFilter: CSRFFilter) extends HttpFilters {
    def filters = Seq(csrfFilter)
  }

}

class CORSWithCSRFSpec extends CORSCommonSpec {
  import play.api.libs.crypto._
  import java.time.{ Clock, Instant, ZoneId }

  def tokenSigner = {
    val cryptoConfig = CryptoConfig("0123456789abcdef", None, "AES")
    val clock = Clock.fixed(Instant.ofEpochMilli(0L), ZoneId.systemDefault)
    val signer = new HMACSHA1CookieSigner(cryptoConfig)
    new DefaultCSRFTokenSigner(signer, clock)
  }

  def withApp[T](filters: Class[_ <: HttpFilters] = classOf[CORSWithCSRFSpec.Filters], conf: Map[String, _ <: Any] = Map())(block: Application => T): T = {
    running(_.configure(conf).overrides(
      bind[Router].to(Router.from {
        case p"/error" => Action { req => throw sys.error("error") }
        case _ => 
          val csrfCheck = new CSRFCheck(play.filters.csrf.CSRFConfig(), tokenSigner)
          csrfCheck(Action(Results.Ok), CSRF.DefaultErrorHandler)
      }),
      bind[HttpFilters].to(filters)
    ))(block)
  }

  def withApplication[T](conf: Map[String, _] = Map.empty)(block: Application => T) =
    withApp(classOf[CORSWithCSRFSpec.Filters], conf)(block)

  private def corsRequest =
    fakeRequest("POST", "/baz")
        .withHeaders(
          ORIGIN -> "http://localhost",
          CONTENT_TYPE -> ContentTypes.FORM,
          COOKIE -> "foo=bar"
        )
        .withBody("foo=1&bar=2")

  "The CORSFilter" should {

    "Mark CORS requests so the CSRF filter will let them through" in withApp() { app =>
      val result = route(app, corsRequest).get

      status(result) must_== OK
      header(ACCESS_CONTROL_ALLOW_ORIGIN, result) must beSome
    }

    "Forbid CSRF requests when CORS filter is not installed" in withApp(classOf[CORSWithCSRFSpec.FiltersWithoutCors]) { app =>
      val result = route(app, corsRequest).get

      status(result) must_== FORBIDDEN
      header(ACCESS_CONTROL_ALLOW_ORIGIN, result) must beNone
    }

    commonTests
  }
}

