/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.concurrent;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.Executor;

/**
 * Execution context for managing Play Java HTTP thread local state.
 *
 * This is essentially a factory for getting an executor for the current HTTP context.  Tasks executed by that executor
 * will have the HTTP context setup in them.
 *
 * For example, it may be used in combination with <code>CompletionStage.thenApplyAsync</code>, to ensure the callbacks
 * executed when the completion stage is redeemed have the correct context:
 *
 * <pre>
 *     CompletionStage&lt;WSResponse&gt; response = ws.url(...).get();
 *     CompletionStage&lt;Result&gt; result = response.thenApplyAsync(response -&gt; {
 *         return ok("Got response body " + ws.body() + " while executing request " + request().uri());
 *     }, httpExecutionContext.current());
 * </pre>
 *
 * Note, this is not a Scala execution context, and is not intended to be used where Scala execution contexts are
 * required.
 */
@Singleton
public class HttpExecutionContext {

    private final Executor delegate;

    @Inject
    public HttpExecutionContext(Executor delegate) {
        this.delegate = delegate;
    }

    /**
     * Get the current executor associated with the current HTTP context.
     *
     * Note that the returned executor is only valid for the current context.  It should be used in a transient
     * fashion, long lived references to it should not be kept.
     *
     * @return An executor that will execute its tasks in the current HTTP context.
     */
    public Executor current() {
        return HttpExecution.fromThread(delegate);
    }
}
