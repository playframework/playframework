/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.concurrent;

import play.core.j.ClassLoaderExecutionContext;
import scala.concurrent.ExecutionContext;
import scala.concurrent.ExecutionContextExecutor;

import java.util.concurrent.Executor;

/** @deprecated Deprecated as of 2.9.0. Renamed to {@link ClassLoaderExecution}. */
@Deprecated
public class HttpExecution {

  /**
   * @deprecated Deprecated as of 2.9.0. Use to {@link
   *     ClassLoaderExecution#fromThread(ExecutionContext)} instead.
   */
  @Deprecated
  public static ExecutionContextExecutor fromThread(ExecutionContext delegate) {
    return ClassLoaderExecutionContext.fromThread(delegate);
  }

  /**
   * @deprecated Deprecated as of 2.9.0. Use to {@link
   *     ClassLoaderExecution#fromThread(ExecutionContextExecutor)} instead.
   */
  @Deprecated
  public static ExecutionContextExecutor fromThread(ExecutionContextExecutor delegate) {
    return ClassLoaderExecutionContext.fromThread(delegate);
  }

  /**
   * @deprecated Deprecated as of 2.9.0. Use to {@link ClassLoaderExecution#fromThread(Executor)}
   *     instead.
   */
  @Deprecated
  public static ExecutionContextExecutor fromThread(Executor delegate) {
    return ClassLoaderExecutionContext.fromThread(delegate);
  }
}
