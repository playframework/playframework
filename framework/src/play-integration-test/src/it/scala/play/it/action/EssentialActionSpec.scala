/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.action

import org.specs2.matcher.MatchResult
import play.api.Environment
import play.api.mvc.AnyContent
import play.api.mvc.AnyContentAsEmpty
import play.api.mvc.BodyParsers
import play.api.mvc.Results._
import play.api.mvc.{ DefaultActionBuilder, EssentialAction }
import play.api.test.{ FakeRequest, PlaySpecification }

import scala.concurrent.Promise

class EssentialActionSpec extends PlaySpecification {

  "an EssentialAction" should {

    "use the classloader of the running application" in {

      // start fake application with its own classloader
      val applicationClassLoader = new ClassLoader() {}

      running(_.in(Environment.simple().copy(classLoader = applicationClassLoader))) { app =>
        import app.materializer

        val Action = app.injector.instanceOf[DefaultActionBuilder]

        def checkAction(actionCons: (ClassLoader => Unit) => EssentialAction): MatchResult[_] = {
          val actionClassLoader = Promise[ClassLoader]()
          val action = actionCons(cl => actionClassLoader.success(cl))
          call(action, FakeRequest())
          await(actionClassLoader.future) must be equalTo applicationClassLoader
        }

        // make sure running thread has applicationClassLoader set
        Thread.currentThread.setContextClassLoader(applicationClassLoader)

        // test with simple sync action
        checkAction { reportCL =>
          Action {
            reportCL(Thread.currentThread.getContextClassLoader)
            Ok("")
          }
        }

        // test with async action
        checkAction { reportCL =>
          Action(BodyParsers.utils.maxLength(100, BodyParsers.utils.ignore(AnyContentAsEmpty: AnyContent))) { _ =>
            reportCL(Thread.currentThread.getContextClassLoader)
            Ok("")
          }
        }
      }
    }
  }

}
