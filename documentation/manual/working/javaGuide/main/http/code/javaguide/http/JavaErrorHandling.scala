/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.http

import scala.reflect.ClassTag

import javaguide.application.`def`.ErrorHandler
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.DefaultActionBuilder
import play.api.test._

class JavaErrorHandling extends PlaySpecification with WsTestClient {
  def fakeApp[A](implicit ct: ClassTag[A]) = {
    GuiceApplicationBuilder()
      .configure("play.http.errorHandler" -> ct.runtimeClass.getName)
      .appRoutes { app =>
        val Action = app.injector.instanceOf[DefaultActionBuilder]
        ({
          case (_, "/error") => Action(_ => throw new RuntimeException("foo"))
        })
      }
      .build()
  }

  "java error handling" should {
    "allow providing a custom error handler" in new WithServer(fakeApp[javaguide.application.root.ErrorHandler]) {
      override def running() = {
        import play.api.libs.ws.DefaultBodyReadables.readableAsString
        await(wsUrl("/error").get()).body must startWith("A server error occurred: ")
      }
    }

    "allow providing a custom error handler" in new WithServer(fakeApp[ErrorHandler]) {
      override def running() = {
        import play.api.libs.ws.DefaultBodyReadables.readableAsString
        (await(wsUrl("/error").get()).body must not).startWith("A server error occurred: ")
      }
    }
  }
}
