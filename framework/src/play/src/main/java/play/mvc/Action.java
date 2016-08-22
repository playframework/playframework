/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.mvc;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import play.mvc.Http.Context;

/**
 * An action acts as decorator for the action method call.
 */
public abstract class Action<T> extends Results implements ActionWrapper {

    /**
     * The action configuration - typically the annotation used to decorate the action method.
     */
    public T configuration;

    /**
     * The wrapped action. This field must be set to some other value to use in action composition.
     */
    public Action<?> delegate;

    /**
     * Executes this action with the given HTTP context and returns the result.
     *
     * This will delegate to the delegate mutable field as the next action in the chain.
     *
     * @param ctx the http context in which to execute this action
     * @return a promise to the action's result
     */
    public CompletionStage<Result> call(Context ctx) {
        // Delegate may be null
        return call(ctx, delegateCall);
    }

    /**
     * A simple action with no configuration.
     */
    public static abstract class Simple extends Action<Void> {}

    private Function<Context, CompletionStage<Result>> delegateCall = ctx -> {
        if (delegate == null) {
            throw new IllegalStateException("Delegate is not set!");
        }
        return delegate.call(ctx);
    };

}
