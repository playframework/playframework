/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it

import org.specs2.execute._
import org.specs2.mutable.Specification
import org.specs2.specification.AroundEach
import play.api.Application
import play.core.server.{ NettyServer, ServerProvider }
import play.core.server.akkahttp.AkkaHttpServer
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

  /**
   * Retry up to 3 times.
   */
  def around[R: AsResult](r: => R) = {
    AsResult(EventuallyResults.eventually(1, 20.milliseconds)(r))
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
    app: play.api.Application = play.api.test.FakeApplication(),
    port: Int = play.api.test.Helpers.testServerPort) extends play.api.test.WithServer(
    app, port, serverProvider = Some(integrationServerProvider))

}
trait NettyIntegrationSpecification extends ServerIntegrationSpecification {
  override def integrationServerProvider: ServerProvider = NettyServer.provider
}
trait AkkaHttpIntegrationSpecification extends ServerIntegrationSpecification {
  self: Specification =>
  // Provide a flag to disable Akka HTTP tests
  private val runTests: Boolean = (System.getProperty("run.akka.http.tests", "true") == "true")
  skipAllIf(!runTests)
  override def integrationServerProvider: ServerProvider = AkkaHttpServer.provider
}
