/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.ws

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{ Result, Action }
import play.api.mvc.Results._
import play.api.test._
import play.libs.ws.ahc.{ AhcWSRequest, AhcWSClient }
import play.test.WithApplication

object WSSpec extends PlaySpecification {

  sequential

  "WS.url().post(InputStream)" should {

    val uploadApp = FakeApplication(withRoutes = {
      case ("POST", "/") =>
        Action { request =>
          request.body.asRaw.fold[Result](BadRequest) { raw =>
            val size = raw.size
            Ok(s"size=$size")
          }
        }
    })

    "uploads the stream" in new WithServer(app = uploadApp, port = 3333) {
      val wsClient = app.injector.instanceOf(classOf[WSClient])

      val input = this.getClass.getClassLoader.getResourceAsStream("play/libs/ws/play_full_color.png")
      val rep = wsClient.url("http://localhost:3333").post(input).toCompletableFuture.get()

      rep.getStatus must ===(200)
      rep.getBody must ===("size=20039")
    }
  }

  "withRequestFilter" should {

    class CallbackRequestFilter(callList: scala.collection.mutable.Buffer[Int], value: Int) extends WSRequestFilter {
      override def apply(executor: WSRequestExecutor): WSRequestExecutor = {
        callList.append(value)
        executor
      }
    }

    "work with one request filter" in new WithServer() {
      val client = app.injector.instanceOf(classOf[play.libs.ws.WSClient])
      val callList = scala.collection.mutable.ArrayBuffer[Int]()
      val responseFuture = client.url(s"http://example.com:$testServerPort")
        .withRequestFilter(new CallbackRequestFilter(callList, 1))
        .get()
      callList must contain(1)
    }

    "work with three request filter" in new WithServer() {
      val client = app.injector.instanceOf(classOf[play.libs.ws.WSClient])
      val callList = scala.collection.mutable.ArrayBuffer[Int]()
      val responseFuture = client.url(s"http://localhost:${testServerPort}")
        .withRequestFilter(new CallbackRequestFilter(callList, 1))
        .withRequestFilter(new CallbackRequestFilter(callList, 2))
        .withRequestFilter(new CallbackRequestFilter(callList, 3))
        .get()
      callList must containTheSameElementsAs(Seq(1, 2, 3))
    }
  }

}
