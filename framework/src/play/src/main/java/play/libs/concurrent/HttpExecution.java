/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.concurrent;

import play.core.Execution;
import play.core.j.HttpExecutionContext;
import scala.concurrent.ExecutionContext;
import scala.concurrent.ExecutionContextExecutor;

import java.util.concurrent.Executor;

/**
 * ExecutionContexts that preserve the current thread's context ClassLoader and
 * Http.Context.
 */
public class HttpExecution {

    /**
     * An ExecutionContext that executes work on the given ExecutionContext. The
     * current thread's context ClassLoader and Http.Context are captured when
     * this method is called and preserved for all executed tasks.
     */
    public static ExecutionContextExecutor fromThread(ExecutionContext delegate) {
        return HttpExecutionContext.fromThread(delegate);
    }

    /**
     * An ExecutionContext that executes work on the given ExecutionContext. The
     * current thread's context ClassLoader and Http.Context are captured when
     * this method is called and preserved for all executed tasks.
     */
    public static ExecutionContextExecutor fromThread(Executor delegate) {
        return HttpExecutionContext.fromThread(delegate);
    }

    /**
     * An ExecutionContext that executes work on the application's internal
     * ActorSystem dispatcher. The current thread's context ClassLoader and
     * Http.Context are captured when this method is called and preserved
     * for all executed tasks.
     */
    public static ExecutionContextExecutor defaultContext() {
        return HttpExecutionContext.fromThread(Execution.internalContext());
    }

}
