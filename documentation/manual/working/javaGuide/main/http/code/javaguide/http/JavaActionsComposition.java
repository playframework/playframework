/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.http;

import play.Logger;
import play.cache.AsyncCacheApi;
import play.cache.Cached;
import play.libs.Json;
import play.mvc.*;

import javax.inject.Inject;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import java.util.concurrent.CompletionStage;

public class JavaActionsComposition extends Controller {

    // #verbose-action
    public class VerboseAction extends play.mvc.Action.Simple {
        public CompletionStage<Result> call(Http.Context ctx) {
            Logger.info("Calling action for {}", ctx);
            return delegate.call(ctx);
        }
    }
    // #verbose-action

    // #verbose-index
    @With(VerboseAction.class)
    public Result verboseIndex() {
        return ok("It works!");
    }
    // #verbose-index

    // #authenticated-cached-index
    @Security.Authenticated
    @Cached(key = "index.result")
    public Result authenticatedCachedIndex() {
        return ok("It works!");
    }
    // #authenticated-cached-index

    // #verbose-annotation
    @With(VerboseAnnotationAction.class)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface VerboseAnnotation {
        boolean value() default true;
    }
    // #verbose-annotation

    // #verbose-annotation-index
    @VerboseAnnotation(false)
    public Result verboseAnnotationIndex() {
        return ok("It works!");
    }
    // #verbose-annotation-index

    // #verbose-annotation-action
    public class VerboseAnnotationAction extends Action<VerboseAnnotation> {
        public CompletionStage<Result> call(Http.Context ctx) {
            if (configuration.value()) {
                Logger.info("Calling action for {}", ctx);
            }
            return delegate.call(ctx);
        }
    }
    // #verbose-annotation-action

    static class User {
        public static Integer findById(Integer id) { return id; }
    }

    // #pass-arg-action
    public class PassArgAction extends play.mvc.Action.Simple {
        public CompletionStage<Result> call(Http.Context ctx) {
            ctx.args.put("user", User.findById(1234));
            return delegate.call(ctx);
        }
    }
    // #pass-arg-action

    // #pass-arg-action-index
    @With(PassArgAction.class)
    public static Result passArgIndex() {
        Object user = ctx().args.get("user");
        return ok(Json.toJson(user));
    }
    // #pass-arg-action-index

    // #annotated-controller
    @Security.Authenticated
    public class Admin extends Controller {
        /// ###insert: ...

    }
    // #annotated-controller

    // #action-composition-dependency-injection-annotation
    @With(MyOwnCachedAction.class)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface WithCache {
        String key();
    }
    // #action-composition-dependency-injection-annotation

    // #action-composition-dependency-injection
    public class MyOwnCachedAction extends Action<WithCache> {

        private final AsyncCacheApi cacheApi;

        @Inject
        public MyOwnCachedAction(AsyncCacheApi cacheApi) {
            this.cacheApi = cacheApi;
        }

        @Override
        public CompletionStage<Result> call(Http.Context ctx) {
            return cacheApi.getOrElseUpdate(configuration.key(), () -> delegate.call(ctx));
        }
    }
    // #action-composition-dependency-injection

}
