package play.filters.redirectplain

import javax.inject.Inject

import com.typesafe.config.ConfigFactory
import play.api.{ Application, Configuration }
import play.api.http.HttpFilters
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.mvc.Results._
import play.api.mvc._
import play.api.routing.Router
import play.api.test._

class TestFilters @Inject() (redirectPlainFilter: RedirectPlainFilter) extends HttpFilters {
  override def filters: Seq[EssentialFilter] = Seq(redirectPlainFilter)
}

class RedirectPlainFilterSpec extends PlaySpecification {

  private def request(app: Application, xForwardedProto: String, uri: String = "/", headers: Seq[(String, String)] = Seq()) = {
    val req = FakeRequest(method = "GET", path = uri)
      .withHeaders(headers: _*)
      .withHeaders(HOST -> "localhost")
      .withHeaders(X_FORWARDED_PROTO -> xForwardedProto)
    route(app, req).get
  }

  def buildApp(config: String) = new GuiceApplicationBuilder()
    .configure(Configuration(ConfigFactory.parseString(config)))
    .overrides(
      bind[Router].to(Router.from {
        case _ => Action(Ok(""))
      }),
      bind[RedirectPlainConfig].toProvider[RedirectPlainConfigProvider],
      bind[HttpFilters].to[TestFilters]
    ).build

  def withApplication[T](config: String)(block: Application => T): T = {
    val app = buildApp(config)
    running(app)(block(app))
  }

  val TestServerPort = 8192
  def withServer[T](config: String)(block: WSClient => T): T = {
    val app = buildApp(config)
    running(TestServer(TestServerPort, app))(block(app.injector.instanceOf[WSClient]))
  }

  "RedirectPlainFilter" should {

    "do nothing when not explicitly enabled" in withApplication("") { app =>
      status(request(app, "http")) must_== OK
    }

    "redirect when not on https" in withApplication("filters.redirectplain.enabled=true") { app =>
      status(request(app, "http")) must_== MOVED_PERMANENTLY
      header(LOCATION, request(app, "http", "/test/123?foo=bar&bar=baz")) must_== Some ("https://localhost/test/123?foo=bar&bar=baz")
    }

    "not redirect when on https" in withApplication("filters.redirectplain.enabled=true") { app =>
      status(request(app, "https")) must_== OK
    }

    "add strict transport security headers on https" in withApplication(
      """
        |filters.redirectplain.enabled=true
        |filters.redirectplain.strict-transport-security.max-age=12345
      """.stripMargin) { app =>
      header(STRICT_TRANSPORT_SECURITY, request(app, "https")) must_== Some ("max-age=12345")
    }
  }

}
