package play.mvc;

import com.google.common.collect.ImmutableMap;
import java.lang.annotation.Annotation;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import play.inject.Injector;
import play.libs.F;

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
        public void testDontSetUsernameToNullUntilDelegateFinishes() {
            runSetUsernameToNullInCallback(false);
        }

        @Test
        public void testDontSetUsernameToNullUntilDelegateRaisesException() {
            runSetUsernameToNullInCallback(true);
        }

        @Test
        public void testSetUsernameToNullWhenExceptionRaised() {
            action.delegate = new Action<Object>() {
                @Override
                public F.Promise<Result> call(Http.Context ctx) {
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

        private void runSetUsernameToNullInCallback(final boolean shouldRaiseException) {
            action.delegate = new Action<Object>() {
                @Override
                public F.Promise<Result> call(Http.Context ctx) {
                    return F.Promise.promise(() -> {
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
                    action.call(ctx).get(1000);
                } catch (Exception e) {
                    Assert.assertEquals(exception, e);
                }
            } else {
                Assert.assertEquals(ok, action.call(ctx).get(1000));
            }

            verify(req).setUsername("test_user");
            verify(req).setUsername(null);
        }
    }
}
