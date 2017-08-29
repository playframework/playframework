/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it

import org.specs2.execute._
import org.specs2.mutable.{ Specification, SpecificationLike }
import org.specs2.specification.AroundEach
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.core.server.{ NettyServer, ServerProvider }
import play.core.server.AkkaHttpServer

import scala.concurrent.duration._

/**
 * Helper for creating tests that test integration with different server
 * backends. Common integration tests should implement this trait, then
 * two specific tests should be created, one extending NettyIntegrationSpecification
 * and another extending AkkaHttpIntegrationSpecification.
 *
 * When a test extends this trait it will automatically get overridden versions of
 * TestServer and WithServer that delegate to the correct server backend.
 */
trait ServerIntegrationSpecification extends PendingUntilFixed with AroundEach {
  parent =>
  implicit def integrationServerProvider: ServerProvider

  def aroundEventually[R: AsResult](r: => R) = {
    EventuallyResults.eventually[R](1, 20.milliseconds)(r)
  }

  def around[R: AsResult](r: => R) = {
    AsResult(aroundEventually(r))
  }

  implicit class UntilAkkaHttpFixed[T: AsResult](t: => T) {
    /**
     * We may want to skip some tests if they're slow due to timeouts. This tag
     * won't remind us if the tests start passing.
     */
    def skipUntilAkkaHttpFixed: Result = parent match {
      case _: NettyIntegrationSpecification => ResultExecution.execute(AsResult(t))
      case _: AkkaHttpIntegrationSpecification => Skipped()
    }
  }

  implicit class UntilNettyHttpFixed[T: AsResult](t: => T) {
    /**
     * We may want to skip some tests if they're slow due to timeouts. This tag
     * won't remind us if the tests start passing.
     */
    def skipUntilNettyHttpFixed: Result = parent match {
      case _: NettyIntegrationSpecification => Skipped()
      case _: AkkaHttpIntegrationSpecification => ResultExecution.execute(AsResult(t))
    }
  }

  implicit class UntilFastCIServer[T: AsResult](t: => T) {
    def skipOnSlowCIServer: Result = parent match {
      case _ if isContinuousIntegrationEnvironment => Skipped()
      case _ => ResultExecution.execute(AsResult(t))
    }
  }

  // There are some tests that we still want to run, but Travis CI will fail
  // because the server is underpowered...
  def isContinuousIntegrationEnvironment: Boolean = {
    System.getenv("CONTINUOUS_INTEGRATION") == "true"
  }

  /**
   * Override the standard TestServer factory method.
   */
  def TestServer(
    port: Int,
    application: Application = play.api.PlayCoreTestApplication(),
    sslPort: Option[Int] = None): play.api.test.TestServer = {
    play.api.test.TestServer(port, application, sslPort, Some(integrationServerProvider))
  }

  /**
   * Override the standard WithServer class.
   */
  abstract class WithServer(
    app: play.api.Application = GuiceApplicationBuilder().build(),
    port: Int = play.api.test.Helpers.testServerPort)
      extends play.api.test.WithServer(
        app, port, serverProvider = Some(integrationServerProvider)
      )

}

/** Run integration tests against a Netty server */
trait NettyIntegrationSpecification extends ServerIntegrationSpecification {
  self: SpecificationLike =>
  // Provide a flag to disable Netty tests
  private val runTests: Boolean = (System.getProperty("run.netty.http.tests", "true") == "true")
  skipAllIf(!runTests)

  final override def integrationServerProvider: ServerProvider = NettyServer.provider
}

/** Run integration tests against an Akka HTTP server */
trait AkkaHttpIntegrationSpecification extends ServerIntegrationSpecification {
  self: SpecificationLike =>

  final override def integrationServerProvider: ServerProvider = AkkaHttpServer.provider
}
