package play.cache;

import play.libs.F;
import play.mvc.*;
import play.mvc.Http.*;

/**
 * Cache another action.
 */
public class CachedAction extends Action<Cached> {
    
    public F.Promise<SimpleResult> call(Context ctx) {
        try {
            final String key = configuration.key();
            final Integer duration = configuration.duration();
            SimpleResult result = (SimpleResult) Cache.get(key);
            F.Promise<SimpleResult> promise;
            if(result == null) {
                promise = delegate.call(ctx);
                promise.onRedeem(new F.Callback<SimpleResult>() {
                    @Override
                    public void invoke(SimpleResult simpleResult) throws Throwable {
                        Cache.set(key, simpleResult, duration);
                    }
                });
            } else {
                promise = F.Promise.pure(result);
            }
            return promise;
        } catch(RuntimeException e) {
            throw e;
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }

}