/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.runsupport

import java.net.InetSocketAddress
import scala.collection.mutable.LinkedHashMap
import scala.util.control.NonFatal

/**
 * The represents an object which "hooks into" play run, and is used to
 * apply startup/cleanup actions around a play application.
 */
trait RunHook {

  /**
   * Called before the play application is started,
   * but after all "before run" tasks have been completed.
   */
  def beforeStarted(): Unit = ()

  /**
   * Called after the play application has been started.
   * @param addr The address/socket that play is listening to
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
  "Multiple exceptions thrown during RunHook run: " +
    throwables.map(t => t + "\n" + t.getStackTrace.take(10).++("...").mkString("\n")).mkString("\n\n")
)

object RunHook {

  // A bit of a magic hack to clean up the PlayRun file
  implicit class RunHooksRunner(val hooks: Seq[RunHook]) extends AnyVal {
    /**
     * Runs all the hooks in the sequence of hooks.
     * Reports last failure if any have failure.
     */
    def run(f: RunHook => Unit, suppressFailure: Boolean = false): Unit = try {

      val failures: LinkedHashMap[RunHook, Throwable] = LinkedHashMap.empty

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
