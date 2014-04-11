/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play

import scala.util.control.NonFatal
import scala.collection.mutable.LinkedHashMap
import java.net.InetSocketAddress

/**
 * The represents an object which "hooks into" play run, and is used to
 *  apply startup/cleanup actions around a play application.
 */
trait PlayRunHook {
  /** Called before the play application is started, but after all "before run" tasks have been completed. */
  def beforeStarted(): Unit = ()
  /**
   * Called after the play application has been started.
   *  @param addr  The address/socket that play is listening to.
   */
  def afterStarted(addr: InetSocketAddress): Unit = ()
  /**
   * Called after the play process has been stopped.
   */
  def afterStopped(): Unit = ()

  /**
   * Called if there was any exception thrown during play run.
   * Useful to implement to clean up any open resources for this hook.
   */
  def onError(): Unit = ()
}

case class RunHookCompositeThrowable(val throwables: Set[Throwable]) extends Exception(
  "Multiple exceptions thrown during PlayRunHook run: " +
    throwables.map(t => t + "\n" + t.getStackTrace.take(10).++("...").mkString("\n")).mkString("\n\n")
)

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

  // A bit of a magic hack to clean up the PlayRun file
  implicit class RunHooksRunner(val hooks: Seq[PlayRunHook]) extends AnyVal {
    /** Runs all the hooks in the sequence of hooks.  reports last failure if any have failure. */
    def run(f: PlayRunHook => Unit, suppressFailure: Boolean = false): Unit = try {

      val failures: LinkedHashMap[PlayRunHook, Throwable] = LinkedHashMap.empty

      hooks foreach { hook =>
        try {
          f(hook)
        } catch {
          case NonFatal(e) =>
            failures += hook -> e
        }
      }

      // Throw failure if it occurred....
      if (!suppressFailure && failures.nonEmpty) {
        if (failures.size == 1) {
          throw failures.values.head
        } else {
          throw RunHookCompositeThrowable(failures.values.toSet)
        }
      }
    } catch {
      case NonFatal(e) if suppressFailure =>
      // Ignoring failure in running hooks... (CCE thrown here)
    }
  }
}
