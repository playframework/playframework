/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.action

import play.api.Environment
import play.api.mvc.Results._
import play.api.mvc.{ Action, EssentialAction }
import play.api.test.{ FakeRequest, PlaySpecification }

import scala.concurrent.Promise

object EssentialActionSpec extends PlaySpecification {

  "an EssentialAction" should {

    "use the classloader of the running application" in {

      val actionClassLoader = Promise[ClassLoader]()
      val action: EssentialAction = Action {
        actionClassLoader.success(Thread.currentThread.getContextClassLoader)
        Ok("")
      }

      // start fake application with its own classloader
      val applicationClassLoader = new ClassLoader() {}

      running(_.in(Environment.simple().copy(classLoader = applicationClassLoader))) { app =>
        import app.materializer
        // run the test with the classloader of the current thread
        Thread.currentThread.getContextClassLoader must not be applicationClassLoader
        call(action, FakeRequest())
        await(actionClassLoader.future) must be equalTo applicationClassLoader
      }
    }
  }

}
