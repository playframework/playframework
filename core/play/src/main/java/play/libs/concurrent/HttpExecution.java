/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.concurrent;

import java.util.concurrent.Executor;
import play.core.j.ClassLoaderExecutionContext;
import scala.concurrent.ExecutionContext;
import scala.concurrent.ExecutionContextExecutor;

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
