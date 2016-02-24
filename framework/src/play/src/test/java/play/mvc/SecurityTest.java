/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.mvc;

import com.google.common.collect.ImmutableMap;
import java.lang.annotation.Annotation;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import play.inject.Injector;

import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class SecurityTest {
    public static class AuthenticatedActionTest {
        Http.Context ctx;
        Http.Request req;
        Injector injector;
        Security.AuthenticatedAction action;

        RuntimeException exception = new RuntimeException("test exception");
        final Result ok = Results.ok();

        @Before
        public void setUp() {
            ctx = mock(Http.Context.class);
            req = mock(Http.Request.class);
            injector = mock(Injector.class);

            when(ctx.session()).thenReturn(new Http.Session(ImmutableMap.of("username", "test_user")));
            when(ctx.request()).thenReturn(req);
            doNothing().when(req).setUsername(anyString());
            doNothing().when(req).setUsername(null);
            when(injector.instanceOf(Security.Authenticator.class)).thenReturn(new Security.Authenticator());

            action = new Security.AuthenticatedAction(injector);
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
        }

        @Test
        public void testDontSetUsernameToNullUntilDelegateFinishes() throws Exception {
            runSetUsernameToNullInCallback(false);
        }

        @Test
        public void testDontSetUsernameToNullUntilDelegateRaisesException() throws Exception {
            runSetUsernameToNullInCallback(true);
        }

        @Test
        public void testSetUsernameToNullWhenExceptionRaised() {
            action.delegate = new Action<Object>() {
                @Override
                public CompletionStage<Result> call(Http.Context ctx) {
                    throw exception;
                }
            };

            try {
                action.call(ctx);
            } catch (RuntimeException e) {
                Assert.assertEquals(exception, e);
            }

            verify(req).setUsername("test_user");
            verify(req).setUsername(null);
        }

        private void runSetUsernameToNullInCallback(final boolean shouldRaiseException) throws Exception {
            action.delegate = new Action<Object>() {
                @Override
                public CompletionStage<Result> call(Http.Context ctx) {
                    return CompletableFuture.supplyAsync(() -> {
                        if (shouldRaiseException) {
                            throw exception;
                        } else {
                            return ok;
                        }
                    });
                }
            };

            if (shouldRaiseException) {
                try {
                    action.call(ctx).toCompletableFuture().get(1, TimeUnit.SECONDS);
                } catch (ExecutionException e) {
                    Assert.assertEquals(exception, e.getCause());
                }
            } else {
                Assert.assertEquals(ok, action.call(ctx).toCompletableFuture().get(1, TimeUnit.SECONDS));
            }

            verify(req).setUsername("test_user");
            verify(req).setUsername(null);
        }
    }
}
