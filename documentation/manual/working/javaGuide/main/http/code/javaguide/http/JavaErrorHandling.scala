/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.http

import play.api.mvc.Action
import play.api.test._

import scala.reflect.ClassTag

object JavaErrorHandling extends PlaySpecification with WsTestClient {

  def fakeApp[A](implicit ct: ClassTag[A]) = {
    FakeApplication(
      additionalConfiguration = Map("play.http.errorHandler" -> ct.runtimeClass.getName),
      withRoutes = {
        case (_, "/error") => Action(_ => throw new RuntimeException("foo"))
      }
    )
  }

  "java error handling" should {
    "allow providing a custom error handler" in new WithServer(fakeApp[root.ErrorHandler]) {
      await(wsUrl("/error").get()).body must startWith("A server error occurred: ")
    }

    "allow providing a custom error handler" in new WithServer(fakeApp[`def`.ErrorHandler]) {
      await(wsUrl("/error").get()).body must not startWith("A server error occurred: ")
    }
  }

}
