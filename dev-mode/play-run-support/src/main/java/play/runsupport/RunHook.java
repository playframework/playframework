/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.runsupport;

/**
 * The represents an object which "hooks into" play run, and is used to apply startup/cleanup
 * actions around a play application.
 */
public interface RunHook {

  /**
   * Called before the play application is started, but after all "before run" tasks have been
   * completed.
   */
  default void beforeStarted() {}

  /** Called after the play application has been started. */
  default void afterStarted() {}

  /** Called after the play process has been stopped. */
  default void afterStopped() {}

  /**
   * Called if there was any exception thrown during play run. Useful to implement to clean up any
   * open resources for this hook.
   */
  default void onError() {}
}
