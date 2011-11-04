package controllers;

import play.mvc.*;
import play.mvc.Http.*;
import play.mvc.Result.*;

import java.lang.annotation.*;

public class Security {
    
    @With(SecureAction.class)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Secured {
        
        Class<? extends Predicate> value() default Connected.class;
        String data() default "";
        
    }
    
    public static class SecureAction extends Action<Secured> {
        
        public Result call(Context ctx) {
            try {
                Predicate predicate = configuration.value().newInstance();
                if(predicate.check(ctx, configuration.data())) {
                    return deleguate.call(ctx);
                } else {
                    return new Forbidden("INTERDIT");
                }
            } catch(RuntimeException e) {
                throw e;
            } catch(Throwable t) {
                throw new RuntimeException(t);
            }
        }

    }
    
    public static interface Predicate {
        public boolean check(Context ctx, String data);
    }
    
    public static class Connected implements Predicate {
        public boolean check(Context ctx, String data) {
            return false;
        }
    }
    
}