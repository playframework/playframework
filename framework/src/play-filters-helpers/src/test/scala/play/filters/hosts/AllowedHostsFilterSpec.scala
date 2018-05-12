/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.hosts

import javax.inject.Inject

import com.typesafe.config.ConfigFactory
import play.api.http.{ HeaderNames, HttpFilters }
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.mvc.Results._
import play.api.mvc.{ DefaultActionBuilder, RequestHeader, Result }
import play.api.routing.{ Router, SimpleRouterImpl }
import play.api.test.{ FakeRequest, PlaySpecification, TestServer }
import play.api.{ Application, Configuration }

import scala.concurrent.Await
import scala.concurrent.duration._

object AllowedHostsFilterSpec {
  class Filters @Inject() (allowedHostsFilter: AllowedHostsFilter) extends HttpFilters {
    def filters = Seq(allowedHostsFilter)
  }

  case class ActionHandler(result: RequestHeader => Result) extends (RequestHeader => Result) {
    def apply(rh: RequestHeader) = result(rh)
  }

  class MyRouter @Inject() (action: DefaultActionBuilder, result: ActionHandler) extends SimpleRouterImpl({
    case request => action(result(request))
  })
}

class AllowedHostsFilterSpec extends PlaySpecification {

  sequential

  import AllowedHostsFilterSpec._

  private def request(app: Application, hostHeader: String, uri: String = "/", headers: Seq[(String, String)] = Seq()) = {
    val req = FakeRequest(method = "GET", path = uri)
      .withHeaders(headers: _*)
      .withHeaders(HOST -> hostHeader)
    route(app, req).get
  }

  private val okWithHost = (req: RequestHeader) => Ok(req.host)

  def newApplication(result: RequestHeader => Result, config: String): Application = {
    new GuiceApplicationBuilder()
      .configure(Configuration(ConfigFactory.parseString(config)))
      .overrides(
        bind[ActionHandler].to(ActionHandler(result)),
        bind[Router].to[MyRouter],
        bind[HttpFilters].to[Filters]
      )
      .build()
  }

  def withApplication[T](result: RequestHeader => Result, config: String)(block: Application => T): T = {
    val app = newApplication(result, config)
    running(app)(block(app))
  }

  val TestServerPort = 8192
  def withServer[T](result: RequestHeader => Result, config: String)(block: WSClient => T): T = {
    val app = newApplication(result, config)
    running(TestServer(TestServerPort, app))(block(app.injector.instanceOf[WSClient]))
  }

  "the allowed hosts filter" should {
    "disallow non-local hosts with default config" in withApplication(okWithHost, "") { app =>
      status(request(app, "localhost")) must_== OK
      status(request(app, "typesafe.com")) must_== BAD_REQUEST
      status(request(app, "")) must_== BAD_REQUEST
    }

    "only allow specific hosts specified in configuration" in withApplication(
      okWithHost,
      """
        |play.filters.hosts.allowed = ["example.com", "example.net"]
      """.stripMargin) { app =>
        status(request(app, "example.com")) must_== OK
        status(request(app, "EXAMPLE.net")) must_== OK
        status(request(app, "example.org")) must_== BAD_REQUEST
        status(request(app, "foo.example.com")) must_== BAD_REQUEST
      }

    "allow defining host suffixes in configuration" in withApplication(
      okWithHost,
      """
        |play.filters.hosts.allowed = [".example.com"]
      """.stripMargin) { app =>
        status(request(app, "foo.example.com")) must_== OK
        status(request(app, "example.com")) must_== OK
      }

    "support FQDN format for hosts" in withApplication(
      okWithHost,
      """
        |play.filters.hosts.allowed = [".example.com", "example.net"]
      """.stripMargin) { app =>
        status(request(app, "foo.example.com.")) must_== OK
        status(request(app, "example.net.")) must_== OK
      }

    "support allowing empty hosts" in withApplication(
      okWithHost,
      """
        |play.filters.hosts.allowed = [".example.com", ""]
      """.stripMargin) { app =>
        status(request(app, "")) must_== OK
        status(request(app, "example.net")) must_== BAD_REQUEST
        status(route(app, FakeRequest().withHeaders(HeaderNames.HOST -> "")).get) must_== OK
      }

    "support host headers with ports" in withApplication(
      okWithHost,
      """
        |play.filters.hosts.allowed = ["example.com"]
      """.stripMargin) { app =>
        status(request(app, "example.com:80")) must_== OK
        status(request(app, "google.com:80")) must_== BAD_REQUEST
      }

    "restrict host headers based on port" in withApplication(
      okWithHost,
      """
        |play.filters.hosts.allowed = [".example.com:8080"]
      """.stripMargin) { app =>
        status(request(app, "example.com:80")) must_== BAD_REQUEST
        status(request(app, "www.example.com:8080")) must_== OK
        status(request(app, "example.com:8080")) must_== OK
      }

    "support matching all hosts" in withApplication(
      okWithHost,
      """
        |play.filters.hosts.allowed = ["."]
      """.stripMargin) { app =>
        status(request(app, "example.net")) must_== OK
        status(request(app, "amazon.com")) must_== OK
        status(request(app, "")) must_== OK
      }

    // See http://www.skeletonscribe.net/2013/05/practical-http-host-header-attacks.html

    "not allow malformed ports" in withApplication(
      okWithHost,
      """
        |play.filters.hosts.allowed = [".mozilla.org"]
      """.stripMargin) { app =>
        status(request(app, "addons.mozilla.org:@passwordreset.net")) must_== BAD_REQUEST
        status(request(app, "addons.mozilla.org: www.securepasswordreset.com")) must_== BAD_REQUEST
      }

    "validate hosts in absolute URIs" in withApplication(
      okWithHost,
      """
        |play.filters.hosts.allowed = [".mozilla.org"]
      """.stripMargin) { app =>
        status(request(app, "www.securepasswordreset.com", "https://addons.mozilla.org/en-US/firefox/users/pwreset")) must_== OK
        status(request(app, "addons.mozilla.org", "https://www.securepasswordreset.com/en-US/firefox/users/pwreset")) must_== BAD_REQUEST
      }

    "not allow bypassing with X-Forwarded-Host header" in withServer(
      okWithHost,
      """
        |play.filters.hosts.allowed = ["localhost"]
      """.stripMargin) { ws =>
        val wsRequest = ws.url(s"http://localhost:$TestServerPort").addHttpHeaders(X_FORWARDED_HOST -> "evil.com").get()
        val wsResponse = Await.result(wsRequest, 5.seconds)
        wsResponse.status must_== OK
        wsResponse.body must_== s"localhost:$TestServerPort"
      }
  }
}
