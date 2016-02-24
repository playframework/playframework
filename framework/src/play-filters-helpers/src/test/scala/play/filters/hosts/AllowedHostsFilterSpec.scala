/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.filters.hosts

import javax.inject.Inject

import com.typesafe.config.ConfigFactory
import play.api.http.HttpFilters
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{ WSClient, WS }
import play.api.mvc.Results._
import play.api.mvc.{ Action, RequestHeader, Result }
import play.api.routing.Router
import play.api.test.{ FakeRequest, PlaySpecification, TestServer }
import play.api.{ Application, Configuration }

import scala.concurrent.Await
import scala.concurrent.duration._

object AllowedHostsFilterSpec extends PlaySpecification {

  sequential

  private def request(hostHeader: String, uri: String = "/", headers: Seq[(String, String)] = Seq()) = {
    val req = FakeRequest(method = "GET", path = uri)
      .withHeaders(headers: _*)
      .withHeaders(HOST -> hostHeader)
    route(req).get
  }

  private val okWithHost = (req: RequestHeader) => Ok(req.host)

  class Filters @Inject() (allowedHostsFilter: AllowedHostsFilter) extends HttpFilters {
    def filters = Seq(allowedHostsFilter)
  }

  def newApplication(result: RequestHeader => Result, config: String): Application = {
    new GuiceApplicationBuilder()
      .configure(Configuration(ConfigFactory.parseString(config)))
      .overrides(
        bind[Router].to(Router.from { case request => Action(result(request)) }),
        bind[HttpFilters].to[Filters]
      )
      .build()
  }

  def withApplication[T](result: RequestHeader => Result, config: String)(block: => T): T = {
    running(newApplication(result, config))(block)
  }

  val TestServerPort = 8192
  def withServer[T](result: RequestHeader => Result, config: String)(block: WSClient => T): T = {
    val app = newApplication(result, config)
    running(TestServer(TestServerPort, app))(block(app.injector.instanceOf[WSClient]))
  }

  "the allowed hosts filter" should {
    "disallow non-local hosts with default config" in withApplication(okWithHost, "") {
      status(request("localhost")) must_== OK
      status(request("typesafe.com")) must_== BAD_REQUEST
      status(request("")) must_== BAD_REQUEST
    }

    "only allow specific hosts specified in configuration" in withApplication(okWithHost,
      """
        |play.filters.hosts.allowed = ["example.com", "example.net"]
      """.stripMargin) {
        status(request("example.com")) must_== OK
        status(request("EXAMPLE.net")) must_== OK
        status(request("example.org")) must_== BAD_REQUEST
        status(request("foo.example.com")) must_== BAD_REQUEST
      }

    "allow defining host suffixes in configuration" in withApplication(okWithHost,
      """
        |play.filters.hosts.allowed = [".example.com"]
      """.stripMargin) {
        status(request("foo.example.com")) must_== OK
        status(request("example.com")) must_== OK
      }

    "support FQDN format for hosts" in withApplication(okWithHost,
      """
        |play.filters.hosts.allowed = [".example.com", "example.net"]
      """.stripMargin) {
        status(request("foo.example.com.")) must_== OK
        status(request("example.net.")) must_== OK
      }

    "support allowing empty hosts" in withApplication(okWithHost,
      """
        |play.filters.hosts.allowed = [".example.com", ""]
      """.stripMargin) {
        status(request("")) must_== OK
        status(request("example.net")) must_== BAD_REQUEST
        status(route(FakeRequest()).get) must_== OK
      }

    "support host headers with ports" in withApplication(okWithHost,
      """
        |play.filters.hosts.allowed = ["example.com"]
      """.stripMargin) {
        status(request("example.com:80")) must_== OK
        status(request("google.com:80")) must_== BAD_REQUEST
      }

    "restrict host headers based on port" in withApplication(okWithHost,
      """
        |play.filters.hosts.allowed = [".example.com:8080"]
      """.stripMargin) {
        status(request("example.com:80")) must_== BAD_REQUEST
        status(request("www.example.com:8080")) must_== OK
        status(request("example.com:8080")) must_== OK
      }

    "support matching all hosts" in withApplication(okWithHost,
      """
        |play.filters.hosts.allowed = ["."]
      """.stripMargin) {
        status(request("example.net")) must_== OK
        status(request("amazon.com")) must_== OK
        status(request("")) must_== OK
      }

    // See http://www.skeletonscribe.net/2013/05/practical-http-host-header-attacks.html

    "not allow malformed ports" in withApplication(okWithHost,
      """
        |play.filters.hosts.allowed = [".mozilla.org"]
      """.stripMargin) {
        status(request("addons.mozilla.org:@passwordreset.net")) must_== BAD_REQUEST
        status(request("addons.mozilla.org: www.securepasswordreset.com")) must_== BAD_REQUEST
      }

    "validate hosts in absolute URIs" in withApplication(okWithHost,
      """
        |play.filters.hosts.allowed = [".mozilla.org"]
      """.stripMargin) {
        status(request("www.securepasswordreset.com", "https://addons.mozilla.org/en-US/firefox/users/pwreset")) must_== OK
        status(request("addons.mozilla.org", "https://www.securepasswordreset.com/en-US/firefox/users/pwreset")) must_== BAD_REQUEST
      }

    "not allow bypassing with X-Forwarded-Host header" in withServer(okWithHost,
      """
        |play.filters.hosts.allowed = ["localhost"]
      """.stripMargin) { ws =>
        val wsRequest = ws.url(s"http://localhost:$TestServerPort").withHeaders(X_FORWARDED_HOST -> "evil.com").get()
        val wsResponse = Await.result(wsRequest, 1.second)
        wsResponse.status must_== OK
        wsResponse.body must_== s"localhost:$TestServerPort"
      }
  }
}
