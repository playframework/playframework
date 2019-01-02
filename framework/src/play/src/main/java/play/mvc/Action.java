/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import java.lang.reflect.AnnotatedElement;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import play.core.j.JavaContextComponents;
import play.libs.typedmap.TypedKey;
import play.mvc.Http.Context;
import play.mvc.Http.Request;

import javax.inject.Inject;

/**
 * An action acts as decorator for the action method call.
 */
public abstract class Action<T> extends Results {

    private JavaContextComponents contextComponents;

    @Inject
    public void setContextComponents(JavaContextComponents contextComponents) {
        this.contextComponents = contextComponents;
    }

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

    private static final TypedKey<Map<String, Object>> CTX_ARGS = TypedKey.<Map<String, Object>>create("http-context-args");

    /**
     * Executes this action with the given HTTP context and returns the result.
     *
     * @param ctx the http context in which to execute this action
     * @return a promise to the action's result
     *
     * @deprecated Since 2.7.0. Use {@link #call(Request)} instead. Please see <a href="https://www.playframework.com/documentation/latest/JavaHttpContextMigration27">the migration guide</a> for more details.
     */
    @Deprecated // TODO: When you remove this method make call(Request) below abstract
    public CompletionStage<Result> call(Context ctx) {
        return call(ctx.args != null && !ctx.args.isEmpty() ? ctx.request().addAttr(CTX_ARGS, ctx.args) : ctx.request());
    }

    /**
     * Executes this action with the given HTTP request and returns the result.
     *
     * @param req the http request with which to execute this action
     * @return a promise to the action's result
     */
    public CompletionStage<Result> call(Request req) { // TODO: Make this method abstract after removing call(Context)
        return Context.safeCurrent().map(threadLocalCtx -> {
            // A previous action did explicitly set a context onto the thread local (via Http.Context.current.set(...))
            // Let's use that context so the user doesn't loose data he/she set onto that ctx (args,...)
            Context newCtx = threadLocalCtx.withRequest(req.removeAttr(CTX_ARGS));
            Context.setCurrent(newCtx);
            return call(newCtx);
        }).orElseGet(() -> {
            // A previous action did not set a context explicitly, we simply create a new one to pass on the request
            Context ctx = new Context(req.removeAttr(CTX_ARGS), contextComponents);
            ctx.args = req.attrs().getOptional(CTX_ARGS).orElse(new HashMap<>());
            return call(ctx);
        });
    }

    /**
     * A simple action with no configuration.
     */
    public static abstract class Simple extends Action<Void> {}

}
