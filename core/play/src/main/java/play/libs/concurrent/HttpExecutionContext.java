/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.concurrent;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.Executor;

/**
 * Execution context for managing the ClassLoader scope.
 *
 * <p>This is essentially a factory for getting an executor for the current ClassLoader. Tasks
 * executed by that executor will have the same ClassLoader in scope.
 *
 * <p>For example, it may be used in combination with <code>CompletionStage.thenApplyAsync</code>,
 * to ensure the callbacks executed when the completion stage is redeemed have the correct
 * ClassLoader:
 *
 * <pre>
 *     CompletionStage&lt;WSResponse&gt; response = ws.url(...).get();
 *     CompletionStage&lt;Result&gt; result = response.thenApplyAsync(response -&gt; {
 *         return ok("Got response body " + ws.body() + " while executing request " + request().uri());
 *     }, httpExecutionContext.current());
 * </pre>
 *
 * Note, this is not a Scala execution context, and is not intended to be used where Scala execution
 * contexts are required.
 */
@Singleton
public class HttpExecutionContext {

  private final Executor delegate;

  @Inject
  public HttpExecutionContext(Executor delegate) {
    this.delegate = delegate;
  }

  /**
   * Get the current executor associated with the current ClassLoader.
   *
   * <p>Note that the returned executor is only valid for the current ClassLoader. It should be used
   * in a transient fashion, long lived references to it should not be kept.
   *
   * @return An executor that will execute its tasks with the current ClassLoader.
   */
  public Executor current() {
    return HttpExecution.fromThread(delegate);
  }
}
