/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.ws

import akka.stream.Materializer
import play.api.mvc.Results._
import play.api.mvc._
import play.api.test._
import play.core.server.Server
import play.libs.ws.ahc.AhcWSClient
import play.shaded.ahc.org.asynchttpclient.AsyncHttpClient

class WSSpec extends PlaySpecification with WsTestClient {

  sequential

  "WSClient.url().post(InputStream)" should {

    "uploads the stream" in {

      var mat: Materializer = NoMaterializer

      Server.withRouterFromComponents() { components =>

        mat = components.materializer

        import components.{ defaultActionBuilder => Action }
        import play.api.routing.sird.{ POST => SirdPost, _ }
        {
          case SirdPost(p"/") => Action { req: Request[AnyContent] =>
            req.body.asRaw.fold[Result](BadRequest) { raw =>
              val size = raw.size
              Ok(s"size=$size")
            }
          }
        }
      } { implicit port =>
        withClient { ws =>
          val javaWs = new AhcWSClient(ws.underlying[AsyncHttpClient], mat)
          val input = this.getClass.getClassLoader.getResourceAsStream("play/libs/ws/play_full_color.png")
          val rep = javaWs.url(s"http://localhost:$port/").post(input).toCompletableFuture.get()

          rep.getStatus must ===(200)
          rep.getBody must ===("size=20039")
        }
      }

    }
  }

}
