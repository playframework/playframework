package play

import java.net.InetSocketAddress

/**
 * The represents an object which "hooks into" play run, and is used to
 *  apply startup/cleanup actions around a play application.
 */
trait PlayRunHook {
  /** Called before the play application is started, but after all "before run" tasks have been completed. */
  def beforeStarted(): Unit = ()
  /**
   * Called after the play applciation has been started.
   *  @param addr  The address/socket that play is listening to.
   */
  def afterStarted(addr: InetSocketAddress): Unit = ()
  /**
   * Called after the play process has been stopped.
   */
  def afterStopped(): Unit = ()
}
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
      // TODO - Should we ignore failure?  Probably not... but just sending first fail may be bad too.
      // TODO - Should probably have a cleanup method on hooks in case of failure...
      var lastFailure: Option[Throwable] = None
      hooks foreach { hook =>
        try {
          f(hook)
        } catch {
          case e: Throwable =>
            // Just save the last failure for now...
            lastFailure = Some(e)
        }
      }
      // Throw failure if it occurred....
      if (!suppressFailure) lastFailure foreach (throw _)
    } catch {
      case e: Throwable if suppressFailure =>
      // Ignoring failure in running hooks... (CCE thrown here)
    }
  }
}
