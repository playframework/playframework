/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.csp

import akka.stream.Materializer
import com.typesafe.config.ConfigFactory
import javax.inject.Inject
import play.api.http.{ HttpFilters, NoHttpFilters }
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.mvc.Results._
import play.api.routing.{ Router, SimpleRouterImpl }
import play.api.test.{ FakeRequest, PlaySpecification }
import play.api.{ Application, Configuration }

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag

object ScalaCSPActionSpec {
  class CSPResultRouter @Inject() (action: CSPActionBuilder)
    extends SimpleRouterImpl({ case _ => action(Ok("hello")) })

  class AssetAwareRouter @Inject() (action: AssetAwareCSPActionBuilder)
    extends SimpleRouterImpl({ case _ => action(Ok("hello")) })

  class AssetAwareCSPActionBuilder @Inject() (bodyParsers: PlayBodyParsers, cspConfig: CSPConfig, assetCache: AssetCache)(
      implicit
      override protected val executionContext: ExecutionContext,
      override protected val mat: Materializer)
    extends CSPActionBuilder {

    override def parser: BodyParser[AnyContent] = bodyParsers.default

    override protected def cspResultProcessor: CSPResultProcessor = {
      val modifiedDirectives: Seq[CSPDirective] = cspConfig.directives.map {
        case CSPDirective(name, value) if name == "script-src" =>
          CSPDirective(name, value + assetCache.cspDigests.mkString(" "))
        case csp: CSPDirective =>
          csp
      }

      CSPResultProcessor(CSPProcessor(cspConfig.copy(directives = modifiedDirectives)))
    }
  }

  // Dummy class that can have a dynamically changing list of csp-hashes
  class AssetCache {
    def cspDigests: Seq[String] = {
      Seq(
        "sha256-HELLO",
        "sha256-WORLD"
      )
    }
  }
}

class ScalaCSPActionSpec extends PlaySpecification {
  import ScalaCSPActionSpec._

  sequential

  def inject[T: ClassTag](implicit app: Application) =
    app.injector.instanceOf[T]

  def configure(rawConfig: String) = {
    val typesafeConfig = ConfigFactory.parseString(rawConfig)
    Configuration(typesafeConfig)
  }

  "CSPActionBuilder" should {

    def withApplication[T](config: String)(block: Application => T): T = {
      val app = new GuiceApplicationBuilder()
        .configure(configure(config))
        .overrides(
          bind[Router].to[CSPResultRouter],
          bind[HttpFilters].to[NoHttpFilters]
        ).build
      running(app)(block(app))
    }

    "work even when there are no filters" in withApplication(
      """
        |play.filters.csp.nonce.header=true
      """.stripMargin) { implicit app =>

        val result = route(app, FakeRequest()).get

        val nonce = header(X_CONTENT_SECURITY_POLICY_NONCE_HEADER, result).get
        header(CONTENT_SECURITY_POLICY, result).get must contain(nonce)
      }
  }

  "Dynamic CSPActionBuilder" should {

    def withApplication[T](config: String)(block: Application => T): T = {
      val app = new GuiceApplicationBuilder()
        .configure(configure(config))
        .overrides(
          bind[Router].to[AssetAwareRouter],
          bind[HttpFilters].to[NoHttpFilters]
        ).build
      running(app)(block(app))
    }

    "work with a processor that is passed in dynamically" in withApplication(
      """
       |play.filters.csp.nonce.header=true
     """.stripMargin) { implicit app =>
        val result = route(app, FakeRequest()).get

        val nonce = header(X_CONTENT_SECURITY_POLICY_NONCE_HEADER, result).get

        header(CONTENT_SECURITY_POLICY, result).get must contain(nonce)
        header(CONTENT_SECURITY_POLICY, result).get must contain("sha256-HELLO")
      }

  }

}
