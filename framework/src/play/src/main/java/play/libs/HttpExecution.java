package play.libs;

import play.core.Invoker;
import play.core.j.HttpExecutionContext;
import play.core.j.OrderedExecutionContext;
import scala.concurrent.ExecutionContext;
import scala.concurrent.ExecutionContextExecutor;

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
     * An ExecutionContext that executes work on the application's internal
     * ActorSystem dispatcher. The current thread's context ClassLoader and
     * Http.Context are captured when this method is called and preserved
     * for all executed tasks.
     */
    public static ExecutionContextExecutor defaultContext() {
        return HttpExecutionContext.fromThread(Invoker.executionContext());
    }

    private static ExecutionContext orderedExecutionContext = new OrderedExecutionContext(Invoker.system(), 64);

    /**
     * An ExecutionContext that executes work for a given Http.Context in the
     * same actor each time, ensuring ordered execution of that work. The
     * current thread's context ClassLoader and Http.Context are captured when
     * this method is called and preserved for all executed tasks.
     * 
     * This ExecutionContext gives the legacy behaviour of Play's F.Promise
     * class.
     */
    public static ExecutionContextExecutor orderedContext() {
        return HttpExecutionContext.fromThread(orderedExecutionContext);
    }

}
