/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.http;

import play.mvc.*;
import play.test.Helpers;

import java.lang.annotation.ElementType;
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
        public CompletionStage<Result> call(Http.Context ctx) {
            return delegate.call(ctx).thenApply(result -> {
                String newContent = "controller" + Helpers.contentAsString(result);
                return Results.ok(newContent);
            });
        }
    }

    @With(ActionComposition.class)
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface ActionAnnotation {}

    static class ActionComposition extends Action<ControllerAnnotation> {
        @Override
        public CompletionStage<Result> call(Http.Context ctx) {
            return delegate.call(ctx).thenApply(result -> {
                String newContent = "action" + Helpers.contentAsString(result);
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
        public CompletionStage<Result> call(Http.Context ctx) {
            return delegate.call(ctx.withRequest(ctx.request().withUsername(configuration.value())));
        }
    }
}
