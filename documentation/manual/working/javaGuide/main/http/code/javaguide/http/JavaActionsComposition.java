/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.ApplicationLoader;
import play.BuiltInComponentsFromContext;
import play.cache.AsyncCacheApi;
import play.cache.Cached;
import play.cache.caffeine.CaffeineCacheComponents;
import play.core.j.MappedJavaHandlerComponents;
import play.filters.components.NoHttpFiltersComponents;
import play.libs.Json;
import play.libs.typedmap.TypedKey;
import play.mvc.*;
import play.routing.Router;

import javax.inject.Inject;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.CompletionStage;

public class JavaActionsComposition extends Controller {

    private static final Logger log = LoggerFactory.getLogger(JavaActionsComposition.class);

    // #verbose-action
    public class VerboseAction extends play.mvc.Action.Simple {
        public CompletionStage<Result> call(Http.Request req) {
            log.info("Calling action for {}", req);
            return delegate.call(req);
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
        public CompletionStage<Result> call(Http.Request req) {
            if (configuration.value()) {
                log.info("Calling action for {}", req);
            }
            return delegate.call(req);
        }
    }
    // #verbose-annotation-action

    static class User {
        public static User findById(Integer id) { return new User(); }
    }

    // #pass-arg-action
    //###replace: public class Attrs {
    static class Attrs {
        public static final TypedKey<User> USER = TypedKey.<User>create("user");
    }

    public class PassArgAction extends play.mvc.Action.Simple {
        public CompletionStage<Result> call(Http.Request req) {
            return delegate.call(req.addAttr(Attrs.USER, User.findById(1234)));
        }
    }
    // #pass-arg-action

    // #pass-arg-action-index
    @With(PassArgAction.class)
    public static Result passArgIndex(Http.Request request) {
        User user = request.attrs().get(Attrs.USER);
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
        public CompletionStage<Result> call(Http.Request req) {
            return cacheApi.getOrElseUpdate(configuration.key(), () -> delegate.call(req));
        }
    }
    // #action-composition-dependency-injection

    // #action-composition-compile-time-di
    public class MyComponents extends BuiltInComponentsFromContext
            implements NoHttpFiltersComponents, CaffeineCacheComponents {

        public MyComponents(ApplicationLoader.Context context) {
            super(context);
        }

        @Override
        public Router router() {
            return Router.empty();
        }

        @Override
        public MappedJavaHandlerComponents javaHandlerComponents() {
            return super.javaHandlerComponents()
                    // Add action that does not depends on any other component
                    .addAction(VerboseAction.class, VerboseAction::new)
                    // Add action that depends on the cache api
                    .addAction(MyOwnCachedAction.class, () -> new MyOwnCachedAction(defaultCacheApi()));
        }
    }
    // #action-composition-compile-time-di
}
