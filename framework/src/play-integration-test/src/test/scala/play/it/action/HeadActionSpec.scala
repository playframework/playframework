/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.action

import akka.stream.scaladsl.Source
import io.netty.handler.codec.http.HttpHeaders
import org.specs2.mutable.Specification
import play.api.Play
import play.api.http.HeaderNames._
import play.api.http.Status._
import play.api.libs.ws.{ WSClient, WSResponse }
import play.api.mvc._
import play.api.routing.Router.Routes
import play.api.routing.Router.Tags._
import play.api.routing.sird._
import play.api.test._
import play.core.server.Server
import play.it._
import play.it.tools.HttpBinApplication._
import scala.concurrent.ExecutionContext.Implicits.global
import org.asynchttpclient.netty.NettyResponse

object NettyHeadActionSpec extends HeadActionSpec with NettyIntegrationSpecification
object AkkaHttpHeadActionSpec extends HeadActionSpec with AkkaHttpIntegrationSpecification

trait HeadActionSpec extends Specification with FutureAwaits with DefaultAwaitTimeout with ServerIntegrationSpecification {

  private def route(verb: String, path: String)(handler: EssentialAction): PartialFunction[(String, String), Handler] = {
    case (v, p) if v == verb && p == path => handler
  }
  sequential

  "HEAD requests" should {

    val chunkedResponse: Routes = {
      case GET(p"/chunked") =>
        Action { request =>
          Results.Ok.chunked(Source(List("a", "b", "c")))
        }
    }

    val routes =
      get // GET /get
        .orElse(patch) // PATCH /patch
        .orElse(post) // POST /post
        .orElse(put) // PUT /put
        .orElse(delete) // DELETE /delete
        .orElse(stream) // GET /stream/0
        .orElse(chunkedResponse) // GET /chunked

    def withServer[T](block: WSClient => T): T = {
      // Routes from HttpBinApplication
      Server.withRouter()(routes) { implicit port =>
        implicit val mat = Play.current.materializer
        WsTestClient.withClient(block)
      }
    }

    def serverWithAction[T](action: EssentialAction)(block: WSClient => T): T = {
      Server.withRouter() {
        case _ => action
      } { implicit port =>
        implicit val mat = Play.current.materializer
        WsTestClient.withClient(block)
      }
    }

    "return 200 in response to a URL with a GET handler" in withServer { client =>
      val result = await(client.url("/get").head())

      result.status must_== OK
    }

    "return an empty body" in withServer { client =>
      val result = await(client.url("/get").head())

      result.body.length must_== 0
    }

    "match the headers of an equivalent GET" in withServer { client =>
      val collectedFutures = for {
        headResponse <- client.url("/get").head()
        getResponse <- client.url("/get").get()
      } yield List(headResponse, getResponse)

      val responses = await(collectedFutures)

      val headHeaders = responses(0).underlying[NettyResponse].getHeaders
      val getHeaders: HttpHeaders = responses(1).underlying[NettyResponse].getHeaders

      // Exclude `Date` header because it can vary between requests
      import scala.collection.JavaConverters._
      val firstHeaders = headHeaders.remove(DATE)
      val secondHeaders = getHeaders.remove(DATE)

      // HTTPHeaders doesn't seem to be anything as simple as an equals method, so let's compare A !< B && B >! A
      val notInFirst = secondHeaders.asScala.collectFirst {
        case entry if !firstHeaders.contains(entry.getKey, entry.getValue, true) =>
          entry
      }
      val notInSecond = firstHeaders.asScala.collectFirst {
        case entry if !secondHeaders.contains(entry.getKey, entry.getValue, true) =>
          entry
      }
      notInFirst must beEmpty
      notInSecond must beEmpty
    }

    "return 404 in response to a URL without an associated GET handler" in withServer { client =>
      val collectedFutures = for {
        putRoute <- client.url("/put").head()
        patchRoute <- client.url("/patch").head()
        postRoute <- client.url("/post").head()
        deleteRoute <- client.url("/delete").head()
      } yield List(putRoute, patchRoute, postRoute, deleteRoute)

      val responseList = await(collectedFutures)

      foreach(responseList)((_: WSResponse).status must_== NOT_FOUND)
    }

    "tag request with DefaultHttpRequestHandler" in serverWithAction(new RequestTaggingHandler with EssentialAction {
      def tagRequest(request: RequestHeader) = request.copy(tags = Map(RouteComments -> "some comment"))
      def apply(rh: RequestHeader) = Action {
        Results.Ok.withHeaders(rh.tags.get(RouteComments).map(RouteComments -> _).toSeq: _*)
      }(rh)
    }) { client =>
      val result = await(client.url("/get").head())
      result.status must_== OK
      result.header(RouteComments) must beSome("some comment")
    }

    "omit Content-Length for chunked responses" in withServer { client =>
      val response = await(client.url("/chunked").head())

      response.body must_== ""
      response.header(CONTENT_LENGTH) must beNone
    }

  }

}
