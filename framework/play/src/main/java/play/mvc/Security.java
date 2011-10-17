package play.mvc;

import play.mvc.Http.*;
import play.mvc.Result.*;

import java.lang.annotation.*;

public class Security {
    
    @With(AuthenticatedAction.class)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Authenticated {
        Class<? extends Authenticator> value() default Authenticator.class;
    }
    
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
    
    public static class Authenticator {
        
        public String getUsername(Context ctx) {
            return ctx.session().get("username");
        }
        
        public Result onUnauthorized(Context ctx) {
            return new Unauthorized(views.html.defaultpages.unauthorized.render());
        }
        
    }
    
    
}