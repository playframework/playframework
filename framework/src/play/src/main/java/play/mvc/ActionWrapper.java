package play.mvc;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import play.mvc.Http.Context;

/**
 * This interface can be used to represent a builder for an action. This allows us to wrap other action builders.
 */
@FunctionalInterface
public interface ActionWrapper {

    /**
     * Executes this action with the given HTTP context and returns the result.
     *
     * @param ctx the http context in which to execute this action
     * @param delegate the handler to delegate to afterward
     * @return a promise to the action's result
     */
    CompletionStage<Result> call(Context ctx, Function<Context, CompletionStage<Result>> delegate);

    /**
     * Use this action to wrap another action and delegate to the handler provided.
     *
     * Note: this modifies the mutable delegate field in this action.
     *
     * @param handler the next handler to be called by this action
     * @return a new ActionHandler
     */
    default Function<Context, CompletionStage<Result>> sync(Function<Context, Result> handler) {
        return async(ctx -> CompletableFuture.completedFuture(handler.apply(ctx)));
    }

    /**
     * Use this action to wrap another action and delegate to the async handler provided.
     *
     * Note: this modifies the mutable delegate field in this action.
     *
     * @param handler the next handler to be called by this action
     * @return a new ActionHandler
     */
    default Function<Context, CompletionStage<Result>> async(Function<Context, CompletionStage<Result>> handler) {
        return ctx -> call(ctx, handler);
    }
}
