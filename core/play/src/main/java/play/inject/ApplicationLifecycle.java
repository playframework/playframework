/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.inject;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;

/**
 * Application lifecycle register.
 *
 * <p>This is used to hook into Play lifecycle events, specifically, when Play is stopped.
 *
 * <p>Stop hooks are executed when the application is shutdown, in reverse from when they were
 * registered.
 *
 * <p>To use this, declare a dependency on ApplicationLifecycle, and then register the stop hook
 * when the component is started.
 */
public interface ApplicationLifecycle {

  /**
   * Add a stop hook to be called when the application stops.
   *
   * <p>The stop hook should redeem the returned future when it is finished shutting down. It is
   * acceptable to stop immediately and return a successful future.
   *
   * @param hook the stop hook.
   */
  void addStopHook(Callable<? extends CompletionStage<?>> hook);

  /**
   * @return The Scala version for this Application Lifecycle.
   */
  play.api.inject.ApplicationLifecycle asScala();
}
