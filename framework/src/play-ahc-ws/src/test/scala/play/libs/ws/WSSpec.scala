/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.ws

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Results._
import play.api.mvc.{ Action, Result }
import play.api.test._

class WSSpec extends PlaySpecification {

  sequential

  "WS.url().post(InputStream)" should {

    val uploadApp = GuiceApplicationBuilder().routes {
      case ("POST", "/") =>
        Action { request =>
          request.body.asRaw.fold[Result](BadRequest) { raw =>
            val size = raw.size
            Ok(s"size=$size")
          }
        }
    }.build()

    "uploads the stream" in new WithServer(app = uploadApp, port = 3333) {
      val wsClient = app.injector.instanceOf(classOf[WSClient])

      val input = this.getClass.getClassLoader.getResourceAsStream("play/libs/ws/play_full_color.png")
      val rep = wsClient.url("http://localhost:3333").post(input).toCompletableFuture.get()

      rep.getStatus must ===(200)
      rep.getBody must ===("size=20039")
    }
  }

}
