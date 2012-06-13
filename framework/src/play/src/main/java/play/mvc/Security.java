package play.mvc;

import play.mvc.Http.*;

import java.lang.annotation.*;

/**
 * Defines several security helpers.
 */
public class Security {
    
    /**
     * Wraps the annotated action in an <code>AuthenticatedAction</code>.
     */
    @With(AuthenticatedAction.class)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Authenticated {
        Class<? extends Authenticator> value() default Authenticator.class;
    }
    
    /**
     * Wraps another action, allowing only authenticated HTTP requests.
     * <p>
     * The user name is retrieved from the session cookie, and added to the HTTP request's
     * <code>user</code> attribute.
     */
    public static class AuthenticatedAction extends Action<Authenticated> {
        
        public Result call(Context ctx) {
            try {
                Authenticator authenticator = configuration.value().newInstance();
                Object user = authenticator.getUser(ctx);
                if(user == null) {
                    return authenticator.onUnauthorized(ctx);
                } else {
                    try {
                        ctx.request().setUser(user);
                        return delegate.call(ctx);
                    } finally {
                        ctx.request().setUser(null);
                    }
                }
            } catch(RuntimeException e) {
                throw e;
            } catch(Throwable t) {
                throw new RuntimeException(t);
            }
        }

    }
    
    /**
     * Handles authentication.
     */
    public static class Authenticator extends Results {
        
        /**
         * Retrieves the user from the HTTP context; the default is to retrieve the username read from the session cookie.
         *
         * @return null if the user is not authenticated.
         */
        public String getUser(Context ctx) {
            return ctx.session().get("username");
        }
        
        /**
         * Generates an alternative result if the user is not authenticated; the default a simple '401 Not Authorized' page.
         */
        public Result onUnauthorized(Context ctx) {
            return unauthorized(views.html.defaultpages.unauthorized.render());
        }
        
    }
    
    
}
