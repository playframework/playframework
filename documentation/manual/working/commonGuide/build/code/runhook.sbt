//
// Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
//

// You can't define objects at the root level of an SBT file, so we do it inside a def
def Grunt(base: File) = {
  //#grunt-before-started
  import play.sbt.PlayRunHook
  import sbt._

  object Grunt {
    def apply(base: File): PlayRunHook = {

      object GruntProcess extends PlayRunHook {

        override def beforeStarted(): Unit = {
          Process("grunt dist", base).run
        }
      }

      GruntProcess
    }
  }
  //#grunt-before-started
  Grunt(base)
}

//#grunt-build-sbt
PlayKeys.playRunHooks += Grunt(baseDirectory.value)
//#grunt-build-sbt

def Grunt2(base: File) = {
  //#grunt-watch
  import play.sbt.PlayRunHook
  import sbt._
  import java.net.InetSocketAddress

  object Grunt {
    def apply(base: File): PlayRunHook = {

      object GruntProcess extends PlayRunHook {

        var watchProcess: Option[Process] = None

        override def beforeStarted(): Unit = {
          Process("grunt dist", base).run
        }

        override def afterStarted(addr: InetSocketAddress): Unit = {
          watchProcess = Some(Process("grunt watch", base).run)
        }

        override def afterStopped(): Unit = {
          watchProcess.map(p => p.destroy())
          watchProcess = None
        }
      }

      GruntProcess
    }
  }
  //#grunt-watch
  Grunt(base)
}

