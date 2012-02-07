package play.cache;

import play.mvc.*;
import play.mvc.Http.*;

/**
 * Cache another action.
 */
public class CachedAction extends Action<Cached> {
    
    public Result call(Context ctx) {
        try {
            String key = configuration.key();
            Integer duration = configuration.duration();
            Result result = (Result)Cache.get(key);
            if(result == null) {
                result = delegate.call(ctx);
                Cache.set(key, result, duration);
            }
            return result;
        } catch(RuntimeException e) {
            throw e;
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }

}