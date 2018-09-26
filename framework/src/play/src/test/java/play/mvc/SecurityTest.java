/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import org.junit.Test;
import play.inject.Injector;

import java.lang.annotation.Annotation;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SecurityTest {
    @Test
    public void testAuthorized() throws Exception {

        Http.RequestBuilder builder = new Http.RequestBuilder();
        builder.session("username", "test_user");
        Http.Context ctx = new Http.Context(builder, null);
        Result r = callWithSecurity(ctx, c -> {
            Http.Request req = c.request();
            String username = req.attrs().get(Security.USERNAME);
            assertEquals("test_user", username);
            return Results.ok().withHeader("Actual-Username", username);
        });
        assertEquals(Http.Status.OK, r.status());
        assertEquals("test_user", r.headers().get("Actual-Username"));
    }

    @Test
    public void testUnauthorized() throws Exception {
        Http.RequestBuilder builder = new Http.RequestBuilder();
        Http.Context ctx = new Http.Context(builder, null);
        Result r = callWithSecurity(ctx, c -> { throw new AssertionError("Action should not be called"); });
        assertEquals(Http.Status.UNAUTHORIZED, r.status());
    }

    private Result callWithSecurity(Http.Context ctx, Function<Http.Context, Result> f) throws Exception {
        Injector injector = mock(Injector.class);
        when(injector.instanceOf(Security.Authenticator.class)).thenReturn(new Security.Authenticator());
        Security.AuthenticatedAction action = new Security.AuthenticatedAction(injector);
        action.configuration = new Security.Authenticated() {
            @Override
            public Class<? extends Security.Authenticator> value() {
                return Security.Authenticator.class;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }
        };
        action.delegate = new Action<Object>() {
            @Override
            public CompletionStage<Result> call(Http.Context ctx) {
                Result r = f.apply(ctx);
                return CompletableFuture.completedFuture(r);
            }
        };
        return action.call(ctx).toCompletableFuture().get(1, TimeUnit.SECONDS);
    }
}
