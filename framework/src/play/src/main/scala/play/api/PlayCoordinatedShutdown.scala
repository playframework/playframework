package play.api

/**
 * Extends Akka's CoordinatedShutdown with Play specific phases.
 */
object PlayCoordinatedShutdown {

  /**
   * The phase Play will use to run all stop hooks registered
   * in the ApplicationLifecycle. Akka's Coordinated Shutdown
   * delegates the execution to Play so stop hooks are granted to
   * run in reverse order they where registered.
   * By default Akka's CoordinatedShutdown runs all tasks on the
   * same phase in parallel so Akka doesn't grant ordering within
   * a phase.
   */
  val PhaseApplicationStopHooks = "application-stop-hooks"

  /**
   * Play Server needs to run some cleanup once the Application is
   * shutdown. This phase is the final step to completely close the
   * play server.
   */
  val PhasePlayServerStop = "play-server-stop"

  /**
   * This phase allows registering stop hooks that will run once
   * the Play server is completely stopped.
   */
  val PhaseAfterPlayServerStop = "after-play-server-stop"
}
