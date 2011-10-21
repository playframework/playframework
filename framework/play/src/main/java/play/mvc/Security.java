package play.mvc;

import play.mvc.Http.*;

import java.lang.annotation.*;

/**
 * Defines several security helpers.
 */
public class Security {
    
    /**
     * Wrap the annotated action into an Authenticated Action
     */
    @With(AuthenticatedAction.class)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Authenticated {
        Class<? extends Authenticator> value() default Authenticator.class;
    }
    
    /**
     * Wrap another action, allowing only authenticated HTTP requests.
     *
     * The username is retrieved from the session cookie, and added to the HTTP request
     * username attribute.
     */
    public static class AuthenticatedAction extends Action<Authenticated> {
        
        public Result call(Context ctx) {
            try {
                Authenticator authenticator = configuration.value().newInstance();
                String username = authenticator.getUsername(ctx);
                if(username == null) {
                    return authenticator.onUnauthorized(ctx);
                } else {
                    try {
                        ctx.request().setUsername(username);
                        return deleguate.call(ctx);
                    } finally {
                        ctx.request().setUsername(null);
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
     * Handle authentication.
     */
    public static class Authenticator extends Results {
        
        /**
         * Retrieve the username from the HTTP context (default is to read from session cookie)..
         *
         * @return null if the user is not authenticated.
         */
        public String getUsername(Context ctx) {
            return ctx.session().get("username");
        }
        
        /**
         * Generate alternative result if the user is not authenticated (default to simple 401 page)
         */
        public Result onUnauthorized(Context ctx) {
            return unauthorized(views.html.defaultpages.unauthorized.render());
        }
        
    }
    
    
}