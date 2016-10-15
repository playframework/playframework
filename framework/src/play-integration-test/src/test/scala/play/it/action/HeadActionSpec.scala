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
import play.api.libs.typedmap.TypedKey

class NettyHeadActionSpec extends HeadActionSpec with NettyIntegrationSpecification
class AkkaHttpHeadActionSpec extends HeadActionSpec with AkkaHttpIntegrationSpecification

trait HeadActionSpec extends Specification with FutureAwaits with DefaultAwaitTimeout with ServerIntegrationSpecification {

  private def route(verb: String, path: String)(handler: EssentialAction): PartialFunction[(String, String), Handler] = {
    case (v, p) if v == verb && p == path => handler
  }
  sequential

  "HEAD requests" should {

    def chunkedResponse(implicit Action: DefaultActionBuilder): Routes = {
      case GET(p"/chunked") =>
        Action { request =>
          Results.Ok.chunked(Source(List("a", "b", "c")))
        }
    }

    def routes(implicit Action: DefaultActionBuilder) =
      get // GET /get
        .orElse(patch) // PATCH /patch
        .orElse(post) // POST /post
        .orElse(put) // PUT /put
        .orElse(delete) // DELETE /delete
        .orElse(stream) // GET /stream/0
        .orElse(chunkedResponse) // GET /chunked

    def withServer[T](block: WSClient => T): T = {
      // Routes from HttpBinApplication
      Server.withRouterFromComponents()(components => routes(components.defaultActionBuilder)) { implicit port =>
        WsTestClient.withClient(block)
      }
    }

    def serverWithHandler[T](handler: Handler)(block: WSClient => T): T = {
      Server.withRouter() {
        case _ => handler
      } { implicit port =>
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

    val CustomAttr = TypedKey[String]("CustomAttr")
    def addCustomTagAndAttr(r: RequestHeader): RequestHeader = {
      r.copy(tags = Map("CustomTag" -> "x")).withAttr(CustomAttr, "y")
    }
    val tagAndAttrAction = ActionBuilder.ignoringBody { rh: RequestHeader =>
      val tagComment = rh.tags.get("CustomTag")
      val attrComment = rh.getAttr(CustomAttr)
      val headers = Array.empty[(String, String)] ++
        rh.tags.get("CustomTag").map("CustomTag" -> _) ++
        rh.getAttr(CustomAttr).map("CustomAttr" -> _)
      Results.Ok.withHeaders(headers: _*)
    }

    "tag request with DefaultHttpRequestHandler" in serverWithHandler(new RequestTaggingHandler with EssentialAction {
      def tagRequest(request: RequestHeader) = addCustomTagAndAttr(request)
      def apply(rh: RequestHeader) = tagAndAttrAction(rh)
    }) { client =>
      val result = await(client.url("/get").head())
      result.status must_== OK
      result.header("CustomTag") must beSome("x")
      result.header("CustomAttr") must beSome("y")
    }

    "modify request with DefaultHttpRequestHandler" in serverWithHandler(
      Handler.Stage.modifyRequest(
        (rh: RequestHeader) => addCustomTagAndAttr(rh),
        tagAndAttrAction
      )
    ) { client =>
        val result = await(client.url("/get").head())
        result.status must_== OK
        result.header("CustomTag") must beSome("x")
        result.header("CustomAttr") must beSome("y")
      }

    "omit Content-Length for chunked responses" in withServer { client =>
      val response = await(client.url("/chunked").head())

      response.body must_== ""
      response.header(CONTENT_LENGTH) must beNone
    }

  }

}
