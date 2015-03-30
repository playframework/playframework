/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.sbt

import java.net.InetSocketAddress

/**
 * The represents an object which "hooks into" play run, and is used to
 * apply startup/cleanup actions around a play application.
 */
trait PlayRunHook extends play.runsupport.RunHook

object PlayRunHook {

  def makeRunHookFromOnStarted(f: (java.net.InetSocketAddress) => Unit): PlayRunHook = {
    // We create an object for a named class...
    object OnStartedPlayRunHook extends PlayRunHook {
      override def afterStarted(addr: InetSocketAddress): Unit = f(addr)
    }
    OnStartedPlayRunHook
  }

  def makeRunHookFromOnStopped(f: () => Unit): PlayRunHook = {
    object OnStoppedPlayRunHook extends PlayRunHook {
      override def afterStopped(): Unit = f()
    }
    OnStoppedPlayRunHook
  }

}
