/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package gettingStarted

import org.specs2.mutable.Specification
import play.api._

package consoleapp {

  class MyPlayConsole {
    def createApplication() = {
      //#consoleapp
      import play.api._
      val env     = Environment(new java.io.File("."), this.getClass.getClassLoader, Mode.Dev)
      val context = ApplicationLoader.createContext(env)
      val loader  = ApplicationLoader(context)
      val app     = loader.load(context)
      Play.start(app)
      //#consoleapp
      app
    }
  }
}

class PlayConsole extends Specification {
  "Play console" should {
    "support creating an instance of the Play application" in {
      val app = new gettingStarted.consoleapp.MyPlayConsole().createApplication()
      app must beAnInstanceOf[Application]
    }
  }
}
