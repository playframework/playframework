/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http;

import play.mvc.*;
import play.test.Helpers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.CompletionStage;

public class ActionCompositionOrderTest {

    @With(ControllerComposition.class)
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface ControllerAnnotation {}

    static class ControllerComposition extends Action<ControllerAnnotation> {
        @Override
        public CompletionStage<Result> call(Http.Request req) {
            return delegate.call(req).thenApply(result -> {
                String newContent = this.annotatedElement.getClass().getName() + "controller" + Helpers.contentAsString(result);
                return Results.ok(newContent);
            });
        }
    }

    @With(ActionComposition.class)
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface ActionAnnotation {}

    static class ActionComposition extends Action<ActionAnnotation> {
        @Override
        public CompletionStage<Result> call(Http.Request req) {
            return delegate.call(req).thenApply(result -> {
                String newContent = this.annotatedElement.getClass().getName() + "action" + Helpers.contentAsString(result);
                return Results.ok(newContent);
            });
        }
    }

    @With(WithUsernameAction.class)
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface WithUsername {
        String value();
    }

    static class WithUsernameAction extends Action<WithUsername> {
        @Override
        public CompletionStage<Result> call(Http.Request req) {
            return delegate.call(req.addAttr(Security.USERNAME, configuration.value()));
        }
    }

    @With({FirstAction.class, SecondAction.class}) // let's run two actions
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Repeatable(SomeRepeatable.List.class)
    public static @interface SomeRepeatable {
        /**
         * Defines several {@code @SomeRepeatable} annotations on the same element.
         */
        @Target({ElementType.TYPE, ElementType.METHOD})
        @Retention(RetentionPolicy.RUNTIME)
        public @interface List {
            SomeRepeatable[] value();
        }
    }

    public static class FirstAction extends Action<SomeRepeatable> {
        @Override
        public CompletionStage<Result> call(Http.Request req) {
            return delegate.call(req).thenApply(result -> {
                String newContent = this.annotatedElement.getClass().getName() + "action1" + Helpers.contentAsString(result);
                return Results.ok(newContent);
            });
        }
    }

    public static class SecondAction extends Action<SomeRepeatable> {
        @Override
        public CompletionStage<Result> call(Http.Request req) {
            return delegate.call(req).thenApply(result -> {
                String newContent = this.annotatedElement.getClass().getName() + "action2" + Helpers.contentAsString(result);
                return Results.ok(newContent);
            });
        }
    }

    /**
     * Could be seen as a container annotation (like SomeRepeatable.List above), however it defines @With so it's simply seen as action annotation
     */
    @With(SomeActionAnnotationAction.class)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface SomeActionAnnotation {
        SomeRepeatable[] value();
    }

    public static class SomeActionAnnotationAction extends Action<SomeActionAnnotation> {
        @Override
        public CompletionStage<Result> call(Http.Request req) {
            return delegate.call(req).thenApply(result -> {
                String newContent = "do_NOT_treat_me_as_container_annotation" + Helpers.contentAsString(result);
                return Results.ok(newContent);
            });
        }
    }

    @With(ContextArgsSetAction.class)
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface ContextArgsSet {}

    static class ContextArgsSetAction extends Action<ContextArgsSet> {
        @Override
        public CompletionStage<Result> call(Http.Context ctx) {
            ctx.args.put("foo", "bar");
            return delegate.call(ctx);
        }
    }

    @With(ContextArgsGetAction.class)
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface ContextArgsGet {}

    static class ContextArgsGetAction extends Action<ContextArgsGet> {
        @Override
        public CompletionStage<Result> call(Http.Context ctx) {
            return delegate.call(ctx).thenApply(result -> {
                if("bar".equals(ctx.args.get("foo"))) {
                    return Results.ok("ctx.args were set");
                }
                return Results.ok();
            });
        }
    }

    @With(NoopUsingRequestAction.class)
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface NoopUsingRequest {}

    static class NoopUsingRequestAction extends Action<NoopUsingRequest> {
        @Override
        public CompletionStage<Result> call(Http.Request req) {
            return delegate.call(req);
        }
    }
}
