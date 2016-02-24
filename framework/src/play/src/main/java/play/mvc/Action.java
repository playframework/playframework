/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.mvc;

import java.util.concurrent.CompletionStage;

import play.mvc.Http.Context;

/**
 * An action acts as decorator for the action method call.
 */
public abstract class Action<T> extends Results {

    /**
     * The action configuration - typically the annotation used to decorate the action method.
     */
    public T configuration;

    /**
     * The wrapped action.
     */
    public Action<?> delegate;

    /**
     * Executes this action with the given HTTP context and returns the result.
     *
     * @param ctx the http context in which to execute this action
     * @return a promise to the action's result
     */
    public abstract CompletionStage<Result> call(Context ctx);

    /**
     * A simple action with no configuration.
     */
    public static abstract class Simple extends Action<Void> {}

}
