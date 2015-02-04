/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */

package play.it.action

import play.api.test._
import scala.concurrent.ExecutionContext.Implicits.global
import play.it._
import play.it.tools.HttpBinApplication._
import play.api.libs.ws.WSResponse
import com.ning.http.client.providers.netty.response.NettyResponse
import play.api.mvc._
import play.api.http.HeaderNames
import play.api.libs.iteratee.Enumerator
import java.util.concurrent.atomic.AtomicBoolean
import play.api.test.TestServer
import play.api.test.FakeApplication

object NettyHeadActionSpec extends HeadActionSpec with NettyIntegrationSpecification
object AkkaHttpHeadActionSpec extends HeadActionSpec with AkkaHttpIntegrationSpecification

trait HeadActionSpec extends PlaySpecification
    with WsTestClient with Results with HeaderNames with ServerIntegrationSpecification {

  def route(verb: String, path: String)(handler: EssentialAction): PartialFunction[(String, String), Handler] = {
    case (v, p) if v == verb && p == path => handler
  }

  "HEAD requests" should {
    implicit val port: Port = testServerPort

    val manualContentSize = route("GET", "/manualContentSize") {
      Action { request =>
        Ok("The Itsy Bitsy Spider Went Up the Water Spout").withHeaders(CONTENT_LENGTH -> "5")
      }
    }

    val chunkedResponse = route("GET", "/chunked") {
      Action { request =>
        Ok.chunked(Enumerator("a", "b", "c"))
      }
    }

    def withServer[T](block: => T): T = {
      // Routes from HttpBinApplication
      val routes =
        get // GET /get
          .orElse(patch) // PATCH /patch
          .orElse(post) // POST /post
          .orElse(put) // PUT /put
          .orElse(delete) // DELETE /delete
          .orElse(stream) // GET /stream/0
          .orElse(manualContentSize) // GET /manualContentSize
          .orElse(chunkedResponse) // GET /chunked
      running(TestServer(port, FakeApplication(withRoutes = routes)))(block)
    }

    def serverWithAction[T](action: EssentialAction)(block: => T): T = {
      running(TestServer(port, FakeApplication(
        withRoutes = {
          case _ => action
        })))(block)
    }

    "return 200 in response to a URL with a GET handler" in withServer {
      val result = await(wsUrl("/get").head())

      result.status must_== OK
    }

    "return an empty body" in withServer {
      val result = await(wsUrl("/get").head())

      result.body.length must_== 0
    }

    "match the headers of an equivalent GET" in withServer {
      val collectedFutures = for {
        headResponse <- wsUrl("/get").head()
        getResponse <- wsUrl("/get").get()
      } yield List(headResponse, getResponse)

      val responses = await(collectedFutures)

      val headHeaders = responses(0).underlying[NettyResponse].getHeaders
      val getHeaders = responses(1).underlying[NettyResponse].getHeaders

      // Exclude `Date` header because it can vary between requests
      (headHeaders.delete(DATE)) must_== (getHeaders.delete(DATE))
    }

    "return 404 in response to a URL without an associated GET handler" in withServer {
      val collectedFutures = for {
        putRoute <- wsUrl("/put").head()
        patchRoute <- wsUrl("/patch").head()
        postRoute <- wsUrl("/post").head()
        deleteRoute <- wsUrl("/delete").head()
      } yield List(putRoute, patchRoute, postRoute, deleteRoute)

      val responseList = await(collectedFutures)

      foreach(responseList)((_: WSResponse).status must_== NOT_FOUND)
    }

    "clean up any onDoneEnumerating callbacks" in {
      val wasCalled = new AtomicBoolean()

      val action = Action {
        Ok.chunked(Enumerator("a", "b", "c").onDoneEnumerating(wasCalled.set(true)))
      }
      serverWithAction(action) {
        await(wsUrl("/get").head())
        wasCalled.get() must be_==(true).eventually
      }
    }

    "respect deliberately set Content-Length headers" in withServer {
      val result = await(wsUrl("/manualContentSize").head())

      result.header(CONTENT_LENGTH) must beSome("5")
    }

    "omit Content-Length for chunked responses" in withServer {
      val response = await(wsUrl("/chunked").head())

      response.body must_== ""
      response.header(CONTENT_LENGTH) must beNone
    }

  }
}
