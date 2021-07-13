/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.concurrent;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.Executor;

/** @deprecated Deprecated as of 2.9.0. Renamed to {@link ClassLoaderExecutionContext}. */
@Singleton
public class HttpExecutionContext {

  private final Executor delegate;

  /** @deprecated Deprecated as of 2.9.0. Use to {@link ClassLoaderExecutionContext} instead. */
  @Deprecated
  @Inject
  public HttpExecutionContext(Executor delegate) {
    this.delegate = delegate;
  }

  /**
   * @deprecated Deprecated as of 2.9.0. Use to {@link ClassLoaderExecutionContext#current()}}
   *     instead.
   */
  @Deprecated
  public Executor current() {
    return ClassLoaderExecution.fromThread(delegate);
  }
}
