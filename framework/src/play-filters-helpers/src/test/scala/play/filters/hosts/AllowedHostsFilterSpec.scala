/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.filters.hosts

import java.net.InetAddress
import javax.inject.Inject

import com.typesafe.config.ConfigFactory
import org.scalacheck.{ Arbitrary, Gen }
import org.specs2.ScalaCheck
import play.api.http.{ HeaderNames, HttpFilters }
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.mvc.Results._
import play.api.mvc.{ DefaultActionBuilder, RequestHeader, Result }
import play.api.routing.{ Router, SimpleRouterImpl }
import play.api.test.{ FakeRequest, PlaySpecification, TestServer }
import play.api.{ Application, Configuration }
import play.core.server.common.Subnet

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

class AllowedHostsFilterSpec extends PlaySpecification with ScalaCheck {

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

    type CIDR = Int
    type IpAddress = String

    final case class ValidAddress(ipAddress: IpAddress, cidr: CIDR)
    final case class InvalidAddress(ipAddress: IpAddress, cidr: CIDR)

    val all: Map[CIDR, (Range, Range)] = Map(
      32 -> (0 to 0, 0 to 0),
      31 -> (0 to 0, 0 until 2),
      30 -> (0 to 0, 0 until 4),
      29 -> (0 to 0, 0 until 8),
      28 -> (0 to 0, 0 until 16),
      27 -> (0 to 0, 0 until 32),
      26 -> (0 to 0, 0 until 64),
      25 -> (0 to 0, 0 until 128),
      24 -> (0 to 0, 0 until 256),
      23 -> (0 until 2, 0 until 256),
      22 -> (0 until 4, 0 until 256),
      21 -> (0 until 8, 0 until 256),
      20 -> (0 until 16, 0 until 256),
      19 -> (0 until 32, 0 until 256),
      18 -> (0 until 64, 0 until 256),
      17 -> (0 until 128, 0 until 256),
      16 -> (0 until 256, 0 until 256)
    )

    def genCIDR: Gen[CIDR] = Gen.choose(16, 32)
    def genIpRanges(cidr: CIDR): Gen[(Range, Range)] = Gen.const(all(cidr))

    def genValidAddress: Gen[ValidAddress] =
      for {
        cidr <- genCIDR
        (as, bs) <- genIpRanges(cidr)
        a <- Gen.choose(as.min, as.max)
        b <- Gen.choose(bs.min, bs.max)
      } yield ValidAddress(s"10.0.$a.$b", cidr)

    implicit val arbValidAddress: Arbitrary[ValidAddress] = Arbitrary(genValidAddress)

    def genInvalidAddress: Gen[InvalidAddress] = {
      val addGen = for {
        cidr <- genCIDR
        a <- Gen.choose(0, 255)
        b <- Gen.choose(0, 255)
        c <- Gen.choose(0, 255)
      } yield s"10.$a.$b.$c" -> cidr

      addGen
        .filter { case ((ipAddress, cidr)) => !Subnet(s"10.0.0.0/$cidr").isInRange(InetAddress.getByName(ipAddress)) }
        .map { case ((ipAddress, cidr)) => InvalidAddress(ipAddress, cidr) }
    }

    implicit val arbInvalidAddress: Arbitrary[InvalidAddress] = Arbitrary(genInvalidAddress)

    "allow IP address inside CIDR range 10.0.0.0/x" >> prop { (address: ValidAddress) =>
      withApplication(
        okWithHost,
        s"""
           |play.filters.hosts.allowed = ["10.0.0.0/${address.cidr}"]
      """.stripMargin) { app =>
          status(request(app, address.ipAddress)) must_== OK
        }
    }

    "reject IP address outside CIDR range 10.0.0.0/x" >> prop { (address: InvalidAddress) =>
      withApplication(
        okWithHost,
        s"""
           |play.filters.hosts.allowed = ["10.0.0.0/${address.cidr}"]
      """.stripMargin) { app =>
          status(request(app, address.ipAddress)) must_== BAD_REQUEST
        }
    }
  }
}
