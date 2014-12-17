package play.it

import play.api.http.HeaderNames._
import play.api.libs.iteratee._
import play.api.libs.ws._
import play.api.mvc._
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Results._
import play.api.test._
import play.core.server.ServerProvider
import scala.concurrent.Future
import scala.concurrent.duration._
import akka.util.Timeout

object NettyServerIntegrationSpecificationSpec extends ServerIntegrationSpecificationSpec with NettyIntegrationSpecification {
  override def isAkkaHttpServer = false
  override def expectedServerTag = None
}
object AkkaHttpServerIntegrationSpecificationSpec extends ServerIntegrationSpecificationSpec with AkkaHttpIntegrationSpecification {
  override def isAkkaHttpServer = true
  override def expectedServerTag = Some("akka-http")
}

/**
 * Tests that the ServerIntegrationSpecification, a helper for testing with different
 * server backends, works properly.
 */
trait ServerIntegrationSpecificationSpec extends PlaySpecification
    with WsTestClient with ServerIntegrationSpecification {

  def isAkkaHttpServer: Boolean

  def expectedServerTag: Option[String]

  "ServerIntegrationSpecification" should {

    val httpServerTagRoutes: PartialFunction[(String, String), Handler] = {
      case ("GET", "/httpServerTag") => Action { implicit request =>
        val httpServer = request.tags.get("HTTP_SERVER")
        Ok(httpServer.toString)
      }
    }

    "run the right HTTP server when using TestServer constructor" in {
      running(TestServer(testServerPort, FakeApplication(withRoutes = httpServerTagRoutes))) {
        val plainRequest = wsUrl("/httpServerTag")(testServerPort)
        val responseFuture = plainRequest.get()
        val response = await(responseFuture)
        response.status must_== 200
        response.body must_== expectedServerTag.toString
      }
    }

    "run the right server when using WithServer trait" in new WithServer(
      app = FakeApplication(withRoutes = httpServerTagRoutes)) {
      val response = await(wsUrl("/httpServerTag").get())
      response.status must equalTo(OK)
      response.body must_== expectedServerTag.toString
    }

    "support pending Akka HTTP tests" in {
      isAkkaHttpServer must beFalse
    }.pendingUntilAkkaHttpFixed

  }
}
