/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.action

import akka.stream.ActorFlowMaterializer
import play.api.mvc.{ Action, EssentialAction }
import play.api.mvc.Results._
import play.api.test.{ FakeApplication, PlaySpecification, FakeRequest }
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
      val fakeApplication = FakeApplication(classloader = applicationClassLoader)
      implicit val mat = ActorFlowMaterializer()(fakeApplication.actorSystem)

      running(fakeApplication) {
        // run the test with the classloader of the current thread
        Thread.currentThread.getContextClassLoader must not be applicationClassLoader
        call(action, FakeRequest())
        await(actionClassLoader.future) must be equalTo applicationClassLoader
      }
    }
  }

}
