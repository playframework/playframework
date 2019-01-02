/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
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
        Result r = callWithSecurity(builder.build(), req -> {
            String username = req.attrs().get(Security.USERNAME);
            assertEquals("test_user", username);
            return Results.ok().withHeader("Actual-Username", username);
        });
        assertEquals(Http.Status.OK, r.status());
        assertEquals("test_user", r.headers().get("Actual-Username"));
    }

    @Test
    public void testUnauthorized() throws Exception {
        Result r = callWithSecurity(new Http.RequestBuilder().build(), c -> { throw new AssertionError("Action should not be called"); });
        assertEquals(Http.Status.UNAUTHORIZED, r.status());
    }

    private Result callWithSecurity(Http.Request req, Function<Http.Request, Result> f) throws Exception {
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
            public CompletionStage<Result> call(Http.Request req) {
                Result r = f.apply(req);
                return CompletableFuture.completedFuture(r);
            }
        };
        return action.call(req).toCompletableFuture().get(1, TimeUnit.SECONDS);
    }
}
