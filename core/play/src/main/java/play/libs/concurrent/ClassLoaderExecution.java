/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.concurrent;

import java.util.concurrent.Executor;
import play.core.j.ClassLoaderExecutionContext;
import scala.concurrent.ExecutionContext;
import scala.concurrent.ExecutionContextExecutor;

/**
 * ExecutionContexts that preserve the current thread's context ClassLoader by passing it through
 * {@link play.libs.concurrent.ClassLoaderExecutionContext}.
 */
public class ClassLoaderExecution {

  /**
   * An ExecutionContext that executes work on the given ExecutionContext. The current thread's
   * context ClassLoader is captured when this method is called and preserved for all executed
   * tasks.
   *
   * @param delegate the delegate execution context.
   * @return the execution context wrapped in an {@link
   *     play.libs.concurrent.ClassLoaderExecutionContext}.
   */
  public static ExecutionContextExecutor fromThread(ExecutionContext delegate) {
    return ClassLoaderExecutionContext.fromThread(delegate);
  }

  /**
   * An ExecutionContext that executes work on the given ExecutionContext. The current thread's
   * context ClassLoader is captured when this method is called and preserved for all executed
   * tasks.
   *
   * @param delegate the delegate execution context.
   * @return the execution context wrapped in an {@link
   *     play.libs.concurrent.ClassLoaderExecutionContext}.
   */
  public static ExecutionContextExecutor fromThread(ExecutionContextExecutor delegate) {
    return ClassLoaderExecutionContext.fromThread(delegate);
  }

  /**
   * An ExecutionContext that executes work on the given ExecutionContext. The current thread's
   * context ClassLoader is captured when this method is called and preserved for all executed
   * tasks.
   *
   * @param delegate the delegate execution context.
   * @return the execution context wrapped in an {@link
   *     play.libs.concurrent.ClassLoaderExecutionContext}.
   */
  public static ExecutionContextExecutor fromThread(Executor delegate) {
    return ClassLoaderExecutionContext.fromThread(delegate);
  }
}
