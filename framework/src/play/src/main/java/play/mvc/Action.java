/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import java.lang.reflect.AnnotatedElement;
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
     * Where an action was defined.
     */
    public AnnotatedElement annotatedElement;

    /**
     * The precursor action.
     *
     * If this action was called in a chain then this will contain the value of the action
     * that is called before this action. If no action was called first, then this value will be null.
     */
    public Action<?> precursor;

    /**
     * The wrapped action.
     *
     * If this action was called in a chain then this will contain the value of the action
     * that is called after this action. If there is no action left to be called, then this value will be null.
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
