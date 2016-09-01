/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.mvc;

import com.google.common.collect.ImmutableMap;
import java.lang.annotation.Annotation;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import play.inject.Injector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class SecurityTest {
    public static class AuthenticatedActionTest {
        Http.Context ctx;
        Http.Request req;
        Http.Context usernameCtx;
        Http.Request usernameReq;

        @Before
        public void setUp() {
            ctx = mock(Http.Context.class);
            req = mock(Http.Request.class);
            usernameCtx = mock(Http.Context.class);
            usernameReq = mock(Http.Request.class);
        }

        @Test
        public void testAuthorized() throws Exception {
            when(ctx.session()).thenReturn(new Http.Session(ImmutableMap.of("username", "test_user")));
            when(ctx.request()).thenReturn(req);
            when(req.withAttr(Security.USERNAME, "test_user")).thenReturn(usernameReq);
            when(ctx.withRequest(usernameReq)).thenReturn(usernameCtx);

            Result r = callWithSecurity(ctx -> {
                assertSame(usernameCtx, ctx);
                when(usernameCtx.request()).thenReturn(usernameReq);
                when(usernameReq.attr(Security.USERNAME)).thenReturn("test_user");
                Http.Request req = ctx.request();
                String username = req.attr(Security.USERNAME);
                assertEquals("test_user", username);
                return Results.ok().withHeader("Actual-Username", username);
            });
            assertEquals(Http.Status.OK, r.status());
            assertEquals("test_user", r.headers().get("Actual-Username"));
        }

        @Test
        public void testUnauthorized() throws Exception {
            when(ctx.session()).thenReturn(new Http.Session(ImmutableMap.of()));
            Result r = callWithSecurity(ctx -> { throw new AssertionError("Action should not be called"); });
            assertEquals(Http.Status.UNAUTHORIZED, r.status());
        }

        private Result callWithSecurity(Function<Http.Context, Result> f) throws Exception {
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
}
